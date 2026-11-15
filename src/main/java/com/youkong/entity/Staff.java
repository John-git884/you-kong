package com.youkong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("staff")
public class Staff {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private String name;

    /** 岗位，如 美甲师 / 发型师 */
    private String title;

    private Boolean active;
}
