package com.youkong;

import com.youkong.service.SlotCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotCalculatorTest {

    @Test
    @DisplayName("60 分钟服务在 15 分钟粒度下占用 4 个槽")
    void requiredSlotsForOneHourService() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 10, 0);
        List<LocalDateTime> slots = SlotCalculator.requiredSlots(start, 60, 15);

        assertThat(slots).hasSize(4);
        assertThat(slots.get(0)).isEqualTo(start);
        assertThat(slots.get(3)).isEqualTo(start.plusMinutes(45));
    }

    @Test
    @DisplayName("45 分钟服务向上取整为 3 个槽，避免服务时长被截断")
    void requiredSlotsRoundsUp() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 10, 0);
        assertThat(SlotCalculator.requiredSlots(start, 45, 15)).hasSize(3);
        assertThat(SlotCalculator.requiredSlots(start, 50, 15)).hasSize(4);
    }

    @Test
    @DisplayName("非法的服务时长应直接报错")
    void rejectNonPositiveDuration() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 10, 0);
        assertThatThrownBy(() -> SlotCalculator.requiredSlots(start, 0, 15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("10:00-20:00 营业、60 分钟服务、15 分钟粒度 -> 最后一单 19:00 开始")
    void candidateStartsFitsWholeServiceInsideBusinessHours() {
        List<LocalDateTime> candidates = SlotCalculator.candidateStarts(
                LocalDate.of(2026, 11, 20), LocalTime.of(10, 0), LocalTime.of(20, 0), 60, 15);

        assertThat(candidates).hasSize(37);
        assertThat(candidates.get(0)).isEqualTo(LocalDateTime.of(2026, 11, 20, 10, 0));
        assertThat(candidates).last().isEqualTo(LocalDateTime.of(2026, 11, 20, 19, 0));
    }
}
