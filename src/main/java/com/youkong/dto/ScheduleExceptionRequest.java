package com.youkong.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleExceptionRequest {

    @NotNull(message = "请选择日期")
    private LocalDate workDate;

    /** true 表示整天休息；false 或为空时按 startTime/endTime 上班 */
    private Boolean rest;

    private LocalTime startTime;

    private LocalTime endTime;
}
