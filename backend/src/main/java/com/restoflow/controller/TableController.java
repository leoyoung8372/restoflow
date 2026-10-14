package com.restoflow.controller;

import com.restoflow.common.result.Result;
import com.restoflow.dto.OpenTableRequest;
import com.restoflow.dto.TableCardVO;
import com.restoflow.entity.Order;
import com.restoflow.service.TableService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 桌台接口。
 *
 * <p>只做"接收请求 → 调用 Service → 包装返回"三件事，不写业务逻辑。
 * 出错时不写 try-catch——异常会冒到 GlobalExceptionHandler 统一处理。
 */
@RestController                // 接口类：返回值自动转成 JSON
@RequestMapping("/api/tables")  // 本类所有接口的路径前缀
public class TableController {

    private final TableService tableService;

    // 构造方法注入：Spring 启动时把 TableService 传进来
    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    /**
     * 桌台看板列表。
     *
     * <p>GET /api/tables
     * <p>返回全部桌台，前端按 areaName 分组展示。
     */
    @GetMapping  // 不写路径，处理的就是类级路径本身：GET /api/tables
    public Result<List<TableCardVO>> list() {
        return Result.ok(tableService.listTableCards());
    }

    /**
     * 开台。
     *
     * <p>POST /api/tables/{id}/open
     * <p>请求体：{"peopleCount": 2, "remark": "靠窗"}
     *
     * <p>操作人不由请求体传入，由拦截器从 token 解出，Service 从 CurrentUserHolder 取。
     *
     * @param id      桌台 id，取自路径
     * @param request 用餐人数与桌台备注
     * @return 新建的订单
     */
    @PostMapping("/{id}/open")
    public Result<Order> open(@PathVariable Long id, @RequestBody OpenTableRequest request) {
        return Result.ok(tableService.openTable(id, request));
    }
}
