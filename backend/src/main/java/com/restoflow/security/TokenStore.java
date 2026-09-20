package com.restoflow.security;

import com.restoflow.dto.SessionUser;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录 token 存储（内存）。
 *
 * <p>登录成功时把 token 和"当前用户"存进来；后续请求带着 token 来，靠它查出是谁在操作。
 *
 * <p>三个要点：
 * <ul>
 *   <li>有效期固定 6 小时，不滑动续期</li>
 *   <li>登出时删除 token，立即失效</li>
 *   <li>服务重启后内存清空，所有人需重新登录（开发阶段可接受）</li>
 * </ul>
 */
@Component  // 交给 Spring 管理，需要它的类可以通过构造方法注入
public class TokenStore {

    /** token 有效期：6 小时 */
    private static final Duration TTL = Duration.ofHours(6);

    /** token → 会话信息。ConcurrentHashMap 保证多请求同时读写时不冲突 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** 安全随机数：用它生成的 token 猜不出来（不能用 Random） */
    private final SecureRandom random = new SecureRandom();

    /**
     * 签发 token：生成随机串并保存用户信息。
     *
     * @return 给前端的 token 字符串
     */
    public String issue(SessionUser user) {
        byte[] bytes = new byte[32];   // 32 字节 = 256 位，足够随机
        random.nextBytes(bytes);
        // URL 安全的 Base64 编码，去掉末尾的 = 让字符串更短，如 "po9ovDGCeM0ggr4N1UoodS..."
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessions.put(token, new Session(user, Instant.now().plus(TTL)));
        return token;
    }

    /**
     * 按 token 取用户信息。
     *
     * @return 用户信息；token 不存在或已过期返回 null
     */
    public SessionUser get(String token) {
        if (token == null) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (Instant.now().isAfter(session.expiresAt)) {
            sessions.remove(token);   // 过期了顺手清掉
            return null;
        }
        return session.user;
    }

    /** 登出：删除该 token，立即失效 */
    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** 删除某用户的全部 token（停用账号时调用，让他在所有设备上立即失效） */
    public void revokeByUser(Long userId) {
        sessions.values().removeIf(session -> session.user.getId().equals(userId));
    }

    /** 一条会话：谁 + 什么时候过期 */
    private static class Session {
        final SessionUser user;
        final Instant expiresAt;

        Session(SessionUser user, Instant expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }
    }
}
