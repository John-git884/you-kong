package com.youkong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotNull(message = "staffId 不能为空")
    private Long staffId;

    @NotNull(message = "serviceId 不能为空")
    private Long serviceId;

    /** 开始时间，格式 2026-11-20T10:00:00，必须是 15 分钟的整数倍 */
    @NotNull(message = "startAt 不能为空")
    private LocalDateTime startAt;

    @NotBlank(message = "customerName 不能为空")
    private String customerName;

    @NotBlank(message = "customerPhone 不能为空")
    private String customerPhone;
}
