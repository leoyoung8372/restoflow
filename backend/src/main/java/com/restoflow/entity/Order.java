package com.restoflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应数据库 orders 表。
 *
 * <p>表名用复数 orders 是因为 order 是 SQL 保留字，所以这里必须显式写 @TableName。
 *
 * <p>金额字段用 BigDecimal 而非 String：本类在 Java 内部参与计算，需要精度无损；
 * 转成 String 是接口层（OrderVO）的事，见 docs/api-conventions.md 第 1 章。
 *
 * <p>status 码值：1待下单 2待结账 3已结账（见 docs/database-design.md 第 4 章）。
 *
 * <p>本类只对应一张表，不含订单明细、桌台等关联对象——
 * 需要它们时由 Service 查出来组装成 VO，不在实体里塞。
 */
@Getter
@Setter
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)  // 主键；数据库自增
    private Long id;

    private String orderNo;   // 订单号，如 202610120001

    private Long tableId;   // 归属桌台（表里无外键，桌台可删除）

    private String tableName;   // 开台时的桌台名快照，桌台删除后历史订单仍可显示

    private Integer peopleCount;   // 用餐人数

    private String remark;   // 桌台备注（开台时填写，选填）

    private Integer status;   // 1待下单 2待结账 3已结账

    // ===== 金额：开台时全为 0，随下单、结账逐步变化 =====
    // 初始给 BigDecimal.ZERO 而不是留 null，避免后续计算到处判空

    private BigDecimal originalAmount = BigDecimal.ZERO;   // 菜品价格合计

    private BigDecimal discountAmount = BigDecimal.ZERO;   // 优惠金额

    private BigDecimal roundOffAmount = BigDecimal.ZERO;   // 抹零金额

    private BigDecimal payableAmount = BigDecimal.ZERO;   // 应收 = 原价 − 优惠 − 抹零

    private BigDecimal receivedAmount = BigDecimal.ZERO;   // 实收

    // ===== 操作人 =====

    private Long openedBy;   // 开台操作人

    private Long settledBy;   // 结账操作人；未结账时为 null

    private LocalDateTime settledAt;   // 结账时间；未结账时为 null

    // ===== 时间：由数据库自动填充，代码不手动赋值 =====

    private LocalDateTime createdAt;   // 开台时间

    private LocalDateTime updatedAt;   // 修改时间
}
