package com.youkong.common;

import lombok.Getter;

/**
 * 业务返回码。按段划分：1xxx 参数、2xxx 档期、3xxx 状态流转，方便前端分类处理。
 */
@Getter
public enum ErrorCode {

    OK(0, "ok"),

    PARAM_INVALID(1001, "参数不合法"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),

    SLOT_TAKEN(2001, "该时段刚被别人约走了，请换一个"),
    SLOT_OUT_OF_SCHEDULE(2002, "该时段不在员工的排班时间内"),
    SLOT_IN_PAST(2003, "不能预约过去的时间"),

    BOOKING_NOT_FOUND(3001, "预约不存在"),
    BOOKING_STATUS_ILLEGAL(3002, "当前预约状态不允许此操作"),
    SERVICE_IN_USE(3003, "该服务已有预约记录，不能删除"),
    CANCEL_TOO_LATE(3004, "已超过可取消时间，请联系门店"),
    NOT_STARTED(3005, "预约还没开始，不能标记为爽约"),

    UNAUTHORIZED(4001, "登录已失效，请重新登录"),
    LOGIN_FAILED(4002, "账号或密码不正确"),

    INTERNAL_ERROR(5001, "服务器开小差了，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
