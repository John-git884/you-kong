package com.youkong;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.BizException;
import com.youkong.common.ErrorCode;
import com.youkong.dto.CreateBookingRequest;
import com.youkong.dto.ServiceItemRequest;
import com.youkong.entity.BookingSlot;
import com.youkong.entity.ServiceItem;
import com.youkong.mapper.BookingSlotMapper;
import com.youkong.service.AdminService;
import com.youkong.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 改期相关的回归测试。日期选在远期工作日，避免与其它测试用例的档期互相干扰。
 */
@SpringBootTest
class BookingRescheduleTest {

    private static final long STAFF_ID = 1L;
    private static final long SERVICE_ID = 1L;
    private static final LocalDate DAY = workday(LocalDate.now().plusDays(20));

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private BookingSlotMapper bookingSlotMapper;

    @Test
    @DisplayName("改期后旧档期释放、新档期被占用")
    void rescheduleMovesSlots() {
        Long bookingId = book(DAY.atTime(13, 0));

        assertThat(slotExists(DAY.atTime(13, 0))).isTrue();
        assertThat(slotExists(DAY.atTime(15, 0))).isFalse();

        bookingService.reschedule(bookingId, DAY.atTime(15, 0));

        assertThat(slotExists(DAY.atTime(13, 0))).isFalse();
        assertThat(slotExists(DAY.atTime(15, 0))).isTrue();
        assertThat(bookingService.reschedule(bookingId, DAY.atTime(15, 0)).getStartAt())
                .isEqualTo(DAY.atTime(15, 0));
    }

    @Test
    @DisplayName("改到已被占用的时段会失败，且原档期不受影响")
    void rescheduleConflictKeepsOriginalSlots() {
        Long mine = book(DAY.atTime(16, 0));
        book(DAY.atTime(17, 0));

        assertThatThrownBy(() -> bookingService.reschedule(mine, DAY.atTime(17, 0)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.SLOT_TAKEN);

        assertThat(slotExists(DAY.atTime(16, 0))).isTrue();
        assertThat(slotExists(DAY.atTime(17, 0))).isTrue();
    }

    @Test
    @DisplayName("被预约过的服务不能删除")
    void deleteServiceInUseIsRejected() {
        ServiceItemRequest request = new ServiceItemRequest();
        request.setName("待删除服务");
        request.setDurationMinutes(30);
        request.setPriceCent(1000);
        ServiceItem item = adminService.createService(1L, request);

        CreateBookingRequest booking = new CreateBookingRequest();
        booking.setStaffId(STAFF_ID);
        booking.setServiceId(item.getId());
        booking.setStartAt(DAY.atTime(18, 0));
        booking.setCustomerName("测试顾客");
        booking.setCustomerPhone("13800000000");
        bookingService.create(booking);

        assertThatThrownBy(() -> adminService.deleteService(item.getId()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.SERVICE_IN_USE);
    }

    /** 取最近的周一到周五，避开周末较短的排班时间 */
    private static LocalDate workday(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek().getValue() > 5) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private Long book(LocalDateTime startAt) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setStaffId(STAFF_ID);
        request.setServiceId(SERVICE_ID);
        request.setStartAt(startAt);
        request.setCustomerName("测试顾客");
        request.setCustomerPhone("13800000000");
        return bookingService.create(request).getId();
    }

    private boolean slotExists(LocalDateTime slotStart) {
        Long count = bookingSlotMapper.selectCount(Wrappers.<BookingSlot>lambdaQuery()
                .eq(BookingSlot::getStaffId, STAFF_ID)
                .eq(BookingSlot::getSlotStart, slotStart));
        return count != null && count > 0;
    }
}
