package com.restoflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.restoflow.common.exception.BusinessException;
import com.restoflow.dto.OpenTableRequest;
import com.restoflow.dto.TableCardVO;
import com.restoflow.entity.DiningArea;
import com.restoflow.entity.DiningTable;
import com.restoflow.entity.Order;
import com.restoflow.mapper.DiningAreaMapper;
import com.restoflow.mapper.DiningTableMapper;
import com.restoflow.mapper.OrderMapper;
import com.restoflow.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 桌台业务：看板列表的组装，以及开台。
 *
 * <p>看板只填桌台自身字段与区域名；人数、用餐时长、消费金额需要订单域支持，
 * 待订单模块实现后在此补充，接口结构不变。
 */
@Service  // 交给 Spring 管理；需要它的类通过构造方法注入
public class TableService {

    /** 桌台状态：1空台。（其余：2待下单 3待结账 4已结账） */
    private static final int TABLE_STATUS_EMPTY = 1;

    /** 桌台状态：2待下单——开台后进入该状态 */
    private static final int TABLE_STATUS_PENDING_ORDER = 2;

    /** 订单状态：1待下单。（其余：2待结账 3已结账） */
    private static final int ORDER_STATUS_PENDING = 1;

    /** 订单状态：3已结账——看板只关心未结账的订单 */
    private static final int ORDER_STATUS_SETTLED = 3;

    /** 订单号里的日期部分，如 20261014 */
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DiningTableMapper diningTableMapper;
    private final DiningAreaMapper diningAreaMapper;
    private final OrderMapper orderMapper;

    // 构造方法注入：Spring 启动时把三个 Mapper 传进来
    public TableService(DiningTableMapper diningTableMapper,
                        DiningAreaMapper diningAreaMapper,
                        OrderMapper orderMapper) {
        this.diningTableMapper = diningTableMapper;
        this.diningAreaMapper = diningAreaMapper;
        this.orderMapper = orderMapper;
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

        // ③ 查未结账的订单，转成 桌台id → 订单 的映射。
        //    同样预先查一次：40 张桌台若逐条查订单，就是 40 次数据库往返。
        //    桌台同时只可能有一张未结账单，所以按 tableId 存即可。
        Map<Long, Order> orderByTable = orderMapper.selectList(
                        Wrappers.<Order>lambdaQuery().ne(Order::getStatus, ORDER_STATUS_SETTLED))
                .stream()
                .collect(Collectors.toMap(Order::getTableId, o -> o, (a, b) -> a));

        // ④ 逐条把实体转换成接口返回对象
        return tables.stream()
                .map(table -> toCard(table, areaNameMap, orderByTable))
                .sorted(Comparator.comparing(TableCardVO::getAreaId)
                        .thenComparing(TableCardVO::getName))
                .toList();
    }

    /**
     * 开台：为空闲桌台创建订单，并把桌台置为"待下单"。
     *
     * <p>整段在同一事务内：建订单与改桌台状态必须一起成功或一起失败，
     * 否则会出现"订单建了但桌台还是空台"这类不一致（NF-02）。
     *
     * @param tableId 桌台 id
     * @param request 用餐人数与桌台备注
     * @return 新建的订单
     */
    @Transactional
    public Order openTable(Long tableId, OpenTableRequest request) {
        // ① 桌台必须存在
        DiningTable table = diningTableMapper.selectById(tableId);
        if (table == null) {
            throw BusinessException.notFound("桌台不存在");
        }

        // ② 只有空台能开
        if (table.getStatus() == null || table.getStatus() != TABLE_STATUS_EMPTY) {
            throw BusinessException.conflict("仅空台可开台");
        }

        // ③ 人数必须是 1~99 的整数
        Integer peopleCount = request.getPeopleCount();
        if (peopleCount == null || peopleCount < 1 || peopleCount > 99) {
            throw BusinessException.badRequest("用餐人数需为 1~99 的整数");
        }

        // ④ 组装订单。操作人取自登录信息，不接受前端传入——
        //    否则可以伪造"是谁做的"，留痕就失去意义
        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setTableId(table.getId());
        order.setTableName(table.getName());   // 快照：桌台将来被删也能显示
        order.setPeopleCount(peopleCount);
        order.setRemark(request.getRemark());
        order.setStatus(ORDER_STATUS_PENDING);
        order.setOpenedBy(CurrentUserHolder.getUserId());
        orderMapper.insert(order);

        // ⑤ 桌台转为待下单
        DiningTable update = new DiningTable();
        update.setId(table.getId());
        update.setStatus(TABLE_STATUS_PENDING_ORDER);
        diningTableMapper.updateById(update);

        return order;
    }

    /**
     * 生成订单号：当天日期 + 4 位流水，如 202610140001。
     *
     * <p>流水取当天已有订单的最大值 +1。并发下两个请求可能拿到同一个号，
     * 由 order_no 的唯一索引拦下并报错——这是既定策略，不加重试或加锁。
     */
    private String nextOrderNo() {
        String prefix = LocalDate.now().format(ORDER_NO_DATE);

        // 订单号格式固定，长度一致，因此按字符串倒序取第一条即为当天最大
        Order last = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .likeRight(Order::getOrderNo, prefix)
                .orderByDesc(Order::getOrderNo)
                .last("LIMIT 1"));

        if (last == null) {
            return prefix + "0001";   // 当天第一单
        }

        String lastNo = last.getOrderNo();
        if (lastNo == null || lastNo.length() < 4) {
            return prefix + "0001";   // 数据异常时从 1 开始，不让开台失败
        }

        int sequence = Integer.parseInt(lastNo.substring(lastNo.length() - 4));
        return prefix + String.format("%04d", sequence + 1);
    }

    /**
     * 单个桌台 → 卡片对象。
     *
     * <p>区域名从映射里取，取不到时留空字符串；人数、备注、金额从订单取，空台无订单时为 null。
     */
    private TableCardVO toCard(DiningTable table, Map<Long, String> areaNameMap,
                               Map<Long, Order> orderByTable) {
        TableCardVO vo = new TableCardVO();
        vo.setId(table.getId());
        vo.setName(table.getName());
        vo.setSeats(table.getSeats());
        vo.setStatus(table.getStatus());
        vo.setAreaId(table.getAreaId());
        vo.setAreaName(areaNameMap.getOrDefault(table.getAreaId(), ""));

        Order order = orderByTable.get(table.getId());
        if (order != null) {
            vo.setPeopleCount(order.getPeopleCount());
            vo.setRemark(order.getRemark());
            BigDecimal amount = order.getPayableAmount();
            // 金额转字符串给前端：JS 的浮点会丢精度，约定用字符串传输
            vo.setAmount(amount == null ? null : amount.toPlainString());
            // firstOrderAt（用餐时长起点）需要 order_item 表，该模块尚未实现，暂不填充
        }
        return vo;
    }
}
