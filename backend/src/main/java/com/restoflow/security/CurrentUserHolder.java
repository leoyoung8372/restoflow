package com.restoflow.security;

import com.restoflow.dto.SessionUser;

/**
 * 当前请求的登录用户中转站。
 *
 * <p>拦截器验证 token 后把用户存进来，Service 层随时取出来用（如开台时记录操作人），
 * 这样业务代码不必直接接触 HTTP 请求对象。
 *
 * <p>用 ThreadLocal 存：一个请求由一个线程处理，各线程的数据互不干扰，
 * 张三的请求只会读到张三。但请求结束后【必须清理】——
 * 线程会被复用，不清理会让下一个请求读到上一个请求的用户。
 * 清理动作由 AuthInterceptor 的 afterCompletion 负责。
 */
public class CurrentUserHolder {

    private static final ThreadLocal<SessionUser> CURRENT = new ThreadLocal<>();

    /** 存入当前请求的登录用户，由拦截器在验证通过后调用 */
    public static void set(SessionUser user) {
        CURRENT.set(user);
    }

    /**
     * 取当前请求的登录用户。
     *
     * @return 登录用户；未登录或被拦截器放行的接口（如登录接口）返回 null
     */
    public static SessionUser get() {
        return CURRENT.get();
    }

    /** 取当前用户 id。Service 记录操作人时用它，未登录时为 null */
    public static Long getUserId() {
        SessionUser user = CURRENT.get();
        return user == null ? null : user.getId();
    }

    /** 清理，由拦截器在请求结束时调用，避免线程复用时数据串号 */
    public static void clear() {
        CURRENT.remove();
    }
}
