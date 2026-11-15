package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 营业开始时间，如 09:00 */
    private LocalTime openTime;

    /** 营业结束时间，如 20:00 */
    private LocalTime closeTime;

    /** 时间槽粒度（分钟），默认 15 */
    private Integer slotMinutes;

    private LocalDateTime createdAt;
}
