package com.restoflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体，对应数据库 user 表。
 *
 * <p>字段名与数据库列的对应关系（下划线 → 驼峰，由 MyBatis-Plus 自动转换）：
 * real_name → realName，created_at → createdAt，updated_at → updatedAt。
 *
 * <p>创建/修改时间由数据库自动填充，代码不手动赋值。
 */
@Getter  // 生成所有字段的 getter（MyBatis-Plus 取数据时需要）
@Setter  // 生成所有字段的 setter（MyBatis-Plus 填数据时需要）
@TableName("user")  // 指定对应的表名；不写的话 MyBatis-Plus 按类名猜，虽然也猜对但不明确
public class User {

    @TableId(type = IdType.AUTO)  // 标在主键上：数据库自增，插入时不用传值
    private Long id;

    private String username;

    private String realName;   // 对应列 real_name

    private String password;

    private String role;

    private Integer status;

    private LocalDateTime createdAt;   // 对应列 created_at

    private LocalDateTime updatedAt;   // 对应列 updated_at
}
