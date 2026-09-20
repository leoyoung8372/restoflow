package com.restoflow.dto;

import lombok.Data;

/**
 * 登录出参：登录成功后返回给前端的内容。
 *
 * <p>前端拿到后：token 存进 localStorage，后续每个请求放在请求头
 * {@code Authorization: Bearer <token>}；user 用于页面显示（如右上角显示姓名）。
 */
@Data  // 生成 getter/setter，Spring 序列化成 JSON 时靠 getter 读值
public class LoginResponse {

    /** 登录凭证，有效期 6 小时 */
    private String token;

    /** 当前登录用户，不含密码 */
    private SessionUser user;
}
