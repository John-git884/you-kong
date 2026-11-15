package com.youkong.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.BizException;
import com.youkong.common.ErrorCode;
import com.youkong.dto.LoginResponse;
import com.youkong.entity.Account;
import com.youkong.mapper.AccountMapper;
import com.youkong.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(String username, String rawPassword) {
        Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getUsername, username.trim()));

        // 账号不存在时也跑一次哈希比对，避免通过响应耗时判断账号是否存在
        String hash = account == null ? dummyHash() : account.getPasswordHash();
        boolean matched = passwordEncoder.matches(rawPassword, hash);
        if (account == null || !matched) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtService.issue(account.getId(), account.getUsername(), account.getRole());
        return new LoginResponse(token, account.getUsername(), account.getRole(), jwtService.ttlSeconds());
    }

    /** 库里一个账号都没有时创建初始账号，返回创建结果用于提示 */
    public Optional<Account> createDefaultIfAbsent(Long shopId, String username, String rawPassword) {
        Long count = accountMapper.selectCount(Wrappers.<Account>lambdaQuery());
        if (count != null && count > 0) {
            return Optional.empty();
        }
        Account account = new Account();
        account.setShopId(shopId);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole("OWNER");
        account.setCreatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        return Optional.of(account);
    }

    private String dummyHash() {
        // 固定的占位哈希，只为让未命中账号的请求也付出同等的校验成本
        return "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    }
}
