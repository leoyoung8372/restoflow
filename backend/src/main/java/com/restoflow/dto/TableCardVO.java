package com.restoflow.dto;

import lombok.Data;

/**
 * 桌台看板卡片，接口返回给前端的数据对象。
 *
 * <p>前 6 个字段直接来自 dining_table 与 dining_area；
 * 后 3 个字段需要在订单域实现后才有值，目前恒为 null，
 * 前端拿到 null 时对应行不显示，接口结构无需变动。
 *
 * <p>金额与时间用 String 而非数字/日期类型，是为了与
 * docs/api-conventions.md 第 1、7 章约定的格式保持一致。
 */
@Data
public class TableCardVO {

    private Long id;   // 桌台 id

    private String name;   // 桌台名，如 A1、包9

    private Integer seats;   // 座位数，如 4

    private Integer status;   // 1空台 2待下单 3待结账 4已结账

    private Long areaId;   // 所属区域 id

    private String areaName;   // 区域名，如 A区（跨表组装）

    private Integer peopleCount;   // 用餐人数，空台时 null

    private String firstOrderAt;   // 首单时间 yyyy-MM-dd HH:mm:ss，前端据此算用餐时长

    private String amount;   // 消费金额，字符串格式如 "88.00"，空台时 null
}
