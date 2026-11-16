package com.youkong.controller;

import com.youkong.common.ApiResponse;
import com.youkong.dto.BookingDetailVO;
import com.youkong.dto.BookingVO;
import com.youkong.dto.ScheduleEntry;
import com.youkong.dto.ScheduleExceptionRequest;
import com.youkong.dto.ServiceItemRequest;
import com.youkong.dto.StaffRequest;
import com.youkong.entity.ServiceItem;
import com.youkong.entity.Staff;
import com.youkong.entity.StaffSchedule;
import com.youkong.entity.StaffScheduleException;
import com.youkong.service.AdminService;
import com.youkong.service.BookingService;
import com.youkong.service.BookingQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 店主后台接口。全部挂在 /api/admin/** 下，由 SecurityConfig 统一要求 JWT。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookingQueryService bookingQueryService;
    private final AdminService adminService;
    private final BookingService bookingService;

    // ---------- 预约 ----------

    @GetMapping("/shops/{shopId}/bookings")
    public ApiResponse<List<BookingDetailVO>> bookings(
            @PathVariable Long shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(bookingQueryService.listByShopAndDate(shopId, date));
    }

    // ---------- 服务项目 ----------

    /** 门店代客取消，不受「提前 N 分钟」限制 */
    @PostMapping("/bookings/{id}/cancel")
    public ApiResponse<BookingVO> cancelBooking(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.cancelByShop(id));
    }

    /** 标记爽约：只能在开场之后打，不释放档期 */
    @PostMapping("/bookings/{id}/no-show")
    public ApiResponse<BookingVO> noShow(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.markNoShow(id));
    }

    @GetMapping("/shops/{shopId}/services")
    public ApiResponse<List<ServiceItem>> services(@PathVariable Long shopId) {
        return ApiResponse.ok(adminService.listServices(shopId));
    }

    @PostMapping("/shops/{shopId}/services")
    public ApiResponse<ServiceItem> createService(@PathVariable Long shopId,
                                                  @Valid @RequestBody ServiceItemRequest req) {
        return ApiResponse.ok(adminService.createService(shopId, req));
    }

    @PutMapping("/services/{id}")
    public ApiResponse<ServiceItem> updateService(@PathVariable Long id,
                                                  @Valid @RequestBody ServiceItemRequest req) {
        return ApiResponse.ok(adminService.updateService(id, req));
    }

    @DeleteMapping("/services/{id}")
    public ApiResponse<Void> deleteService(@PathVariable Long id) {
        adminService.deleteService(id);
        return ApiResponse.ok(null);
    }

    // ---------- 员工 ----------

    @GetMapping("/shops/{shopId}/staff")
    public ApiResponse<List<Staff>> staff(@PathVariable Long shopId) {
        return ApiResponse.ok(adminService.listStaff(shopId));
    }

    @PostMapping("/shops/{shopId}/staff")
    public ApiResponse<Staff> createStaff(@PathVariable Long shopId,
                                          @Valid @RequestBody StaffRequest req) {
        return ApiResponse.ok(adminService.createStaff(shopId, req));
    }

    @PutMapping("/staff/{id}")
    public ApiResponse<Staff> updateStaff(@PathVariable Long id,
                                          @Valid @RequestBody StaffRequest req) {
        return ApiResponse.ok(adminService.updateStaff(id, req));
    }

    // ---------- 排班 ----------

    @GetMapping("/staff/{id}/schedules")
    public ApiResponse<List<StaffSchedule>> schedules(@PathVariable Long id) {
        return ApiResponse.ok(adminService.listSchedules(id));
    }

    @PutMapping("/staff/{id}/schedules")
    public ApiResponse<List<StaffSchedule>> saveSchedules(@PathVariable Long id,
                                                          @RequestBody List<ScheduleEntry> entries) {
        return ApiResponse.ok(adminService.saveSchedules(id, entries));
    }

    // ---------- 临时请假 ----------

    @GetMapping("/staff/{id}/exceptions")
    public ApiResponse<List<StaffScheduleException>> exceptions(@PathVariable Long id) {
        return ApiResponse.ok(adminService.listExceptions(id, LocalDate.now()));
    }

    @PostMapping("/staff/{id}/exceptions")
    public ApiResponse<StaffScheduleException> saveException(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleExceptionRequest req) {
        return ApiResponse.ok(adminService.saveException(id, req));
    }

    @DeleteMapping("/exceptions/{id}")
    public ApiResponse<Void> deleteException(@PathVariable Long id) {
        adminService.deleteException(id);
        return ApiResponse.ok(null);
    }
}
