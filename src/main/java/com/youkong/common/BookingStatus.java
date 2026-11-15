package com.youkong.common;

/**
 * 预约状态机。
 *
 * PENDING  --confirm-->  CONFIRMED  --complete-->  COMPLETED
 *    |                       |
 *    +------cancel-----------+------>  CANCELLED
 *
 * PENDING 超过 5 分钟未确认，由 ExpireJob 自动取消并释放档期。
 * 流转用 CAS 更新（where status = 期望值）实现，并发下不会互相覆盖。
 */
public final class BookingStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    private BookingStatus() {
    }
}
