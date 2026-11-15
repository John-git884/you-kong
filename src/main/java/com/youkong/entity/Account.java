package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台登录账号。密码只存 BCrypt 哈希，不存明文。
 */
@Data
@TableName("account")
public class Account {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private String username;

    private String passwordHash;

    /** OWNER / MANAGER */
    private String role;

    private LocalDateTime createdAt;
}
