package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 某个员工某一天的特例，优先级高于周排班。
 * 时间为空表示整天休息；填了时间表示当天按这个时段上班。
 */
@Data
@TableName("staff_schedule_exception")
public class StaffScheduleException {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long staffId;

    private LocalDate workDate;

    private LocalTime startTime;

    private LocalTime endTime;

    public boolean isRest() {
        return startTime == null || endTime == null;
    }
}
