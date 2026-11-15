package com.youkong;

import com.youkong.common.BizException;
import com.youkong.dto.CreateBookingRequest;
import com.youkong.dto.ScheduleExceptionRequest;
import com.youkong.service.AdminService;
import com.youkong.service.BookingService;
import com.youkong.service.SlotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 临时请假：优先级高于周排班，且必须同时影响「查空闲时段」和「下单校验」两条路径。
 */
@SpringBootTest
class ScheduleExceptionTest {

    private static final long STAFF_ID = 1L;
    private static final long SERVICE_ID = 1L;
    private static final LocalDate DAY = workday(LocalDate.now().plusDays(30));

    @Autowired
    private AdminService adminService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("请假当天查不到任何可选时段")
    void restDayHasNoSlots() {
        saveException(true, null, null);

        assertThat(slotService.availability(STAFF_ID, SERVICE_ID, DAY).getSlots()).isEmpty();
    }

    @Test
    @DisplayName("请假当天无法下单")
    void cannotBookOnRestDay() {
        saveException(true, null, null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setStaffId(STAFF_ID);
        request.setServiceId(SERVICE_ID);
        request.setStartAt(DAY.atTime(10, 0));
        request.setCustomerName("测试顾客");
        request.setCustomerPhone("13800000000");

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未排班或已请假");
    }

    @Test
    @DisplayName("特殊营业时间会覆盖周排班")
    void customWindowOverridesWeeklySchedule() {
        saveException(false, LocalTime.of(13, 0), LocalTime.of(15, 0));

        var slots = slotService.availability(STAFF_ID, SERVICE_ID, DAY).getSlots();

        assertThat(slots).isNotEmpty();
        assertThat(slots.get(0)).isEqualTo("13:00");
        assertThat(slots).doesNotContain("10:00", "12:00", "14:45");
    }

    @Test
    @DisplayName("删掉例外后恢复按周排班")
    void deletingExceptionRestoresWeeklySchedule() {
        var created = saveException(true, null, null);
        assertThat(slotService.availability(STAFF_ID, SERVICE_ID, DAY).getSlots()).isEmpty();

        adminService.deleteException(created.getId());

        assertThat(slotService.availability(STAFF_ID, SERVICE_ID, DAY).getSlots()).isNotEmpty();
    }

    private com.youkong.entity.StaffScheduleException saveException(
            boolean rest, LocalTime start, LocalTime end) {
        ScheduleExceptionRequest request = new ScheduleExceptionRequest();
        request.setWorkDate(DAY);
        request.setRest(rest);
        request.setStartTime(start);
        request.setEndTime(end);
        return adminService.saveException(STAFF_ID, request);
    }

    private static LocalDate workday(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek().getValue() > 5) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}
