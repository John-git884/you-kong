package com.youkong;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.BizException;
import com.youkong.common.ErrorCode;
import com.youkong.dto.CreateBookingRequest;
import com.youkong.entity.BookingSlot;
import com.youkong.mapper.BookingSlotMapper;
import com.youkong.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发争抢同一档期，只能有一个成功。
 *
 * 断言两件事：成功数为 1；占用槽位数恰好等于服务时长对应的格数，
 * 以此确认失败的事务完整回滚，没有留下半截占用。
 */
@SpringBootTest
class BookingConcurrencyTest {

    private static final int THREADS = 20;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingSlotMapper bookingSlotMapper;

    @Test
    @DisplayName("20 个线程抢同一个档期，只有 1 个成功，且槽位数不会超")
    void onlyOneBookingWins() throws Exception {
        LocalDateTime startAt = LocalDate.now().plusDays(1).atTime(10, 0);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setStaffId(1L);
        req.setServiceId(1L);
        req.setStartAt(startAt);
        req.setCustomerName("测试用户");
        req.setCustomerPhone("13800000000");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger slotTaken = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();
        List<Throwable> unexpectedCauses = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    bookingService.create(req);
                    success.incrementAndGet();
                } catch (BizException e) {
                    if (e.getErrorCode() == ErrorCode.SLOT_TAKEN) {
                        slotTaken.incrementAndGet();
                    } else {
                        unexpected.incrementAndGet();
                        unexpectedCauses.add(e);
                    }
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                    unexpectedCauses.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        long occupied = bookingSlotMapper.selectCount(Wrappers.<BookingSlot>lambdaQuery()
                .eq(BookingSlot::getStaffId, 1L)
                .ge(BookingSlot::getSlotStart, startAt)
                .lt(BookingSlot::getSlotStart, startAt.plusHours(1)));

        System.out.printf("success=%d slotTaken=%d unexpected=%d occupiedSlots=%d%n",
                success.get(), slotTaken.get(), unexpected.get(), occupied);

        assertThat(success.get()).isEqualTo(1);
        assertThat(occupied).isEqualTo(2); // 服务 30 分钟 = 2 个槽
        assertThat(unexpected.get()).isZero();
    }
}
