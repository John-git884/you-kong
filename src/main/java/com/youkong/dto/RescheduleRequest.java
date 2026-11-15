package com.youkong.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleRequest {

    @NotNull(message = "startAt 不能为空")
    private LocalDateTime startAt;
}
