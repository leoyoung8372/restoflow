package com.restoflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 桌台区域实体，对应数据库 dining_area 表。
 *
 * <p>区域用于在看板上把桌台分组展示（如 A区、包房），数据由管理后台维护。
 */
@Getter
@Setter
@TableName("dining_area")  // 指定表名，不依赖"类名转下划线"的默认推断
public class DiningArea {

    @TableId(type = IdType.AUTO)  // 主键；AUTO 表示由数据库自增，不用 MyBatis-Plus 默认的雪花算法
    private Long id;

    private String name;   // 区域名称，如 A区、包房

    private LocalDateTime createdAt;   // 创建时间

    private LocalDateTime updatedAt;   // 修改时间
}
