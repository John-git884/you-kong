package com.youkong.common;

/**
 * 预约状态机。
 *
 * PENDING  --confirm-->  CONFIRMED  --complete-->  COMPLETED
 *    |                       |
 *    +------cancel-----------+------>  CANCELLED      （开场前，释放档期）
 *    |                       |
 *    +------no-show----------+------>  NO_SHOW        （开场后，不释放档期）
 *
 * 取消与爽约分开：取消是「提前说好不来了」，档期要还给别人；
 * 爽约是「时间到了人没出现」，档期已经过去，释放没有意义，也不该算进可约率。
 *
 * PENDING 超过 5 分钟未确认，由 ExpireJob 自动取消并释放档期。
 * 流转用 CAS 更新（where status = 期望值）实现，并发下不会互相覆盖。
 */
public final class BookingStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";
    public static final String NO_SHOW = "NO_SHOW";

    private BookingStatus() {
    }
}
