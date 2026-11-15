package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时间槽占用表 —— 整个系统的心脏。
 *
 * 设计要点：
 * 1. 把时间切成固定粒度（15min）的格子，一格一行，占用 = 插一行。
 * 2. 唯一索引 (staff_id, slot_start) 是防超卖的唯一真相来源。
 *    并发插入时数据库层直接拒绝，不依赖应用层的锁。
 * 3. 一次 60 分钟的服务 = 同一事务里插 4 行，要么全成要么全败。
 * 4. 取消预约 = 删掉 booking_id 对应的行，档期自然释放。
 */
@Data
@TableName("booking_slot")
public class BookingSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long staffId;

    private LocalDateTime slotStart;

    private Long bookingId;

    public static BookingSlot of(Long staffId, LocalDateTime slotStart, Long bookingId) {
        BookingSlot s = new BookingSlot();
        s.setStaffId(staffId);
        s.setSlotStart(slotStart);
        s.setBookingId(bookingId);
        return s;
    }
}
