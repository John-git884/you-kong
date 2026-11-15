package com.youkong.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceItemRequest {

    @NotBlank(message = "服务名称不能为空")
    private String name;

    @NotNull(message = "服务时长不能为空")
    @Min(value = 15, message = "服务时长不能少于 15 分钟")
    @Max(value = 480, message = "服务时长不能超过 480 分钟")
    private Integer durationMinutes;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负")
    private Integer priceCent;
}
