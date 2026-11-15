package com.youkong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 有空 - 门店预约排班系统
 */
@EnableScheduling
@MapperScan("com.youkong.mapper")
@SpringBootApplication
public class YouKongApplication {

    public static void main(String[] args) {
        SpringApplication.run(YouKongApplication.class, args);
    }
}
