package com.restoflow.dto;

import lombok.Data;

/**
 * 登录会话中的用户信息：登录成功后"记住"的当前用户是谁。
 *
 * <p>与 entity.User 的区别：这里<b>不含 password</b>——
 * 密码只在登录校验的那一瞬间用到，之后不该带到任何地方。
 *
 * <p>用途：存进 token（TokenStore），后续每个请求靠它识别"现在是谁在操作"。
 */
@Data  // 生成 getter/setter，以及 toString/equals/hashCode（无密码字段，打印安全）
public class SessionUser {

    /** 用户主键，用于关联订单、支付记录里的"操作人" */
    private Long id;

    /** 工号，如 admin */
    private String username;

    /** 姓名，账单上显示的收银员 */
    private String realName;

    /** 角色：admin / manager / supervisor / chef，用于权限判断 */
    private String role;
}
