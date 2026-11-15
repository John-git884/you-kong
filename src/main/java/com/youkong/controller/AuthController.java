package com.youkong.controller;

import com.youkong.common.ApiResponse;
import com.youkong.common.ErrorCode;
import com.youkong.dto.LoginRequest;
import com.youkong.dto.LoginResponse;
import com.youkong.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(accountService.login(req.getUsername(), req.getPassword()));
    }

    /** 前端启动时用它校验本地 token 是否还有效 */
    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me(Authentication authentication) {
        if (authentication == null) {
            return ApiResponse.fail(ErrorCode.UNAUTHORIZED);
        }
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_OWNER")
                .replace("ROLE_", "");
        return ApiResponse.ok(Map.of("username", authentication.getName(), "role", role));
    }
}
