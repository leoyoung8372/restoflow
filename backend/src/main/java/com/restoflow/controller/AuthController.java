package com.restoflow.controller;

import com.restoflow.common.result.Result;
import com.restoflow.dto.LoginRequest;
import com.restoflow.dto.LoginResponse;
import com.restoflow.service.LoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录。
 *
 * <p>只做"接收请求 → 调用 Service → 包装返回"三件事，不写业务逻辑。
 * 出错时不写 try-catch——异常会冒到 GlobalExceptionHandler 统一处理。
 */
@RestController            // 接口类：返回值自动转成 JSON
@RequestMapping("/api/auth")  // 本类所有接口的路径前缀
public class AuthController {

    private final LoginService loginService;

    // 构造方法注入：Spring 启动时把 LoginService 传进来
    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * 登录。
     *
     * <p>POST /api/auth/login
     * <p>请求体：{"username":"admin","password":"123456"}
     * <p>成功返回：{code:0, message:"ok", data:{token:"...", user:{...}}}
     *
     * @param request 请求体 JSON，由 @RequestBody 自动转换
     * @return 统一格式的返回结果
     */
    @PostMapping("/login")  // 完整路径 = /api/auth + /login
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(loginService.login(request));
    }
}
