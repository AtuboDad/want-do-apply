package org.dromara.apply.util;

import cn.hutool.jwt.JWTUtil;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序端登录态工具
 * <p>
 * App 端与管理端使用完全独立的令牌体系：小程序令牌由本类签发（HS256 JWT），
 * 只承载 app_user.id，不参与 Sa-Token 的权限判定，避免两套用户体系互相污染。
 *
 * @author want-do-apply
 */
public class AppLoginHelper {

    private static final String CLAIM_UID = "uid";
    private static final String DEFAULT_SECRET = "want-do-apply-default-secret-please-change";

    private static final ThreadLocal<Long> CURRENT_UID = new ThreadLocal<>();

    private AppLoginHelper() {
    }

    /**
     * 读取签名密钥（配置项 apply.jwt-secret）
     */
    public static String secret() {
        String v = SpringUtils.getBean(Environment.class).getProperty("apply.jwt-secret");
        return StringUtils.isBlank(v) ? DEFAULT_SECRET : v;
    }

    /**
     * 签发令牌
     *
     * @param userId 用户ID
     * @return token
     */
    public static String createToken(Long userId) {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> payload = new HashMap<>(4);
        payload.put(CLAIM_UID, userId);
        payload.put("iat", now);
        payload.put("exp", now + ApplyConstants.APP_TOKEN_TTL);
        return JWTUtil.createToken(payload, secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析并校验令牌
     *
     * @param token 令牌
     * @return 用户ID，无效返回 null
     */
    public static Long parseToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String raw = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        try {
            if (!JWTUtil.verify(raw, secret().getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            Object uid = JWTUtil.parseToken(raw).getPayload(CLAIM_UID);
            if (uid == null) {
                return null;
            }
            Object exp = JWTUtil.parseToken(raw).getPayload("exp");
            if (exp != null && Long.parseLong(exp.toString()) < System.currentTimeMillis() / 1000) {
                return null;
            }
            return Long.parseLong(uid.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static void setUserId(Long userId) {
        CURRENT_UID.set(userId);
    }

    /**
     * 当前登录用户ID（可能为 null）
     */
    public static Long getUserId() {
        return CURRENT_UID.get();
    }

    /**
     * 当前登录用户ID（未登录直接抛错）
     */
    public static Long requireUserId() {
        Long uid = CURRENT_UID.get();
        if (uid == null) {
            throw new ServiceException("请先登录", 401);
        }
        return uid;
    }

    public static void clear() {
        CURRENT_UID.remove();
    }
}
