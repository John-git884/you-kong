package com.youkong.dto;

import com.youkong.entity.Booking;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingVO {

    private Long id;
    private Long staffId;
    private Long serviceId;
    private String customerName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;

    public static BookingVO from(Booking b) {
        BookingVO vo = new BookingVO();
        vo.setId(b.getId());
        vo.setStaffId(b.getStaffId());
        vo.setServiceId(b.getServiceId());
        vo.setCustomerName(b.getCustomerName());
        vo.setStartAt(b.getStartAt());
        vo.setEndAt(b.getEndAt());
        vo.setStatus(b.getStatus());
        return vo;
    }
}
