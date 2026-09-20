package com.restoflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restoflow.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口。
 *
 * <p>继承 {@link BaseMapper} 后自动获得单表 CRUD 方法（selectById、insert、updateById 等），
 * 无需手写 SQL、也无需写实现类——MyBatis-Plus 在启动时生成实现。
 *
 * <p>泛型 {@code <User>} 指定本 Mapper 操作的是 user 表。
 */
@Mapper  // 告诉 Spring：这是 Mapper，启动时扫描并生成实现
public interface UserMapper extends BaseMapper<User> {
}
