package com.youkong.controller;

import com.youkong.common.ApiResponse;
import com.youkong.common.ErrorCode;
import com.youkong.dto.AvailabilityVO;
import com.youkong.dto.BookingDetailVO;
import com.youkong.dto.BookingVO;
import com.youkong.dto.CreateBookingRequest;
import com.youkong.dto.RescheduleRequest;
import com.youkong.service.BookingQueryService;
import com.youkong.service.BookingService;
import com.youkong.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final SlotService slotService;
    private final BookingService bookingService;
    private final BookingQueryService bookingQueryService;

    /** 查询某员工某天还能约的开始时间 */
    @GetMapping("/availability")
    public ApiResponse<AvailabilityVO> availability(
            @RequestParam Long staffId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(slotService.availability(staffId, serviceId, date));
    }

    @PostMapping("/bookings")
    public ApiResponse<BookingVO> create(@Valid @RequestBody CreateBookingRequest req) {
        return ApiResponse.ok(bookingService.create(req));
    }

    @PostMapping("/bookings/{id}/confirm")
    public ApiResponse<BookingVO> confirm(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.confirm(id));
    }

    @PostMapping("/bookings/{id}/complete")
    public ApiResponse<BookingVO> complete(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.complete(id));
    }

    @PostMapping("/bookings/{id}/reschedule")
    public ApiResponse<BookingVO> reschedule(@PathVariable Long id,
                                            @Valid @RequestBody RescheduleRequest req) {
        return ApiResponse.ok(bookingService.reschedule(id, req.getStartAt()));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ApiResponse<BookingVO> cancel(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.cancel(id));
    }

    @GetMapping("/bookings")
    public ApiResponse<List<BookingVO>> list(
            @RequestParam Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(bookingService.listByStaffAndDate(staffId, date));
    }

    /** 顾客端：按手机号查自己的预约 */
    @GetMapping("/bookings/mine")
    public ApiResponse<List<BookingDetailVO>> mine(
            @RequestParam(defaultValue = "1") Long shopId,
            @RequestParam String phone) {
        return ApiResponse.ok(bookingQueryService.listByPhone(shopId, phone));
    }

    @GetMapping("/bookings/{id}")
    public ApiResponse<BookingDetailVO> detail(@PathVariable Long id) {
        BookingDetailVO vo = bookingQueryService.findDetail(id);
        if (vo == null) {
            return ApiResponse.fail(ErrorCode.BOOKING_NOT_FOUND);
        }
        return ApiResponse.ok(vo);
    }
}
