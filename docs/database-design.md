# RestoFlow 数据库设计文档

## 1. 文档说明

- **目的**：定义 V1 全部表结构与字段，是建表 SQL 与后端实体类（MyBatis-Plus）的唯一权威依据
- **上游依据**：requirements-analysis.md（字段来源）、business-process.md（状态枚举值）、system-design.md（MySQL 8、金额 DECIMAL、密码 BCrypt）
- **维护规则**：表结构变更须评审通过，同步本文档并记录 CHANGELOG.md，之后方可修改代码

## 2. 设计约定

| 约定项 | 规则 | 理由 / 依据 |
|---|---|---|
| 存储引擎与字符集 | InnoDB，utf8mb4 | 支持事务（NF-02）、完整中文 |
| 命名 | 表名、字段名一律 snake_case；主键统一 `id` BIGINT 自增 | 企业惯例 |
| 金额 | DECIMAL(10,2)，Java 侧 BigDecimal | NF-01，system-design.md |
| 状态存储 | TINYINT 存状态码，码值见第 6 章对照表，Java 侧枚举类映射 | 推荐 TINYINT 而非 VARCHAR 存状态词：企业惯例、避免拼写错误，可读性由枚举类与对照表保证 |
| 时间 | DATETIME；created_at 为创建时间，updated_at 为最后修改时间 | 通用约定 |
| 留痕 | 业务操作记录操作人（关联 user 表 id）与操作时间 | NF-03；user 只停用不删除（AUTH-04），操作人引用安全 |
| 删除策略 | 一律物理删除，不做逻辑删除 | 引用保护靠业务规则（business-process.md 4.7），历史展示靠快照字段（BASE-02③） |
| 外键 | 基础数据内部、订单域内部加外键约束；跨删除边界的引用不加（详见第 5 章） | 见第 5 章理由 |

## 3. 表清单

| 模块 | 表名 | 职责 |
|---|---|---|
| 用户管理 | user | 用户账号、密码（BCrypt）、角色、启用状态 |
| 桌台 | dining_area | 桌台区域 |
| | dining_table | 桌台信息与状态 |
| 基础数据 | dish_category | 菜品分类 |
| | dish | 菜品与沽清标记 |
| 订单 | orders | 订单主表（桌台快照、人数、状态、四个金额字段） |
| | order_item | 订单明细（菜名价格快照、备注、退菜信息） |
| | payment | 支付记录（有效/失效） |
| | order_operation_log | 订单操作记录（NF-03 留痕，ORDER-05② 展示） |

共 9 张表。角色不建表：权限矩阵为固定配置（requirements-analysis.md 3.1），user 表带 role 字段即可。

## 4. 表结构设计

### 4.1 user（用户表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| username | VARCHAR(50) | 非空、唯一 | 登录名（AUTH-01） |
| password | VARCHAR(100) | 非空 | BCrypt 哈希，不存明文（NF-05） |
| real_name | VARCHAR(50) | 非空 | 姓名 |
| role | TINYINT | 非空 | 1服务员 2后厨 3店长 4管理员；固定配置 |
| status | TINYINT | 非空，默认 1 | 1启用 0停用（AUTH-04②） |
| created_at | DATETIME | 非空 | 创建时间 |
| updated_at | DATETIME | 非空 | 修改时间 |

索引：`uk_username (username)`

### 4.2 dining_area（桌台区域表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| name | VARCHAR(50) | 非空 | 区域名称（BASE-03①） |
| created_at | DATETIME | 非空 | 创建时间 |
| updated_at | DATETIME | 非空 | 修改时间 |

### 4.3 dining_table（桌台表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| area_id | BIGINT | 非空，FK → dining_area | 所属区域（BASE-04①） |
| name | VARCHAR(50) | 非空 | 桌台名称（BASE-04①） |
| seats | INT | 非空 | 座位数（BASE-04①） |
| status | TINYINT | 非空，默认 1 | 1空闲 2占用 3待清台（business-process.md 2.1） |
| created_at | DATETIME | 非空 | 创建时间 |
| updated_at | DATETIME | 非空 | 修改时间 |

索引：`idx_area (area_id)`

### 4.4 dish_category（菜品分类表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| name | VARCHAR(50) | 非空 | 分类名称（BASE-01①） |
| created_at | DATETIME | 非空 | 创建时间 |
| updated_at | DATETIME | 非空 | 修改时间 |

### 4.5 dish（菜品表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| category_id | BIGINT | 非空，FK → dish_category | 所属分类（BASE-02①） |
| name | VARCHAR(50) | 非空 | 菜品名称（BASE-02①） |
| price | DECIMAL(10,2) | 非空 | 价格，精确到分（BASE-02②） |
| sold_out | TINYINT | 非空，默认 0 | 0在售 1沽清（KDS-03） |
| created_at | DATETIME | 非空 | 创建时间 |
| updated_at | DATETIME | 非空 | 修改时间 |

索引：`idx_category (category_id)`

### 4.6 orders（订单表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| table_id | BIGINT | 非空，无外键 | 绑定桌台；桌台可删除（BASE-04②），弱引用 |
| table_name | VARCHAR(50) | 非空 | 开台时桌台名快照，桌台删除后历史订单仍显示 |
| people_count | INT | 非空 | 用餐人数 1~99（TABLE-02②） |
| status | TINYINT | 非空 | 1已开台 2已下单 3已结账（business-process.md 2.2） |
| original_amount | DECIMAL(10,2) | 非空，默认 0 | 原价总额 = 有效明细快照价 × 数量合计（PAY-01②） |
| discount_amount | DECIMAL(10,2) | 非空，默认 0 | 优惠金额（PAY-02①） |
| payable_amount | DECIMAL(10,2) | 非空，默认 0 | 应收金额 = 原价 − 优惠 − 抹零（PAY-02③） |
| received_amount | DECIMAL(10,2) | 非空，默认 0 | 已收总额 = 有效支付记录合计（PAY-03②） |
| opened_by | BIGINT | 非空，FK → user | 开台操作人（NF-03） |
| settled_by | BIGINT | 可空，FK → user | 结账操作人 |
| settled_at | DATETIME | 可空 | 结账时间 |
| created_at | DATETIME | 非空 | 开台时间 |
| updated_at | DATETIME | 非空 | 修改时间 |

索引：`idx_table (table_id)`、`idx_status_created (status, created_at)`（ORDER-05① 按状态与日期筛选）

订单状态与明细状态的分工：

- **orders.status 表示订单生命周期**（已开台/已下单/已结账），不代表明细状态；订单没有"已退/退款"状态
- **退菜仅影响 order_item**：明细标记为"已退"，不改变 orders.status，仅触发订单金额重算（ORDER-03②）——original_amount 重算；若 discount_amount > 0 则 payable_amount 同步重算为 original − discount（不抹零）；重算后 payable < received 则拒绝退菜（409）
- 结账后的纠错通过反结账完成（ORDER-04）

字段重置规则（与 business-process.md 3.5、4.2 对应）：

- **反结账**：status 回"已下单"（0 元无明细订单回"已开台"）；支付记录标记失效；received_amount 归 0；discount_amount 归 0、payable_amount 重算为 original_amount；settled_by / settled_at 置空
- **取消结账**：有效收款记录标记失效；received_amount 归 0；discount_amount 归 0、payable_amount 重算为 original_amount（结账流程完全回退，含优惠清除）；订单保持未结、桌台保持占用

### 4.7 order_item（订单明细表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| order_id | BIGINT | 非空，FK → orders | 归属订单 |
| dish_name | VARCHAR(50) | 非空 | 下单时菜名快照（BASE-02③） |
| price | DECIMAL(10,2) | 非空 | 下单时单价快照（BASE-02③） |
| quantity | INT | 非空 | 数量 |
| remark | VARCHAR(100) | 可空 | 口味备注（POS-02） |
| status | TINYINT | 非空 | 1待制作 2已出餐 3已退（business-process.md 2.3） |
| refund_reason | VARCHAR(100) | 可空 | 退菜原因，退菜时必填（ORDER-03①） |
| refunded_by | BIGINT | 可空，FK → user | 退菜操作人（NF-03） |
| refunded_at | DATETIME | 可空 | 退菜时间 |
| created_at | DATETIME | 非空 | 下单时间（KDS-01②） |
| updated_at | DATETIME | 非空 | 修改时间 |

索引：`idx_order (order_id)`、`idx_status_created (status, created_at)`（KDS 轮询待制作/已出餐明细）

### 4.8 payment（支付记录表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| order_id | BIGINT | 非空，FK → orders | 归属订单 |
| pay_method | TINYINT | 非空 | 1现金 2微信 3支付宝（BASE-05 预置） |
| amount | DECIMAL(10,2) | 非空 | 本次收款金额（PAY-03①） |
| status | TINYINT | 非空，默认 1 | 1有效 0失效；反结账与取消结账时标记失效，不删除（ORDER-04③、business-process.md 4.2） |
| operator_id | BIGINT | 非空，FK → user | 收款操作人（PAY-03①，NF-03） |
| created_at | DATETIME | 非空 | 收款时间（PAY-03①） |

索引：`idx_order (order_id)`

说明：0 元订单直接结账，不产生支付记录（PAY-04③）

### 4.9 order_operation_log（订单操作记录表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK 自增 | |
| order_id | BIGINT | 非空，FK → orders | 归属订单 |
| action | TINYINT | 非空 | 1开台 2下单 3退菜 4结账 5反结账 6清台（NF-03） |
| operator_id | BIGINT | 非空，FK → user | 操作人（NF-03） |
| remark | VARCHAR(255) | 可空 | 动作摘要，由后端生成（如"宫保鸡丁×1，米饭×2"） |
| created_at | DATETIME | 非空 | 操作时间 |

索引：`idx_order (order_id)`

说明：ORDER-05② 订单详情"操作记录"的数据来源；一次下单/退菜请求记一条（同一请求内的多条明细合并为一条记录）；remark 为后端在写入时生成的摘要（下单/退菜记菜品清单，开台/结账/反结账/清台可留空），不接受前端输入

## 5. 表关系

```
dining_area 1 ── N dining_table 1 ── N orders 1 ── N order_item
                                 orders 1 ── N payment
dish_category 1 ── N dish        orders 1 ── N order_operation_log
user 1 ── N orders / payment / order_operation_log（操作人引用）
```

- **外键约束（可安全添加）**：`dining_table.area_id → dining_area`、`dish.category_id → dish_category`（上级存在下级时不可删除，business-process.md 4.7）；订单域内部 `order_item / payment / order_operation_log → orders`（订单永不删除）；操作人字段 `→ user`（user 只停用不删除）
- **弱引用（不加外键）**：
  - `orders.table_id`：BASE-04② 允许删除空闲桌台，删除后历史订单仍保留，靠 `table_name` 快照展示桌台名
  - `order_item` 不引用 dish：明细展示（KDS、订单详情）全部依赖菜名/价格快照，菜品可任意删除不受历史明细约束（BASE-02③）；将来需要按菜品统计时再走变更流程

## 6. 状态码值对照表

| 字段 | 码值 |
|---|---|
| dining_table.status | 1空闲 2占用 3待清台 |
| orders.status | 1已开台 2已下单 3已结账 |
| order_item.status | 1待制作 2已出餐 3已退 |
| payment.status | 0失效 1有效 |
| user.role | 1服务员 2后厨 3店长 4管理员 |
| user.status | 0停用 1启用 |
| dish.sold_out | 0在售 1沽清 |
| payment.pay_method | 1现金 2微信 3支付宝 |
| order_operation_log.action | 1开台 2下单 3退菜 4结账 5反结账 6清台 |

码值定义以本文档为唯一权威来源，Java 侧枚举类与后端状态校验均对齐本表。
