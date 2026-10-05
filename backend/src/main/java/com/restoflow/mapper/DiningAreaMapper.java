package com.restoflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restoflow.entity.DiningArea;
import org.apache.ibatis.annotations.Mapper;

/**
 * 桌台区域数据访问接口。
 *
 * <p>继承 {@link BaseMapper} 后自动获得单表 CRUD 方法，无需手写 SQL。
 * 泛型 {@code <DiningArea>} 指定本 Mapper 操作的是 dining_area 表。
 */
@Mapper  // 告诉 Spring：这是 Mapper，启动时扫描并生成实现
public interface DiningAreaMapper extends BaseMapper<DiningArea> {
}
