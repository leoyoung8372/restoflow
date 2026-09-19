# RestoFlow 数据库设计

> **依据**：`docs/logic.md`（业务逻辑）
> **性质**：本文件是**表结构与状态码值的唯一权威来源**。代码、SQL 与本文件冲突时，以本文件为准。
> **维护**：表结构变更属于契约变更——先说、确认、改本文件，再改代码。

---

## 1. 设计决策

| # | 决策 | 取值 |
|---|---|---|
| 1 | 主键 | `BIGINT` 自增，字段名统一 `id` |
| 2 | 状态存储 | `TINYINT` 数字码，码值见第 4 章 |
| 3 | 金额 | `DECIMAL(10,2)`；Java 侧用 `BigDecimal` |
| 4 | 外键 | **加 7 处**，其余引用（2 处）靠快照字段 |
| 5 | 删除策略 | 物理删除，**分两级**：<br>**交易数据**（订单/明细/支付）**永不删除**；<br>**字典数据**（区域/桌台/分类/菜品）可删，父表删除时子表**级联删除**；<br>用户只停用不删除 |
| 6 | 时间字段 | **数据库自动填充**（`DEFAULT CURRENT_TIMESTAMP`）。**禁止 Java 代码手动赋值** |
| 7 | 订单号 | `VARCHAR(20)`，纯数字 `202609130001` |
| 8 | 命名 | 表名、字段名一律 snake_case |

---

## 2. 表清单

| 域 | 表 | 职责 |
|---|---|---|
| 用户 | `user` | 账号、密码、角色、启用状态 |
| 桌台 | `dining_area` | 区域 |
| | `dining_table` | 桌台与状态 |
| 菜品 | `dish_category` | 菜品分类 |
| | `dish` | 菜品与沽清标记 |
| 订单 | `orders` | 订单主表 |
| | `order_item` | 订单明细 |
| | `payment` | 支付记录 |

共 **8 张**。

---

## 3. 表结构

### 3.1 user（用户）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `username` | VARCHAR(50) | 非空、唯一 | **工号**，如 `zhangwei0101`。登录用它 |
| `real_name` | VARCHAR(50) | 非空 | 姓名，如 `张伟`。账单显示用它 |
| `password` | VARCHAR(100) | 非空 | 密码，**BCrypt 哈希存储**（`$2a$` 开头，约 60 字符）。**不存明文、不存明文哈希** |
| `role` | VARCHAR(20) | 非空 | 角色：`admin` / `manager` / `supervisor` / `chef`（**暂定，界面显示中文**） |
| `status` | TINYINT | 非空，默认 1 | 1启用 0停用 |
| `created_at` | DATETIME | 非空 | 创建时间 |
| `updated_at` | DATETIME | 非空 | 修改时间 |

索引：`uk_username (username)`

### 3.2 dining_area（区域）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `name` | VARCHAR(50) | 非空 | 区域名称，如"A区" |
| `created_at` | DATETIME | 非空 | |
| `updated_at` | DATETIME | 非空 | |

### 3.3 dining_table（桌台）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `area_id` | BIGINT | 非空，**FK → dining_area** | 所属区域 |
| `name` | VARCHAR(50) | 非空 | 桌台名，如"A8"、"包9" |
| `seats` | INT | 非空 | 座位数（看板卡片显示） |
| `status` | TINYINT | 非空，默认 1 | 1空台 2待下单 3待结账 4已结账 |
| `created_at` | DATETIME | 非空 | |
| `updated_at` | DATETIME | 非空 | |

索引：`idx_area (area_id)`

### 3.4 dish_category（菜品分类）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `name` | VARCHAR(50) | 非空 | 分类名称，如"热菜" |
| `created_at` | DATETIME | 非空 | |
| `updated_at` | DATETIME | 非空 | |

### 3.5 dish（菜品）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `category_id` | BIGINT | 非空，**FK → dish_category** | 所属分类 |
| `name` | VARCHAR(50) | 非空 | 菜品名称 |
| `price` | DECIMAL(10,2) | 非空 | 单价 |
| `sold_out` | TINYINT | 非空，默认 0 | 0在售 1沽清（**V1 无设置入口，字段先建**） |
| `created_at` | DATETIME | 非空 | |
| `updated_at` | DATETIME | 非空 | |

索引：`idx_category (category_id)`

### 3.6 orders（订单）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `order_no` | VARCHAR(20) | 非空、唯一 | 订单号 `202609130001` |
| `table_id` | BIGINT | 非空，**无外键** | 归属桌台；**桌台可删除**，故不加约束 |
| `table_name` | VARCHAR(50) | 非空 | 桌台名**快照**；桌台删除后历史订单仍可显示。**转台时随 `table_id` 一起更新** |
| `people_count` | INT | 非空 | 用餐人数 |
| `status` | TINYINT | 非空 | 1待下单 2待结账 3已结账 |
| `original_amount` | DECIMAL(10,2) | 非空，默认 0 | 菜品价格合计 |
| `discount_amount` | DECIMAL(10,2) | 非空，默认 0 | 优惠金额 |
| `round_off_amount` | DECIMAL(10,2) | 非空，默认 0 | 抹零金额 |
| `payable_amount` | DECIMAL(10,2) | 非空，默认 0 | 应收 = 原价 − 优惠 − 抹零 |
| `received_amount` | DECIMAL(10,2) | 非空，默认 0 | 实收 |
| `opened_by` | BIGINT | 非空，**FK → user** | 开台操作人 |
| `settled_by` | BIGINT | 可空，**FK → user** | 结账操作人 |
| `settled_at` | DATETIME | 可空 | 结账时间 |
| `created_at` | DATETIME | 非空 | 开台时间 |
| `updated_at` | DATETIME | 非空 | |

索引：`uk_order_no (order_no)`、`idx_table (table_id)`、`idx_status (status)`

> 金额字段全部由**后端计算**，前端只提交原始输入（优惠金额、收款金额）。
> 桌台状态与订单状态**一一对应**（1↔1、2↔2、3↔3），这是刻意的设计。

### 3.7 order_item（订单明细）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `order_id` | BIGINT | 非空，**FK → orders** | 归属订单 |
| `dish_id` | BIGINT | **可空，无外键** | 菜品 id；**菜品可删除**，删除后为 NULL |
| `dish_name` | VARCHAR(50) | 非空 | 下单时**菜名快照** |
| `price` | DECIMAL(10,2) | 非空 | 下单时**单价快照** |
| `quantity` | INT | 非空 | 下单数量（**原始数量，不因退菜而改变**） |
| `refunded_quantity` | INT | 非空，默认 0 | **已退数量**（可部分退；= `quantity` 时为整道退） |
| `refund_reason` | VARCHAR(200) | 可空 | 退菜原因（退菜时必填） |
| `refunded_by` | BIGINT | 可空，**FK → user** | 退菜操作人（取当前登录账号） |
| `refunded_at` | DATETIME | 可空 | 退菜时间 |
| `remark` | VARCHAR(100) | 可空 | 备注 |
| `status` | TINYINT | 非空 | 1待制作 2已出餐 3已退 |
| `created_at` | DATETIME | 非空 | 下单时间 |
| `updated_at` | DATETIME | 非空 | |

索引：`idx_order (order_id)`

> **有效数量** = `quantity` − `refunded_quantity`，订单金额按有效数量计算。
> **退菜只更新本条明细**，不新增行——同一道菜分多次退时，`refunded_quantity` 累加。

### 3.8 payment（支付记录）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK 自增 | |
| `order_id` | BIGINT | 非空，**FK → orders** | 归属订单 |
| `pay_method` | TINYINT | 非空 | 1现金 2支付宝 |
| `amount` | DECIMAL(10,2) | 非空 | 收款金额 |
| `status` | TINYINT | 非空，默认 1 | 1有效 0失效 |
| `operator_id` | BIGINT | 非空，**FK → user** | 收款操作人 |
| `created_at` | DATETIME | 非空 | 收款时间 |

索引：`idx_order (order_id)`、`idx_created_at (created_at)`

> `idx_created_at` 用于**按日期统计收款**（报表查询：`SUM(amount) WHERE status=1 AND created_at 在区间`），详见 `logic.md` 3.5 收款触发机制。

---

## 4. 状态码值对照表

> **本表是全部码值的唯一权威来源。** Java 侧枚举类、前端文案映射均对齐本表。

| 字段 | 码值 |
|---|---|
| `dining_table.status` | 1空台 2待下单 3待结账 4已结账 |
| `orders.status` | 1待下单 2待结账 3已结账 |
| `order_item.status` | 1待制作 2已出餐 3已退 |
| `payment.status` | 0失效 1有效 |
| `payment.pay_method` | 1现金 2支付宝 |
| `user.status` | 0停用 1启用 |
| `dish.sold_out` | 0在售 1沽清 |
| `user.role` | `admin` 管理员 / `manager` 店长 / `supervisor` 主管 / `chef` 厨师长（**暂定，界面显示中文**） |

---

## 5. 表关系

```
dining_area ──1:N──▶ dining_table
                          │
                          │ (orders.table_id，无外键约束)
                          ▼
dish_category ──1:N──▶ dish    orders ──1:N──▶ order_item
                                 │  ▲
                                 │  └── (order_item.dish_id，无外键约束)
                                 │
                                 └──1:N──▶ payment

user ──引用──▶ orders.opened_by / orders.settled_by / payment.operator_id
```

### 外键（7 处）

| # | 子表.字段 | 父表 | 删除行为 |
|---|---|---|---|
| 1 | `dining_table.area_id` | `dining_area` | **CASCADE**（删区域连带删桌台） |
| 2 | `dish.category_id` | `dish_category` | **CASCADE**（删分类连带删菜品） |
| 3 | `orders.opened_by` | `user` | RESTRICT |
| 4 | `orders.settled_by` | `user` | RESTRICT |
| 5 | `order_item.order_id` | `orders` | RESTRICT |
| 6 | `payment.order_id` | `orders` | RESTRICT |
| 7 | `payment.operator_id` | `user` | RESTRICT |

> **CASCADE 只用在字典表内部**（区域→桌台、分类→菜品）。
> 其余全部 RESTRICT：父表被引用时，删除会被数据库直接拒绝——这是订单、支付、用户**不被误删**的兜底保障。

### 不加外键的引用（2 处）

| 字段 | 为什么不加 | 靠什么保证历史可查 |
|---|---|---|
| `orders.table_id` | **桌台可以删除** | `orders.table_name` 快照 |
| `order_item.dish_id` | **菜品可以删除** | `order_item.dish_name` + `price` 快照 |

---

## 6. 删除规则汇总

| 表 | 能否删除 | 条件 |
|---|---|---|
| `user` | ❌ **不能删** | 只能停用（`status=0`）——账单要显示收银员 |
| `dining_area` | ⚠️ 条件删除 | 其下桌台**全为空台**时可删，级联删除这些桌台 |
| `dining_table` | ⚠️ **仅空台可删** | 占用中、已结账的都不能删（代码校验，无外键兜底） |
| `dish_category` | ✅ **可删** | **级联删除**其下所有菜品 |
| `dish` | ✅ 可以删 | 历史订单靠快照不受影响 |
| `orders` | ❌ **不能删** | 订单是凭证 |
| `order_item` | ❌ **不能删** | 随订单一起保留 |
| `payment` | ❌ **不能删** | 支付是凭证；反结账时标记 `status=0` 失效 |

> **桌台的删除条件由代码校验**（数据库没外键拦不住）。占用中的桌台删不掉，需在接口里判断并返回友好提示。

### 级联规则（字典表）

| 删除 | 前置校验 | 级联效果 |
|---|---|---|
| 删除 `dining_area` | **其下桌台必须全为空台** | 这些桌台一并删除（`ON DELETE CASCADE`） |
| 删除 `dish_category` | 无 | 其下所有菜品一并删除（`ON DELETE CASCADE`） |

> **区域的空台校验必须在代码里做**——外键只能保证"删除时下级跟着删"，**拦不住"删掉一个区域连正在用餐的桌台一起没了"**。
>
> 级联删除**只影响字典数据**——订单、明细、支付记录不受任何影响，历史凭证靠快照字段保留。

---

## 7. 不落库的数据

### 7.1 登录 token

**token 存内存，不进数据库。**

| 项 | 决定 |
|---|---|
| 存储位置 | 后端内存（如 `Map<String, 用户信息>`） |
| 有效期 | **固定 6 小时**（不滑动续期） |
| 登出 | 后端删除该 token + 前端清除本地存储，**立即失效** |
| 服务重启 | 所有 token 失效，需重新登录（开发阶段可接受） |
| 停用用户 | 每次请求校验 `user.status`，停用即失效 |

### 7.2 用餐时长

**不新增字段。**

```
首单时间 = 该订单下最早的 order_item.created_at
用餐时长 = 前端用「本地当前时间 − 首单时间」实时计算
```

> 后端只返回首单时间戳，**不返回"时长"**——因为时长每秒都在变，由前端自己算更合理。

**注意**：`dining_table.status = 2（待下单）` 时不显示时长（还没下单，没有起点）。

---

## 8. 待办

| # | 事项 | 何时处理 |
|---|---|---|
| 1 | `user.role` 各角色的**权限划分**（取值已暂定，权限矩阵未定） | 做管理模块时 |
| 2 | 反结账相关字段（是否需要记录反结账次数、操作人） | 做管理后台的反结账功能时 |
| 3 | 建表 SQL | 本文件定稿后生成到 `sql/` 目录 |
