package com.restoflow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常：把"出错原因"和"HTTP 状态码"绑在一起抛出。
 *
 * <p>用法：在 Service 里直接 {@code throw BusinessException.conflict("仅空台可开台")}，
 * 由 GlobalExceptionHandler 统一捕获，转成对应状态码 + 统一返回结构。
 *
 * <p>好处：业务代码里不出现 HTTP 状态码，只表达"出了什么错"。
 */
@Getter  // 生成 getStatus()：异常处理器需要读取状态码
public class BusinessException extends RuntimeException {

    /** HTTP 状态码，决定接口返回 400 还是 401/403/404/409 */
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);      // 交给父类 RuntimeException 保存错误提示
        this.status = status;
    }

    /** 400 参数错误：入参不合法（如用餐人数超范围、退菜原因没填） */
    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    /** 401 未认证：工号或密码错误、token 失效 */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 403 无权限：模块权限不足、账号已停用 */
    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    /** 404 资源不存在：桌台/订单/明细/菜品的 id 查不到 */
    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    /** 409 业务状态冲突：状态机不允许的操作（如已结账订单还要退菜） */
    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}
