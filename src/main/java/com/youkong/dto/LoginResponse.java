package com.youkong.dto;

/** token 放响应体，前端存起来并放进 Authorization 头 */
public record LoginResponse(String token, String username, String role, long expiresIn) {
}
