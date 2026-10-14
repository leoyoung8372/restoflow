package com.restoflow.dto;

import lombok.Data;

/**
 * 开台入参：接收前端提交的用餐人数。
 *
 * <p>对应请求体 JSON：{@code {"peopleCount": 2}}，由 Spring 按字段名自动填充。
 *
 * <p>人数范围（1~99）不在这里校验，放在 TableService 里手写判断——
 * 校验失败时抛 BusinessException.badRequest，由全局异常处理器统一转成 400 响应。
 */
@Data  // 生成 getter/setter，Spring 反序列化 JSON 时靠它填值
public class OpenTableRequest {

    private Integer peopleCount;   // 用餐人数，取值 1~99

    private String remark;   // 桌台备注，选填（如"靠窗"）
}
