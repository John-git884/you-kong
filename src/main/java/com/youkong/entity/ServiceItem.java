package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("service_item")
public class ServiceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private String name;

    /** 服务时长（分钟），决定占用几个时间槽 */
    private Integer durationMinutes;

    /** 价格，单位：分。用整数存钱，避免浮点误差 */
    private Integer priceCent;
}
