package com.youkong.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.entity.Shop;
import com.youkong.entity.StaffSchedule;
import com.youkong.entity.StaffScheduleException;
import com.youkong.mapper.StaffScheduleExceptionMapper;
import com.youkong.mapper.StaffScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 把「周排班 + 临时请假 + 店铺营业时间」合成员工当天真正能上班的时段。
 *
 * 优先级：临时请假 > 周排班 > 不排班；最后再和店铺营业时间取交集。
 * 查空闲时段和校验下单都走这里，避免两处各写一套判断。
 */
@Component
@RequiredArgsConstructor
public class ScheduleResolver {

    private final StaffScheduleMapper staffScheduleMapper;
    private final StaffScheduleExceptionMapper exceptionMapper;

    public record WorkWindow(LocalTime start, LocalTime end) {
    }

    public Optional<WorkWindow> window(Long staffId, Shop shop, LocalDate date) {
        LocalTime from;
        LocalTime to;

        StaffScheduleException exception = exceptionMapper.selectOne(
                Wrappers.<StaffScheduleException>lambdaQuery()
                        .eq(StaffScheduleException::getStaffId, staffId)
                        .eq(StaffScheduleException::getWorkDate, date));

        if (exception != null) {
            if (exception.isRest()) {
                return Optional.empty();
            }
            from = exception.getStartTime();
            to = exception.getEndTime();
        } else {
            StaffSchedule weekly = staffScheduleMapper.selectOne(
                    Wrappers.<StaffSchedule>lambdaQuery()
                            .eq(StaffSchedule::getStaffId, staffId)
                            .eq(StaffSchedule::getDayOfWeek, date.getDayOfWeek().getValue()));
            if (weekly == null) {
                return Optional.empty();
            }
            from = weekly.getStartTime();
            to = weekly.getEndTime();
        }

        if (shop.getOpenTime().isAfter(from)) {
            from = shop.getOpenTime();
        }
        if (shop.getCloseTime().isBefore(to)) {
            to = shop.getCloseTime();
        }
        return from.isBefore(to) ? Optional.of(new WorkWindow(from, to)) : Optional.empty();
    }
}
