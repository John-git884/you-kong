package com.youkong.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String title;

    private Boolean active;
}
