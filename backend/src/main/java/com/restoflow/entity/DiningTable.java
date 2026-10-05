package com.restoflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 桌台实体，对应数据库 dining_table 表。
 *
 * <p>status 码值：1空台 2待下单 3待结账 4已结账（见 docs/database-design.md 第 4 章）。
 */
@Getter
@Setter
@TableName("dining_table")  // 指定表名，不依赖"类名转下划线"的默认推断
public class DiningTable {

    @TableId(type = IdType.AUTO)  // 主键；AUTO 表示由数据库自增，不用 MyBatis-Plus 默认的雪花算法
    private Long id;

    private Long areaId;   // 所属区域

    private String name;   // 桌台名，如 A1、包9

    private Integer seats;   // 座位数

    private Integer status;   // 1空台 2待下单 3待结账 4已结账

    private LocalDateTime createdAt;   // 创建时间

    private LocalDateTime updatedAt;   // 修改时间
}
