package com.restoflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.restoflow.common.exception.BusinessException;
import com.restoflow.dto.LoginRequest;
import com.restoflow.dto.LoginResponse;
import com.restoflow.dto.SessionUser;
import com.restoflow.entity.User;
import com.restoflow.mapper.UserMapper;
import com.restoflow.security.TokenStore;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录业务：校验工号密码，成功后签发 token。
 *
 * <p>校验顺序：参数 → 账号密码 → 账号状态 → 签发 token。
 * 出错时抛 {@link BusinessException}，由 GlobalExceptionHandler 统一转成 HTTP 响应。
 */
@Service  // 交给 Spring 管理；需要它的类（如 Controller）通过构造方法注入
public class LoginService {

    private final UserMapper userMapper;
    private final TokenStore tokenStore;

    /** 密码编码器：matches(明文, 哈希) 判断密码是否正确 */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 构造方法注入：Spring 启动时把 UserMapper 和 TokenStore 传进来
    public LoginService(UserMapper userMapper, TokenStore tokenStore) {
        this.userMapper = userMapper;
        this.tokenStore = tokenStore;
    }

    /**
     * 登录。
     *
     * @param request 工号 + 明文密码
     * @return token 与用户信息
     * @throws BusinessException 400 参数为空 / 401 工号或密码错误 / 403 账号已停用
     */
    public LoginResponse login(LoginRequest request) {
        // ① 参数校验
        if (isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            throw BusinessException.badRequest("请输入工号和密码");
        }

        // ② 按工号查用户。LambdaQueryWrapper 只拼接 WHERE username = ?，
        //    参数用占位符传入，不会被 SQL 注入
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));

        // ③ 校验密码。用户不存在与密码错误返回同一个提示，
        //    避免攻击者靠提示差异试出哪些工号真实存在
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("工号或密码错误");
        }

        // ④ 校验账号状态（status: 1启用 0停用）
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("账号已停用，请联系管理员");
        }

        // ⑤ 组装会话用户（丢掉密码），签发 token
        SessionUser sessionUser = new SessionUser();
        sessionUser.setId(user.getId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setRealName(user.getRealName());
        sessionUser.setRole(user.getRole());

        String token = tokenStore.issue(sessionUser);

        // ⑥ 组装返回
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(sessionUser);
        return response;
    }

    /** 判断字符串是否为空白（null、空串、纯空格都算） */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
