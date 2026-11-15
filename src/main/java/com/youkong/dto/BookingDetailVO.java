package com.youkong.dto;

import com.youkong.entity.Booking;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带员工名与服务名的预约视图，供客户端列表直接渲染。
 */
@Data
public class BookingDetailVO {

    private Long id;
    private Long staffId;
    private String staffName;
    private Long serviceId;
    private String serviceName;
    private String customerName;
    private String customerPhone;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;

    public static BookingDetailVO from(Booking b) {
        BookingDetailVO vo = new BookingDetailVO();
        vo.setId(b.getId());
        vo.setStaffId(b.getStaffId());
        vo.setServiceId(b.getServiceId());
        vo.setCustomerName(b.getCustomerName());
        vo.setCustomerPhone(b.getCustomerPhone());
        vo.setStartAt(b.getStartAt());
        vo.setEndAt(b.getEndAt());
        vo.setStatus(b.getStatus());
        return vo;
    }
}
