package com.youkong.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.BizException;
import com.youkong.common.ErrorCode;
import com.youkong.dto.AvailabilityVO;
import com.youkong.entity.BookingSlot;
import com.youkong.entity.ServiceItem;
import com.youkong.entity.Shop;
import com.youkong.entity.Staff;
import com.youkong.mapper.BookingSlotMapper;
import com.youkong.mapper.ServiceItemMapper;
import com.youkong.mapper.ShopMapper;
import com.youkong.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 档期查询。
 *
 * 已占用槽位一次性查出来在内存里做集合运算。单店单日只有几百行，
 * 比在 SQL 里写 NOT EXISTS 更简单也更快；若单员工日槽位上到几千，再考虑下推到数据库。
 */
@Service
@RequiredArgsConstructor
public class SlotService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ShopMapper shopMapper;
    private final StaffMapper staffMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final BookingSlotMapper bookingSlotMapper;
    private final ScheduleResolver scheduleResolver;

    public AvailabilityVO availability(Long staffId, Long serviceId, LocalDate date) {
        return availability(staffId, serviceId, date, LocalDateTime.now());
    }

    /**
     * 显式传入 now，方便单测固定时间。
     */
    public AvailabilityVO availability(Long staffId, Long serviceId, LocalDate date, LocalDateTime now) {
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "staff " + staffId);
        }
        ServiceItem item = serviceItemMapper.selectById(serviceId);
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "service " + serviceId);
        }
        Shop shop = shopMapper.selectById(staff.getShopId());
        if (shop == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "shop " + staff.getShopId());
        }

        AvailabilityVO vo = new AvailabilityVO();
        vo.setDate(date);
        vo.setStaffId(staffId);
        vo.setServiceId(serviceId);
        vo.setDurationMinutes(item.getDurationMinutes());

        Optional<ScheduleResolver.WorkWindow> window = scheduleResolver.window(staffId, shop, date);
        if (window.isEmpty()) {
            vo.setSlots(Collections.emptyList());
            return vo;
        }

        int slotMinutes = shop.getSlotMinutes() == null ? 15 : shop.getSlotMinutes();
        List<LocalDateTime> candidates = SlotCalculator.candidateStarts(
                date, window.get().start(), window.get().end(), item.getDurationMinutes(), slotMinutes);
        Set<LocalDateTime> occupied = occupiedSlots(staffId, date);

        List<String> available = new ArrayList<>();
        for (LocalDateTime start : candidates) {
            if (!start.isAfter(now)) {
                continue;
            }
            List<LocalDateTime> needed = SlotCalculator.requiredSlots(
                    start, item.getDurationMinutes(), slotMinutes);
            boolean conflict = needed.stream().anyMatch(occupied::contains);
            if (!conflict) {
                available.add(start.format(HH_MM));
            }
        }
        vo.setSlots(available);
        return vo;
    }

    /** 该员工这一天已被占用的槽位 */
    public Set<LocalDateTime> occupiedSlots(Long staffId, LocalDate date) {
        List<BookingSlot> rows = bookingSlotMapper.selectList(
                Wrappers.<BookingSlot>lambdaQuery()
                        .eq(BookingSlot::getStaffId, staffId)
                        .ge(BookingSlot::getSlotStart, LocalDateTime.of(date, LocalTime.MIN))
                        .lt(BookingSlot::getSlotStart, LocalDateTime.of(date.plusDays(1), LocalTime.MIN)));
        return rows.stream().map(BookingSlot::getSlotStart).collect(Collectors.toCollection(HashSet::new));
    }

}
