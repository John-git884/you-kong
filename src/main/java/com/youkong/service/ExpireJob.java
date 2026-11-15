package com.youkong.service;

import com.youkong.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超时未确认预约的自动回收。
 *
 * 当前用定时扫表而不是 Redis ZSet 延迟队列：
 *   - 单店量级下每分钟扫一次 PENDING 小表，成本可以忽略；
 *   - 少一个中间件依赖，部署和排查都简单；
 *   - 扫表天然幂等，进程重启不丢任务，Redis 延迟队列要额外处理重复消费。
 * 量上来之后改成 Redis ZSet 触发 + 扫表兜底，两者互补。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireJob {

    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void releaseExpiredBookings() {
        List<Booking> expired = bookingService.findExpiredPending();
        if (expired.isEmpty()) {
            return;
        }
        int released = 0;
        for (Booking booking : expired) {
            if (bookingService.cancelQuietly(booking.getId())) {
                released++;
            }
        }
        log.info("expire job: scanned={} released={}", expired.size(), released);
    }
}
