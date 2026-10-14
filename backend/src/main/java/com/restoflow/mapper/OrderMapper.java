package com.restoflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restoflow.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问接口。
 *
 * <p>继承 {@link BaseMapper} 后自动获得单表 CRUD 方法，无需手写 SQL。
 * 泛型 {@code <Order>} 指定本 Mapper 操作的是 orders 表。
 *
 * <p>开台时创建订单、下单后改金额、结账时改状态都会用到它。
 */
@Mapper  // 告诉 Spring：这是 Mapper，启动时扫描并生成实现
public interface OrderMapper extends BaseMapper<Order> {
}
