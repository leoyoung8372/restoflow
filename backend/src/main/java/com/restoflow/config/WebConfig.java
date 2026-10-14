package com.restoflow.config;

import com.restoflow.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：注册登录校验拦截器。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦下所有接口请求
                .addPathPatterns("/api/**")
                // 登录接口必须放行：登录时还没有 token，拦了谁也登不进来。
                // 登出接口将来要在这里加，因为它需要知道当前用户才能作废 token
                .excludePathPatterns("/api/auth/login");
    }
}
