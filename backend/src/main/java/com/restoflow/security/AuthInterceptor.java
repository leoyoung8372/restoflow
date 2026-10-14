package com.restoflow.security;

import com.restoflow.common.exception.BusinessException;
import com.restoflow.dto.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器：每个请求进来先确认"是谁在操作"。
 *
 * <p>流程：从请求头取 token → 查 TokenStore → 查到就放行并记录用户，查不到返回 401。
 *
 * <p>做成拦截器而非过滤器，是为了让 401 能被 GlobalExceptionHandler 统一处理——
 * 过滤器在 DispatcherServlet 外层，抛出的异常到不了那里。
 *
 * <p>被 WebConfig 排除的路径（如登录接口）不经过这里，此时 CurrentUserHolder 为空。
 */
@Component  // 交给 Spring 管理，由 WebConfig 注入并注册
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求头里 token 的前缀，与前端 request.ts 里发送的格式一致 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * 请求进入 Controller 之前执行。
     *
     * @return true 放行；抛异常表示拒绝（由全局异常处理器转成 401 响应）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 先清理一次：防止上一次请求异常导致残留（线程会被复用）
        CurrentUserHolder.clear();

        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }

        // 去掉 "Bearer " 前缀，只留 token 本身
        String token = header.substring(BEARER_PREFIX.length());
        SessionUser user = tokenStore.get(token);

        if (user == null) {
            // token 不存在或已过期
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }

        CurrentUserHolder.set(user);
        return true;
    }

    /** 请求结束（无论成功还是出错）都会执行，必须清理 ThreadLocal */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
