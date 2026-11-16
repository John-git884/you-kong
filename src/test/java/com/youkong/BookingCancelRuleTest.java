package com.youkong;

import com.youkong.common.BizException;
import com.youkong.dto.BookingVO;
import com.youkong.dto.CreateBookingRequest;
import com.youkong.entity.Booking;
import com.youkong.entity.Shop;
import com.youkong.mapper.BookingMapper;
import com.youkong.mapper.ShopMapper;
import com.youkong.service.BookingService;
import com.youkong.service.SlotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 取消与爽约的时间规则。
 *
 * 这一组用例守的是「技术上能释放」和「业务上该不该释放」之间的差：
 * 档期一秒就能还回去，但开场前两分钟还回去没有意义，开场之后还回去更是语义错误。
 */
@SpringBootTest
class BookingCancelRuleTest {

    private static final Long SHOP_ID = 1L;
    private static final Long STAFF_ID = 1L;
    private static final Long SERVICE_ID = 1L;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private ShopMapper shopMapper;

    @AfterEach
    void restoreShopConfig() {
        Shop shop = shopMapper.selectById(SHOP_ID);
        shop.setCancelBeforeMinutes(120);
        shopMapper.updateById(shop);
    }

    private void setCancelBefore(int minutes) {
        Shop shop = shopMapper.selectById(SHOP_ID);
        shop.setCancelBeforeMinutes(minutes);
        shopMapper.updateById(shop);
    }

    /** 取最近的周一到周五，避开周末较短的排班 */
    private static LocalDate workday(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek().getValue() > 5) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private BookingVO book(LocalDate day, String time) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setStaffId(STAFF_ID);
        req.setServiceId(SERVICE_ID);
        req.setStartAt(LocalDateTime.of(day, LocalTime.parse(time)));
        req.setCustomerName("测试顾客");
        req.setCustomerPhone("13800138000");
        return bookingService.create(req);
    }

    private int codeOf(Runnable action) {
        BizException e = assertThrows(BizException.class, action::run);
        return e.getErrorCode().getCode();
    }

    @Test
    @DisplayName("提前三天取消：档期立刻回到可约列表，别人马上能约")
    void cancelLongBeforeIsImmediate() {
        LocalDate day = workday(LocalDate.now().plusDays(3));
        String slot = "15:00";

        BookingVO booking = book(day, slot);
        assertFalse(slotService.availability(STAFF_ID, SERVICE_ID, day).getSlots().contains(slot));

        bookingService.cancel(booking.getId());
        List<String> after = slotService.availability(STAFF_ID, SERVICE_ID, day).getSlots();
        assertTrue(after.contains(slot), "取消后档期没有立刻释放");

        // 第二个人能立刻抢到，才算真的「上架」
        BookingVO another = book(day, slot);
        assertEquals("PENDING", another.getStatus());
    }

    @Test
    @DisplayName("距开场不足配置分钟数：拒绝取消，返回 3004")
    void cancelTooLateIsRejected() {
        LocalDate day = workday(LocalDate.now().plusDays(2));
        BookingVO booking = book(day, "16:00");

        // 把门店的「提前取消」窗口调到 30 天，任何未来预约都落在窗口内
        setCancelBefore(60 * 24 * 30);

        assertEquals(3004, codeOf(() -> bookingService.cancel(booking.getId())));
        assertEquals("PENDING", bookingService.listByStaffAndDate(STAFF_ID, day).stream()
                .filter(b -> b.getId().equals(booking.getId()))
                .findFirst().orElseThrow().getStatus());
    }

    @Test
    @DisplayName("门店代客取消不受时限限制")
    void shopCanCancelAnytime() {
        LocalDate day = workday(LocalDate.now().plusDays(2));
        BookingVO booking = book(day, "17:00");
        setCancelBefore(60 * 24 * 30);

        BookingVO cancelled = bookingService.cancelByShop(booking.getId());
        assertEquals("CANCELLED", cancelled.getStatus());
        assertTrue(slotService.availability(STAFF_ID, SERVICE_ID, day).getSlots().contains("17:00"));
    }

    @Test
    @DisplayName("已经开始的单不能取消，只能标记爽约")
    void startedBookingCannotBeCancelled() {
        Long id = insertStartedBooking();

        assertEquals(3004, codeOf(() -> bookingService.cancel(id)));

        BookingVO noShow = bookingService.markNoShow(id);
        assertEquals("NO_SHOW", noShow.getStatus());
    }

    @Test
    @DisplayName("还没开始的单不能标记爽约，返 3005")
    void futureBookingCannotBeNoShow() {
        LocalDate day = workday(LocalDate.now().plusDays(2));
        BookingVO booking = book(day, "18:00");

        assertEquals(3005, codeOf(() -> bookingService.markNoShow(booking.getId())));
    }

    @Test
    @DisplayName("爽约之后不能再取消，状态机拦住")
    void noShowIsTerminal() {
        Long id = insertStartedBooking();
        bookingService.markNoShow(id);

        assertEquals(3002, codeOf(() -> bookingService.cancel(id)));
        assertEquals(3002, codeOf(() -> bookingService.cancelByShop(id)));
    }

    /** 造一条「已经开场」的预约，绕过下单时的时间校验 */
    private Long insertStartedBooking() {
        LocalDateTime startAt = LocalDateTime.now().minusMinutes(30).withSecond(0).withNano(0);
        Booking booking = new Booking();
        booking.setShopId(SHOP_ID);
        booking.setStaffId(STAFF_ID);
        booking.setServiceId(SERVICE_ID);
        booking.setCustomerName("爽约顾客");
        booking.setCustomerPhone("13900139000");
        booking.setStartAt(startAt);
        booking.setEndAt(startAt.plusMinutes(30));
        booking.setStatus("CONFIRMED");
        booking.setVersion(0);
        booking.setCreatedAt(startAt.minusDays(1));
        booking.setUpdatedAt(startAt.minusDays(1));
        bookingMapper.insert(booking);
        return booking.getId();
    }
}
