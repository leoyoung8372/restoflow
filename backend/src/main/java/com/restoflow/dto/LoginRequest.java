package com.restoflow.dto;

import lombok.Data;

/**
 * 登录入参：接收前端提交的工号与密码。
 *
 * <p>对应请求体 JSON：{@code {"username":"admin","password":"123456"}}，
 * 由 Spring 按字段名自动填充。
 */
@Data  // 生成 getter/setter，Spring 反序列化 JSON 时靠它填值
public class LoginRequest {

    /** 工号，如 admin */
    private String username;

    /** 明文密码，仅用于本次校验，不会存库 */
    private String password;
}
