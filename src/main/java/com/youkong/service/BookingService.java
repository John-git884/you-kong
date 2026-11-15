package com.youkong.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.BizException;
import com.youkong.common.BookingStatus;
import com.youkong.common.ErrorCode;
import com.youkong.dto.BookingVO;
import com.youkong.dto.CreateBookingRequest;
import com.youkong.entity.Booking;
import com.youkong.entity.BookingSlot;
import com.youkong.entity.ServiceItem;
import com.youkong.entity.Shop;
import com.youkong.entity.Staff;
import com.youkong.mapper.BookingMapper;
import com.youkong.mapper.BookingSlotMapper;
import com.youkong.mapper.ServiceItemMapper;
import com.youkong.mapper.ShopMapper;
import com.youkong.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 预约核心服务。
 *
 * 防超卖的三层设计：
 *   1) 唯一索引 (staff_id, slot_start) —— 数据库层兜底，是唯一真相来源
 *   2) 单事务内整段插入 —— 60 分钟服务要么占满 4 格，要么一格不占
 *   3) 状态机 CAS 更新 —— 改期/核销/取消不会互相覆盖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int PENDING_TTL_MINUTES = 5;

    private final BookingMapper bookingMapper;
    private final BookingSlotMapper bookingSlotMapper;
    private final StaffMapper staffMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ShopMapper shopMapper;
    private final ScheduleResolver scheduleResolver;

    @Transactional(rollbackFor = Exception.class)
    public BookingVO create(CreateBookingRequest req) {
        Staff staff = staffMapper.selectById(req.getStaffId());
        if (staff == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "staff " + req.getStaffId());
        }
        ServiceItem item = serviceItemMapper.selectById(req.getServiceId());
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "service " + req.getServiceId());
        }
        Shop shop = shopMapper.selectById(staff.getShopId());
        if (shop == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "shop " + staff.getShopId());
        }

        int slotMinutes = shop.getSlotMinutes() == null ? 15 : shop.getSlotMinutes();
        LocalDateTime startAt = req.getStartAt();
        if (startAt.getSecond() != 0 || startAt.getNano() != 0
                || startAt.getMinute() % slotMinutes != 0) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "开始时间必须是 " + slotMinutes + " 分钟的整数倍");
        }
        if (!startAt.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.SLOT_IN_PAST);
        }
        LocalDateTime endAt = startAt.plusMinutes(item.getDurationMinutes());
        checkInSchedule(req.getStaffId(), shop, startAt, endAt);

        Booking booking = new Booking();
        booking.setShopId(shop.getId());
        booking.setStaffId(req.getStaffId());
        booking.setServiceId(req.getServiceId());
        booking.setCustomerName(req.getCustomerName());
        booking.setCustomerPhone(req.getCustomerPhone());
        booking.setStartAt(startAt);
        booking.setEndAt(endAt);
        booking.setStatus(BookingStatus.PENDING);
        booking.setVersion(0);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.insert(booking);

        List<LocalDateTime> slots = SlotCalculator.requiredSlots(
                startAt, item.getDurationMinutes(), slotMinutes);
        try {
            for (LocalDateTime slotStart : slots) {
                bookingSlotMapper.insert(BookingSlot.of(req.getStaffId(), slotStart, booking.getId()));
            }
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突 = 该格已被别人占了。
            // 直接抛业务异常让整个事务回滚，主单不会留下残骸。
            // 注意：不在这里"降级重试"，因为竞态下重试只会连续失败，交给前端换时间更合理。
            log.info("slot conflict staffId={} startAt={}", req.getStaffId(), startAt);
            throw new BizException(ErrorCode.SLOT_TAKEN);
        }

        return BookingVO.from(booking);
    }

    @Transactional(rollbackFor = Exception.class)
    public BookingVO confirm(Long bookingId) {
        if (!transit(bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED)) {
            throw new BizException(ErrorCode.BOOKING_STATUS_ILLEGAL, "confirm " + bookingId);
        }
        return BookingVO.from(requireBooking(bookingId));
    }

    @Transactional(rollbackFor = Exception.class)
    public BookingVO complete(Long bookingId) {
        if (!transit(bookingId, BookingStatus.CONFIRMED, BookingStatus.COMPLETED)) {
            throw new BizException(ErrorCode.BOOKING_STATUS_ILLEGAL, "complete " + bookingId);
        }
        return BookingVO.from(requireBooking(bookingId));
    }

    /**
     * 取消预约并释放档期。CAS 更新保证并发下只有一次生效。
     */
    @Transactional(rollbackFor = Exception.class)
    public BookingVO cancel(Long bookingId) {
        boolean moved = transit(bookingId, BookingStatus.PENDING, BookingStatus.CANCELLED)
                || transit(bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
        if (!moved) {
            throw new BizException(ErrorCode.BOOKING_STATUS_ILLEGAL, "cancel " + bookingId);
        }
        releaseSlots(bookingId);
        return BookingVO.from(requireBooking(bookingId));
    }

    /**
     * 改期：释放原档期并占用新档期，两者在同一个事务里完成。
     *
     * 先删旧槽位再插新槽位，是因为新时段可能与旧时段重叠（例如 10:00 改到 10:15），
     * 反过来先插会撞上自己的唯一索引。删和插在同一事务内，新档期被别人占了就整体回滚，
     * 原档期不会丢。
     */
    @Transactional(rollbackFor = Exception.class)
    public BookingVO reschedule(Long bookingId, LocalDateTime newStartAt) {
        Booking booking = requireBooking(bookingId);
        if (!BookingStatus.PENDING.equals(booking.getStatus())
                && !BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new BizException(ErrorCode.BOOKING_STATUS_ILLEGAL, "reschedule " + bookingId);
        }

        ServiceItem item = serviceItemMapper.selectById(booking.getServiceId());
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "service " + booking.getServiceId());
        }
        Shop shop = shopMapper.selectById(booking.getShopId());
        if (shop == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "shop " + booking.getShopId());
        }

        int slotMinutes = shop.getSlotMinutes() == null ? 15 : shop.getSlotMinutes();
        if (newStartAt.getSecond() != 0 || newStartAt.getNano() != 0
                || newStartAt.getMinute() % slotMinutes != 0) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "开始时间必须是 " + slotMinutes + " 分钟的整数倍");
        }
        if (!newStartAt.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.SLOT_IN_PAST);
        }
        if (newStartAt.equals(booking.getStartAt())) {
            return BookingVO.from(booking);
        }

        LocalDateTime newEndAt = newStartAt.plusMinutes(item.getDurationMinutes());
        checkInSchedule(booking.getStaffId(), shop, newStartAt, newEndAt);

        releaseSlots(bookingId);
        try {
            for (LocalDateTime slotStart : SlotCalculator.requiredSlots(
                    newStartAt, item.getDurationMinutes(), slotMinutes)) {
                bookingSlotMapper.insert(BookingSlot.of(booking.getStaffId(), slotStart, bookingId));
            }
        } catch (DuplicateKeyException e) {
            log.info("reschedule conflict bookingId={} newStart={}", bookingId, newStartAt);
            throw new BizException(ErrorCode.SLOT_TAKEN);
        }

        int rows = bookingMapper.update(null, Wrappers.<Booking>lambdaUpdate()
                .eq(Booking::getId, bookingId)
                .eq(Booking::getVersion, booking.getVersion())
                .set(Booking::getStartAt, newStartAt)
                .set(Booking::getEndAt, newEndAt)
                .set(Booking::getUpdatedAt, LocalDateTime.now())
                .setSql("version = version + 1"));
        if (rows == 0) {
            throw new BizException(ErrorCode.BOOKING_STATUS_ILLEGAL, "预约已被修改，请刷新后重试");
        }
        return BookingVO.from(requireBooking(bookingId));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelQuietly(Long bookingId) {
        boolean moved = transit(bookingId, BookingStatus.PENDING, BookingStatus.CANCELLED);
        if (moved) {
            releaseSlots(bookingId);
        }
        return moved;
    }

    public void releaseSlots(Long bookingId) {
        bookingSlotMapper.delete(Wrappers.<BookingSlot>lambdaQuery()
                .eq(BookingSlot::getBookingId, bookingId));
    }

    public List<BookingVO> listByStaffAndDate(Long staffId, LocalDate date) {
        LocalDateTime from = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime to = LocalDateTime.of(date.plusDays(1), LocalTime.MIN);
        return bookingMapper.selectList(Wrappers.<Booking>lambdaQuery()
                        .eq(Booking::getStaffId, staffId)
                        .ge(Booking::getStartAt, from)
                        .lt(Booking::getStartAt, to)
                        .orderByAsc(Booking::getStartAt))
                .stream().map(BookingVO::from).toList();
    }

    /** 找出超时未确认的预约，供 ExpireJob 调用 */
    public List<Booking> findExpiredPending() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(PENDING_TTL_MINUTES);
        return bookingMapper.selectList(Wrappers.<Booking>lambdaQuery()
                .eq(Booking::getStatus, BookingStatus.PENDING)
                .lt(Booking::getCreatedAt, deadline)
                .last("limit 200"));
    }

    private boolean transit(Long bookingId, String from, String to) {
        int rows = bookingMapper.update(null, Wrappers.<Booking>lambdaUpdate()
                .eq(Booking::getId, bookingId)
                .eq(Booking::getStatus, from)
                .set(Booking::getStatus, to)
                .set(Booking::getUpdatedAt, LocalDateTime.now())
                .setSql("version = version + 1"));
        return rows > 0;
    }

    private Booking requireBooking(Long bookingId) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null) {
            throw new BizException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return b;
    }

    private void checkInSchedule(Long staffId, Shop shop,
                                 LocalDateTime startAt, LocalDateTime endAt) {
        LocalDate date = startAt.toLocalDate();
        ScheduleResolver.WorkWindow window = scheduleResolver.window(staffId, shop, date)
                .orElseThrow(() -> new BizException(ErrorCode.SLOT_OUT_OF_SCHEDULE,
                        "当天未排班或已请假"));

        LocalDateTime dayOpen = LocalDateTime.of(date, window.start());
        LocalDateTime dayClose = LocalDateTime.of(date, window.end());
        if (startAt.isBefore(dayOpen) || endAt.isAfter(dayClose)) {
            throw new BizException(ErrorCode.SLOT_OUT_OF_SCHEDULE,
                    "可约时间 " + window.start() + "-" + window.end());
        }
    }
}
