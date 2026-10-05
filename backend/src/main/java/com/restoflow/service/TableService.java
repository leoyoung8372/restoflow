package com.restoflow.service;

import com.restoflow.dto.TableCardVO;
import com.restoflow.entity.DiningArea;
import com.restoflow.entity.DiningTable;
import com.restoflow.mapper.DiningAreaMapper;
import com.restoflow.mapper.DiningTableMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 桌台业务：把 dining_table 与 dining_area 组装成看板需要的卡片数据。
 *
 * <p>当前只填桌台自身字段与区域名；人数、用餐时长、消费金额需要订单域支持，
 * 待订单模块实现后在此补充，接口结构不变。
 */
@Service  // 交给 Spring 管理；需要它的类通过构造方法注入
public class TableService {

    private final DiningTableMapper diningTableMapper;
    private final DiningAreaMapper diningAreaMapper;

    // 构造方法注入：Spring 启动时把两个 Mapper 传进来
    public TableService(DiningTableMapper diningTableMapper, DiningAreaMapper diningAreaMapper) {
        this.diningTableMapper = diningTableMapper;
        this.diningAreaMapper = diningAreaMapper;
    }

    /** 查询全部桌台卡片，按区域、桌台名排序（看板按区域分组展示）。 */
    public List<TableCardVO> listTableCards() {
        // ① 查所有桌台
        List<DiningTable> tables = diningTableMapper.selectList(null);

        // ② 查所有区域，转成 id → 区域名 的映射。
        //    预先查一次，避免在下面的循环里逐条查数据库。
        //    merge 函数 (a, b) -> a 表示：万一出现重复 id，保留先出现的那个，不抛异常。
        Map<Long, String> areaNameMap = diningAreaMapper.selectList(null).stream()
                .collect(Collectors.toMap(DiningArea::getId, DiningArea::getName, (a, b) -> a));

        // ③ 逐条把实体转换成接口返回对象
        return tables.stream()
                .map(table -> toCard(table, areaNameMap))
                .sorted(Comparator.comparing(TableCardVO::getAreaId)
                        .thenComparing(TableCardVO::getName))
                .toList();
    }

    /** 单个桌台 → 卡片对象。区域名从映射里取，取不到时留空字符串。 */
    private TableCardVO toCard(DiningTable table, Map<Long, String> areaNameMap) {
        TableCardVO vo = new TableCardVO();
        vo.setId(table.getId());
        vo.setName(table.getName());
        vo.setSeats(table.getSeats());
        vo.setStatus(table.getStatus());
        vo.setAreaId(table.getAreaId());
        vo.setAreaName(areaNameMap.getOrDefault(table.getAreaId(), ""));
        // peopleCount / firstOrderAt / amount 属于订单域，暂不填充（保持 null）
        return vo;
    }
}
