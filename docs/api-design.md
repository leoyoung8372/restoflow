# RestoFlow API 接口设计文档

## 1. 文档说明

- **目的**：定义 V1 全部 REST 接口的路径、参数、响应结构与业务规则，是前后端并行开发的唯一契约
- **上游依据**：requirements-analysis.md（接口动作与校验规则的来源）、business-process.md（状态流转与异常场景）、database-design.md（字段与状态码值）、system-design.md（统一返回结构、JWT、权限校验方式）
- **维护规则**：接口变更须评审通过，同步本文档并记录 CHANGELOG.md，之后方可修改代码
- **状态码值**：本文中所有状态字段返回码值（TINYINT），码值定义见 database-design.md 第 6 章；文案映射由前端统一维护

## 2. 通用约定

### 2.1 URL 规范

- 前缀 `/api`；资源名用复数名词；动作用子路径表达，例如 `/api/orders/{id}/settle`
- 路径参数为资源 id（BIGINT）；V1 不引入版本前缀

### 2.2 统一返回结构

```json
{ "code": 0, "message": "ok", "data": { } }
```

- `code`：业务错误码，**V1 恒为 0**（预留字段，暂不启用；错误由 HTTP 状态码 + message 表达）
- `message`：成功时为 `"ok"`；失败时为面向用户的提示文案（如"仅空闲桌台可开台"）
- `data`：成功时为接口约定的数据对象；失败时为 `null`
- 判定成败一律看 HTTP 状态码（200 为成功），前端 axios 拦截器统一处理

### 2.3 认证

- 登录成功后返回 token，前端存 localStorage，后续请求携带请求头 `Authorization: Bearer <token>`，有效期 4 小时（AUTH-01）
- **登出无后端接口**：JWT 无状态，登出即前端清除本地凭证，该客户端后续请求不再携带凭证（AUTH-02，system-design.md 第 6 章）
- 无 token、token 过期或无效 → HTTP 401，message"登录状态已失效，请重新登录"；前端拦截 401 后清除凭证并跳转登录页

### 2.4 权限标注与校验

- 每个接口标注所需模块权限；用户持有任一标注模块的权限即可访问
- 权限校验由后端统一拦截完成（**不能相信前端**），前端不做权限拦截（AUTH-03，system-design.md 第 6 章）
- 无权限 → HTTP 403，message"无权限访问该模块"
- 例外：反结账接口在模块权限之上追加角色校验（仅店长，ORDER-04④）
- **多模块复用语义**：同一资源接口可被多个业务模块复用，权限标注只决定"是否可访问"，**不代表资源归属多个模块**；代码中接口按数据/业务核心归属一个域（如订单详情接口归属订单域，桌台点餐是其使用场景之一），不为使用场景拆 Controller

### 2.5 分页约定

- 仅订单列表使用分页（历史订单会增长）；其余列表 V1 全量返回
- 请求参数 `page`（从 1 起）、`pageSize`（≤100，默认 20）；响应 `PageResult`

### 2.6 数据格式约定

| 项 | 约定 |
|---|---|
| 字段命名 | JSON 一律 camelCase |
| 金额 | 字符串，如 `"128.00"`，后端 BigDecimal 序列化无损（NF-01）；**所有业务金额（原价、优惠、应收、已收、抹零）均由后端计算**，前端仅输入原始数值（菜品选择、优惠金额、收款金额）并展示结果 |
| 时间 | 字符串，`yyyy-MM-dd HH:mm:ss` |
| 状态字段 | 返回码值（TINYINT），码值表见 database-design.md 第 6 章；**前端须集中维护码值→文案/颜色的枚举映射**，不得在页面代码中散落判断状态数字（如 `ORDER_STATUS = {1:'已开台', 2:'已下单', 3:'已结账'}`） |
| 轮询 | 桌台看板与 KDS 列表为轮询接口（间隔 3~5 秒，保证状态改变到前端可见 ≤5 秒：T1−T0 ≤ 5s，NF-04）；前端轮询逻辑统一封装（usePolling），桌台与 KDS 复用同一实现；其余为常规请求 |

### 2.7 错误处理约定

| HTTP 状态码 | 含义 | 典型场景 |
|---|---|---|
| 400 | 参数错误 | 用餐人数超范围、退菜原因缺失、支付方式非法 |
| 401 | 未认证 | 无 token、token 过期/无效（登录失败也返回 401） |
| 403 | 无权限 | 无模块权限、非店长反结账、停用用户登录 |
| 404 | 资源不存在 | 桌台/订单/明细/菜品 id 不存在 |
| 409 | 业务状态冲突 | 状态机不允许的操作：非空闲开台、已出餐退菜、未收齐结账、非待清台反结账、沽清下单、有菜品的分类删除等 |
| 500 | 服务器异常 | 未捕获异常，message"系统异常，请稍后重试" |

失败时 `data = null`，具体原因看 message；**V1 不为每种错误定义业务 code**，出现前端必须程序化区分、message 无法表达的场景时，再评审启用 code。

### 2.8 事务约定

- 涉及多表写入的接口必须在**同一数据库事务**内完成，要么全部生效、要么全部不生效（NF-02）：
  - 开台（订单 + 桌台状态 + 操作日志）
  - 下单（明细 + 订单金额 + 操作日志）
  - 退菜（明细 + 订单金额 + 操作日志）
  - 收款（支付记录 + received_amount 累加）
  - 完成结账（订单 + 桌台状态 + 操作日志）
  - 反结账（订单 + 支付记录失效 + 桌台状态 + 操作日志）
  - 清台（桌台状态 + 操作日志）
- 事务边界在 Service 层（system-design.md 4.1）

## 3. 数据对象约定

对象为 API 传输视角的字段定义：基于 database-design.md 表字段（下划线转驼峰），只增删两类字段——**组装字段**（跨表拼接，如 areaName、operatorName）与**敏感字段**（password 等永不返回）。

### 3.1 User（用户）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| username | string | 登录名 |
| realName | string | 姓名 |
| role | number | 1服务员 2后厨 3店长 4管理员 |
| status | number | 1启用 0停用 |
| createdAt | string | 创建时间 |

> 不含 password。

### 3.2 Area（区域）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| name | string | 区域名称 |

### 3.3 Table（桌台）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| areaId | number | 所属区域 |
| areaName | string | 组装：区域名称（看板按区域分组展示，TABLE-01①） |
| name | string | 桌台名称 |
| seats | number | 座位数 |
| status | number | 1空闲 2占用 3待清台 |
| currentOrderId | number/null | 组装：当前未清台订单 id；空闲桌台为 null；**仅用于 API 返回，不作为 dining_table 持久化字段** |

> currentOrderId 说明：由后端根据桌台状态与订单状态**实时组装**——开台时指向新订单，结账后（待清台）保留指向已结账订单（供反结账定位，ORDER-04），清台后桌台空闲、置为 null。**数据库 dining_table 无此字段**，禁止以冗余列方式持久化；需要时按"桌台当前未清台订单"查询得出。

### 3.4 Category（分类）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| name | string | 分类名称 |

### 3.5 Dish（菜品）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| categoryId | number | 所属分类 |
| categoryName | string | 组装：分类名称（点餐页按分类分组，POS-01①） |
| name | string | 菜品名称 |
| price | string | 单价，精确到分 |
| soldOut | number | 0在售 1沽清（POS-03①） |

### 3.6 OrderSummary（订单列表项）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| tableId | number | 桌台 id（可能已删除，历史订单靠 tableName 展示） |
| tableName | string | 开台时桌台名快照 |
| peopleCount | number | 用餐人数 |
| status | number | 1已开台 2已下单 3已结账 |
| originalAmount | string | 原价总额 |
| discountAmount | string | 优惠金额 |
| payableAmount | string | 应收金额 |
| receivedAmount | string | 已收总额 |
| createdAt | string | 开台时间 |
| settledAt | string/null | 结账时间 |

### 3.7 OrderDetail（订单详情）

OrderSummary 的全部字段，另加：

| 字段 | 类型 | 说明 |
|---|---|---|
| items | OrderItem[] | 消费明细（含已退） |
| payments | Payment[] | 支付记录（含失效） |
| logs | OperationLog[] | 操作记录（ORDER-05②） |

> 服务三类页面：点餐页（TABLE-03）、结账页（PAY-01）、订单详情（ORDER-05②）。

### 3.8 OrderItem（订单明细）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| dishName | string | 下单时菜名快照 |
| price | string | 下单时单价快照 |
| quantity | number | 数量 |
| remark | string/null | 口味备注（POS-02） |
| status | number | 1待制作 2已出餐 3已退 |
| refundReason | string/null | 退菜原因 |
| refundedAt | string/null | 退菜时间 |
| createdAt | string | 下单时间（KDS-01②） |

### 3.9 Payment（支付记录）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| payMethod | number | 1现金 2微信 3支付宝 |
| amount | string | 本次收款金额 |
| status | number | 1有效 0失效 |
| operatorName | string | 组装：收款人姓名（PAY-03①） |
| createdAt | string | 收款时间 |

### 3.10 OperationLog（操作记录）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | |
| action | number | 1开台 2下单 3退菜 4结账 5反结账 6清台 |
| remark | string/null | 动作摘要（后端生成，下单/退菜记菜品清单） |
| operatorName | string | 组装：操作人姓名 |
| createdAt | string | 操作时间 |

### 3.11 KitchenItem（KDS 明细）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | number | 明细 id |
| orderId | number | 归属订单 |
| dishName | string | 菜名（快照） |
| quantity | number | 数量 |
| remark | string/null | 口味备注 |
| status | number | 1待制作 2已出餐 |
| tableName | string | 组装：桌台名快照（KDS-01②） |
| createdAt | string | 下单时间 |

### 3.12 PageResult

| 字段 | 类型 | 说明 |
|---|---|---|
| list | T[] | 当前页数据 |
| total | number | 总条数 |
| page | number | 当前页码 |
| pageSize | number | 每页条数 |

## 4. 接口总览

| # | 方法 | 路径 | 职责 | 权限 |
|---|---|---|---|---|
| 1 | POST | /api/auth/login | 登录 | 无 |
| 2 | GET | /api/tables | 桌台看板列表（轮询） | 桌台 或 基础数据 |
| 3 | POST | /api/tables/{id}/open | 开台 | 桌台 |
| 4 | GET | /api/orders/{id} | 订单详情 | 桌台 或 订单 |
| 5 | POST | /api/orders/{id}/items | 下单/加菜 | 桌台 |
| 6 | POST | /api/tables/{id}/clear | 清台 | 桌台 |
| 7 | GET | /api/dishes | 菜品列表（点餐页/维护页） | 桌台 或 基础数据 |
| 8 | GET | /api/orders | 订单列表（筛选+分页） | 订单 |
| 9 | POST | /api/orders/{id}/items/{itemId}/refund | 退菜 | 订单 |
| 10 | POST | /api/orders/{id}/checkout/discount | 设置优惠与抹零 | 订单 |
| 11 | POST | /api/orders/{id}/payments | 收款 | 订单 |
| 12 | POST | /api/orders/{id}/checkout/cancel | 取消结账（作废本次收款，清除优惠） | 订单 |
| 13 | POST | /api/orders/{id}/settle | 完成结账 | 订单 |
| 14 | POST | /api/orders/{id}/reverse | 反结账 | 订单，且仅店长 |
| 15 | GET | /api/kitchen/items | KDS 明细列表（轮询） | 厨房 KDS |
| 16 | POST | /api/kitchen/items/{itemId}/done | 出餐 | 厨房 KDS |
| 17 | PUT | /api/dishes/{id}/sold-out | 沽清/恢复在售 | 厨房 KDS |
| 18 | GET | /api/categories | 分类列表 | 基础数据 |
| 19 | POST | /api/categories | 新增分类 | 基础数据 |
| 20 | PUT | /api/categories/{id} | 修改分类 | 基础数据 |
| 21 | DELETE | /api/categories/{id} | 删除分类 | 基础数据 |
| 22 | POST | /api/dishes | 新增菜品 | 基础数据 |
| 23 | PUT | /api/dishes/{id} | 修改菜品 | 基础数据 |
| 24 | DELETE | /api/dishes/{id} | 删除菜品 | 基础数据 |
| 25 | GET | /api/areas | 区域列表 | 基础数据 |
| 26 | POST | /api/areas | 新增区域 | 基础数据 |
| 27 | PUT | /api/areas/{id} | 修改区域 | 基础数据 |
| 28 | DELETE | /api/areas/{id} | 删除区域 | 基础数据 |
| 29 | POST | /api/tables | 新增桌台 | 基础数据 |
| 30 | PUT | /api/tables/{id} | 修改桌台 | 基础数据 |
| 31 | DELETE | /api/tables/{id} | 删除桌台 | 基础数据 |
| 32 | GET | /api/users | 用户列表 | 用户管理 |
| 33 | POST | /api/users | 新增用户 | 用户管理 |
| 34 | PUT | /api/users/{id} | 修改用户 | 用户管理 |
| 35 | PUT | /api/users/{id}/status | 停用/启用用户 | 用户管理 |

> 权限为模块集合：用户持有任一即可访问；接口 14 在订单模块之上追加"仅店长角色"。

## 5. 接口详述

> 全局规则不再逐接口重复：401 未认证与 403 无权限由统一拦截处理；所有失败响应 `data=null`。

### 5.1 认证

#### 1. 登录

`POST /api/auth/login`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 登录名 |
| password | string | 是 | 明文密码（仅用于本次校验） |

成功 `data`：

```json
{ "token": "eyJhbGciOi...", "user": { ...User } }
```

规则与错误（AUTH-01）：

- 用户名或密码错误 → 401，"用户名或密码错误"
- 停用用户 → 403，"账号已停用，请联系管理员"（AUTH-04②）
- token 有效期 4 小时；前端保存后随请求携带

登出：无接口，前端清除本地凭证（AUTH-02，见 2.3）。

### 5.2 桌台模块

#### 2. 桌台看板列表（轮询）

`GET /api/tables`

无请求参数。成功 `data`：`Table[]`（含各区域全部桌台，前端按 areaName 分组）。

依据：TABLE-01、NF-04。

#### 3. 开台

`POST /api/tables/{id}/open`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| peopleCount | number | 是 | 用餐人数 |

规则与错误（TABLE-02、ORDER-01）：

- 桌台不存在 → 404
- 桌台非空闲 → 409，"仅空闲桌台可开台"
- peopleCount 非 1~99 整数 → 400
- 行为：创建订单（状态"已开台"、金额 0、opened_by=当前用户、table_name 快照）→ 桌台变"占用"→ 记录操作日志（开台）
- 成功 `data`：`OrderDetail`（新订单，items 为空）

#### 4. 订单详情

`GET /api/orders/{id}`

无请求参数。成功 `data`：`OrderDetail`。

- 订单不存在 → 404
- 依据：TABLE-03（点餐页）、PAY-01（结账页）、ORDER-05②（订单详情）

#### 5. 下单 / 加菜

`POST /api/orders/{id}/items`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| items | array | 是 | 购物车条目，至少 1 条 |
| items[].dishId | number | 是 | 菜品 id |
| items[].quantity | number | 是 | 数量，≥1 整数 |
| items[].remark | string | 否 | 口味备注 |

规则与错误（POS-03、POS-04、ORDER-02）：

- 订单不存在 → 404；订单已结账 → 409，"已结账订单不可下单"
- 菜品不存在 → 404；菜品已沽清 → 409，"菜品已沽清：<菜名>"（POS-03②）
- 行为：按 dishId 读取菜名与单价，**以快照落库**（database-design.md 4.7）；订单状态"已开台"→"已下单"；original_amount 重算；记录操作日志（下单，remark 为菜品清单摘要，如"宫保鸡丁×1，米饭×2"）
- 成功 `data`：`OrderDetail`

#### 6. 清台

`POST /api/tables/{id}/clear`

无请求 body。规则与错误（TABLE-04）：

- 桌台不存在 → 404；桌台非待清台 → 409，"仅待清台桌台可清台"
- 行为：桌台变"空闲"，currentOrderId 置空；记录操作日志（清台，归属该桌台结账订单）
- 成功 `data`：`Table`

#### 7. 菜品列表

`GET /api/dishes`

无请求参数。成功 `data`：`Dish[]`（含 categoryName，点餐页按分类分组；含 soldOut 标记）。

依据：POS-01①、BASE-02（维护页复用）。

### 5.3 订单模块

#### 8. 订单列表

`GET /api/orders`

| 查询参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| startDate | string | 否 | 开台起始日期，yyyy-MM-dd |
| endDate | string | 否 | 开台截止日期，yyyy-MM-dd |
| status | number | 否 | 1已开台 2已下单 3已结账 |
| page | number | 否 | 默认 1 |
| pageSize | number | 否 | 默认 20，≤100 |

成功 `data`：`PageResult<OrderSummary>`。依据：ORDER-05①。

#### 9. 退菜

`POST /api/orders/{id}/items/{itemId}/refund`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| reason | string | 是 | 退菜原因 |

规则与错误（ORDER-03，business-process.md 4.3）：

- 订单不存在 → 404；明细不存在或不属于该订单 → 404
- 订单已结账 → 409，"已结账订单不可退菜，需先反结账"（ORDER-03③）
- 明细非"待制作" → 409，"已出餐明细不可退"
- reason 为空 → 400，"退菜原因必填"
- 行为：明细变"已退"（refund_reason/refunded_by/refunded_at）；original_amount 重算（ORDER-03②）；若 discount_amount > 0 则 payable_amount 同步重算为 original − discount（不抹零）；重算后 payable < received → 409，"应收金额低于已收总额，不能退菜"；记录操作日志（退菜，remark 为退菜摘要）
- 成功 `data`：`OrderDetail`

#### 10. 设置优惠与抹零

`POST /api/orders/{id}/checkout/discount`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| discountAmount | string | 是 | 优惠金额 |
| roundOff | boolean | 是 | 是否抹零 |

规则与错误（PAY-02）：

- 订单不存在 → 404；订单已结账 → 409
- discountAmount 非金额格式或 < 0 → 400
- discountAmount > original_amount → 409，"优惠金额不能超过原价总额"（PAY-02②）
- 重算：payable = original − discount；roundOff=true 时 payable 舍去角分（PAY-02③）；重算后 payable < received → 409，"已收总额不能超过应收金额"（PAY-03③）
- 行为：写入 discount_amount、payable_amount（金额均由后端计算）
- 成功 `data`：`OrderDetail`

#### 11. 收款

`POST /api/orders/{id}/payments`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| payMethod | number | 是 | 1现金 2微信 3支付宝 |
| amount | string | 是 | 本次收款金额 |

规则与错误（PAY-03）：

- 订单不存在 → 404；订单已结账 → 409
- payMethod 不在 1/2/3 → 400，"支付方式无效"
- amount ≤ 0 或非金额格式 → 400；已收总额 + 本次 > 应收金额 → 409，"已收总额不能超过应收金额"（PAY-03③）
- 行为：插入支付记录（有效，operator_id=当前用户）；received_amount 累加
- 成功 `data`：`OrderDetail`

#### 12. 取消结账

`POST /api/orders/{id}/checkout/cancel`

无请求 body。规则与错误（business-process.md 4.2）：

- 订单不存在 → 404；订单已结账 → 409
- "当前有效支付记录"的精确定义：该订单 **status=1 的全部支付记录**。V1 状态机下它等价于"本次结账流程中产生的收款"——反结账已使历史记录失效（#14），结账完成后本接口不可调用（409），故取消时该订单的有效记录只可能来自本次结账流程
- 行为：上述有效支付记录全部标记失效；received_amount 归 0；discount_amount 归 0、payable_amount 重算为原价总额（结账流程完全回退，含优惠清除）；订单保持未结、桌台保持占用
- 成功 `data`：`OrderDetail`

#### 13. 完成结账

`POST /api/orders/{id}/settle`

无请求 body。规则与错误（PAY-04）：

- 订单不存在 → 404；订单已结账 → 409
- 已收总额 ≠ 应收金额 → 409，"已收总额与应收金额不一致，不能完成结账"（PAY-03②）
- 行为：订单变"已结账"（settled_by/at=当前用户/当前时间）；桌台变"待清台"；记录操作日志（结账）
- 0 元订单：已收=应收=0 自然满足校验，直接结账，无支付记录（PAY-04③）
- 成功 `data`：`OrderDetail`

#### 14. 反结账

`POST /api/orders/{id}/reverse`

无请求 body。**权限：订单模块，且仅店长角色**（ORDER-04④），非店长 → 403，"仅店长可执行反结账"。

规则与错误（ORDER-04，business-process.md 3.5）：

- 订单不存在 → 404；订单未结账 → 409，"仅已结账订单可反结账"
- 桌台非待清台 → 409，"仅待清台桌台可反结账"（ORDER-04⑤）
- 行为：支付记录标记失效（不删除，ORDER-04③）；订单变"已下单"（0 元无明细订单变"已开台"）；received_amount 归 0，discount_amount 归 0、payable_amount 重算为原价总额，settled_by/at 置空；桌台变"占用"；记录操作日志（反结账）
- 成功 `data`：`OrderDetail`

### 5.4 厨房模块

#### 15. KDS 明细列表（轮询）

`GET /api/kitchen/items`

无请求参数。成功 `data`：`KitchenItem[]`（**仅未结账订单的明细**：status 为 1待制作、2已出餐；已退明细不展示；结账完成后该单明细从 KDS 消失；前端按 status 分区渲染）。

依据：KDS-01、NF-04。

#### 16. 出餐

`POST /api/kitchen/items/{itemId}/done`

无请求 body。规则与错误（KDS-02）：

- 明细不存在 → 404；明细非"待制作" → 409，"仅待制作明细可出餐"
- 行为：明细变"已出餐"
- 成功 `data`：`KitchenItem`

#### 17. 沽清 / 恢复在售

`PUT /api/dishes/{id}/sold-out`

| 请求 body | 类型 | 必填 | 说明 |
|---|---|---|---|
| soldOut | number | 是 | 1沽清 0恢复在售 |

规则与错误（KDS-03）：

- 菜品不存在 → 404
- 行为：更新菜品沽清标记，点餐端立即生效（POS-03①）
- 成功 `data`：`Dish`

### 5.5 基础数据模块

#### 18~21. 分类维护

| # | 接口 | 请求 body | 说明 |
|---|---|---|---|
| 18 | GET /api/categories | — | 成功 `data`：`Category[]` |
| 19 | POST /api/categories | `{ name }` | name 非空（400）；成功 `data`：`Category` |
| 20 | PUT /api/categories/{id} | `{ name }` | 分类不存在 → 404；成功 `data`：`Category` |
| 21 | DELETE /api/categories/{id} | — | 存在菜品 → 409，"存在菜品的分类不可删除"（BASE-01②） |

#### 22~24. 菜品维护

| # | 接口 | 请求 body | 说明 |
|---|---|---|---|
| 22 | POST /api/dishes | `{ categoryId, name, price }` | 分类不存在 → 404；price 非金额或 < 0 → 400（BASE-02②）；成功 `data`：`Dish` |
| 23 | PUT /api/dishes/{id} | `{ categoryId, name, price }` | 同上校验；成功 `data`：`Dish` |
| 24 | DELETE /api/dishes/{id} | — | 物理删除；历史订单靠快照展示，不受影响（BASE-02③） |

#### 25~28. 区域维护

| # | 接口 | 请求 body | 说明 |
|---|---|---|---|
| 25 | GET /api/areas | — | 成功 `data`：`Area[]` |
| 26 | POST /api/areas | `{ name }` | name 非空（400）；成功 `data`：`Area` |
| 27 | PUT /api/areas/{id} | `{ name }` | 区域不存在 → 404；成功 `data`：`Area` |
| 28 | DELETE /api/areas/{id} | — | 存在桌台 → 409，"存在桌台的区域不可删除"（BASE-03②） |

#### 29~31. 桌台维护

| # | 接口 | 请求 body | 说明 |
|---|---|---|---|
| 29 | POST /api/tables | `{ areaId, name, seats }` | 区域不存在 → 404；seats < 1 → 400（BASE-04①）；成功 `data`：`Table` |
| 30 | PUT /api/tables/{id} | `{ areaId, name, seats }` | 同上校验；成功 `data`：`Table` |
| 31 | DELETE /api/tables/{id} | — | 桌台占用或待清台 → 409，"占用或待清台桌台不可删除"（BASE-04②） |

### 5.6 用户管理模块

#### 32~35. 用户维护

| # | 接口 | 请求 body | 说明 |
|---|---|---|---|
| 32 | GET /api/users | — | 成功 `data`：`User[]` |
| 33 | POST /api/users | `{ username, password, realName, role }` | username 已存在 → 409，"用户名已存在"；role 非 1~4 → 400；密码 BCrypt 哈希存储（NF-05）；成功 `data`：`User` |
| 34 | PUT /api/users/{id} | `{ realName?, role?, password? }` | 修改姓名/角色/重置密码；password 留空表示不修改；username 不可修改；成功 `data`：`User` |
| 35 | PUT /api/users/{id}/status | `{ status: 0或1 }` | 停用/启用（AUTH-04）；成功 `data`：`User` |

依据：AUTH-04①（管理员创建用户并分配角色）、AUTH-04②（停用用户无法登录）。
