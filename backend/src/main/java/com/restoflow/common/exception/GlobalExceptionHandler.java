package com.restoflow.common.exception;

import com.restoflow.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：把 Controller/Service 抛出的异常，统一转成 HTTP 响应。
 *
 * <p>有了它，业务代码里不需要写 try-catch——
 * 抛出的 BusinessException 会自动被这里接住，并按它自带的状态码返回。
 *
 * <p>返回体统一用 {@link Result} 包装，格式为 { code, message, data }。
 */
@Slf4j      // 生成 log 对象，用于记录日志（Lombok 提供）
@RestControllerAdvice  // 告诉 Spring：这是全局异常处理器，所有 Controller 的异常都经过它
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：按异常自带的状态码返回。
     * <p>例：BusinessException.unauthorized("工号或密码错误") → HTTP 401
     */
    @ExceptionHandler(BusinessException.class)  // 指定本方法处理 BusinessException
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        // 业务异常是"预期内的错误"，用 warn 级别，不打印堆栈
        log.warn("业务异常：{}", e.getMessage());
        // ResponseEntity 让我们能同时控制 HTTP 状态码和响应体
        return ResponseEntity.status(e.getStatus()).body(Result.error(e.getMessage()));
    }

    /**
     * 兜底处理其他异常：返回 500。
     * <p>对外只给一句通用提示，不暴露堆栈、SQL、类名等内部细节；
     * 真正的原因打印到日志里，供开发者排查。
     */
    @ExceptionHandler(Exception.class)  // 所有未被上面方法接住的异常都到这里
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        // 未预期的异常要打印完整堆栈，方便定位问题
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("系统异常，请稍后重试"));
    }
}
