package com.youkong.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间槽计算。纯函数、无依赖，方便单测。
 *
 * 把时间离散成格子，区间重叠问题就变成了唯一约束问题；
 * 代价是精度固定为 slotMinutes，换来的是并发正确性由数据库保证。
 */
public final class SlotCalculator {

    private SlotCalculator() {
    }

    /**
     * 一次服务需要占用哪些槽位。
     * 例如服务 60 分钟、粒度 15 分钟、从 10:00 开始 -> [10:00, 10:15, 10:30, 10:45]
     */
    public static List<LocalDateTime> requiredSlots(LocalDateTime startAt, int durationMinutes, int slotMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        int count = (int) Math.ceil(durationMinutes / (double) slotMinutes);
        List<LocalDateTime> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(startAt.plusMinutes((long) i * slotMinutes));
        }
        return slots;
    }

    /**
     * 某一天所有可能作为"开始时间"的候选点。
     * 只要整段服务放得进营业时间，就算候选。
     */
    public static List<LocalDateTime> candidateStarts(LocalDate date,
                                                      LocalTime openTime,
                                                      LocalTime closeTime,
                                                      int durationMinutes,
                                                      int slotMinutes) {
        LocalDateTime cursor = LocalDateTime.of(date, openTime);
        LocalDateTime close = LocalDateTime.of(date, closeTime);
        List<LocalDateTime> candidates = new ArrayList<>();
        while (!cursor.plusMinutes(durationMinutes).isAfter(close)) {
            candidates.add(cursor);
            cursor = cursor.plusMinutes(slotMinutes);
        }
        return candidates;
    }
}
