package com.youkong.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AvailabilityVO {

    private LocalDate date;
    private Long staffId;
    private Long serviceId;
    private Integer durationMinutes;
    /** 可选的开始时间，格式 HH:mm */
    private List<String> slots;
}
