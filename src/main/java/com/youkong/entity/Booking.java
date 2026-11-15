package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("booking")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long staffId;

    private Long serviceId;

    private String customerName;

    private String customerPhone;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** PENDING / CONFIRMED / CANCELLED / COMPLETED，见 BookingStatus */
    private String status;

    /** 乐观锁版本号，改期、核销时使用 */
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
