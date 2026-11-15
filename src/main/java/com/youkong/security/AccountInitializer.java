package com.youkong.security;

import com.youkong.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 首次启动时写入初始后台账号，密码来自配置，库里存的是 BCrypt 哈希。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountInitializer implements ApplicationRunner {

    private final AccountService accountService;

    @Value("${youkong.security.default-username:owner}")
    private String defaultUsername;

    @Value("${youkong.security.default-password:youkong123}")
    private String defaultPassword;

    @Value("${youkong.security.default-shop-id:1}")
    private Long defaultShopId;

    @Override
    public void run(ApplicationArguments args) {
        accountService.createDefaultIfAbsent(defaultShopId, defaultUsername, defaultPassword)
                .ifPresent(account -> log.warn(
                        "已创建初始后台账号 [{}]，上线前请通过 ADMIN_PASSWORD 环境变量替换默认密码",
                        account.getUsername()));
    }
}
