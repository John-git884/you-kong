package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

/**
 * 员工周排班模板。1=周一 ... 7=周日。
 * 一期不做临时请假，二期用 staff_schedule_exception 表叠加。
 */
@Data
@TableName("staff_schedule")
public class StaffSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long staffId;

    private Integer dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;
}
