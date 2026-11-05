package org.dromara.apply.service;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 微信小程序服务端接口
 *
 * @author want-do-apply
 */
@Slf4j
@Service
public class WxMiniService {

    private static final String CODE2SESSION_URL =
        "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    @Value("${apply.wx.app-id:}")
    private String appId;

    @Value("${apply.wx.secret:}")
    private String secret;

    /** 未配置真实 AppID 时启用的本地调试模式 */
    @Value("${apply.wx.mock:true}")
    private boolean mock;

    /**
     * 是否放行前端伪造的 dev:xxx 登录凭证。
     * 仅用于本地联调（H5 调试、接口冒烟测试无法拿到微信真实 code）。
     * 生产环境必须为 false，否则任何人都能伪造任意用户登录。
     */
    @Value("${apply.wx.allow-dev-code:false}")
    private boolean allowDevCode;

    private static final String DEV_CODE_PREFIX = "dev:";

    private final RestClient restClient = RestClient.create();

    /**
     * 用 code 换取 openid
     *
     * @param code wx.login 返回的临时凭证
     * @return 会话信息
     */
    public WxSession code2Session(String code) {
        // 开发用假 code 优先处理：命中时完全不碰微信接口，但必须显式开启开关
        if (code.startsWith(DEV_CODE_PREFIX)) {
            if (!allowDevCode) {
                log.warn("收到 dev: 开头的登录凭证，但未开启 allow-dev-code，已拒绝");
                throw new ServiceException("微信登录失败：非法的登录凭证");
            }
            log.debug("开发模式登录，标识={}", code.substring(DEV_CODE_PREFIX.length()));
            return mockSession(code.substring(DEV_CODE_PREFIX.length()));
        }
        if (mock || StringUtils.isBlank(appId) || StringUtils.isBlank(secret)) {
            // 未配置真实凭据：单用户固定 openid
            return mockSession("default");
        }
        try {
            String body = restClient.get()
                .uri(CODE2SESSION_URL, appId, secret, code)
                .retrieve()
                .body(String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = JsonUtils.parseObject(body, Map.class);
            if (map == null) {
                throw new ServiceException("微信登录失败：响应为空");
            }
            Object errcode = map.get("errcode");
            if (errcode != null && !"0".equals(errcode.toString())) {
                throw new ServiceException("微信登录失败：" + map.get("errmsg"));
            }
            WxSession session = new WxSession();
            session.setOpenId((String) map.get("openid"));
            session.setUnionId((String) map.get("unionid"));
            session.setSessionKey((String) map.get("session_key"));
            session.setMock(false);
            return session;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 code2session 失败", e);
            throw new ServiceException("微信登录异常，请稍后再试");
        }
    }

    public boolean isMock() {
        return mock || StringUtils.isBlank(appId) || StringUtils.isBlank(secret);
    }

    /**
     * 构造一个假的会话（相同 suffix 恒定映射到同一 openid，便于多账号联调）
     */
    private WxSession mockSession(String suffix) {
        WxSession session = new WxSession();
        session.setOpenId("mock_" + DigestUtil.md5Hex(suffix).substring(0, 16));
        session.setUnionId(null);
        session.setSessionKey("mock-session-key");
        session.setMock(true);
        return session;
    }

    /**
     * 微信会话
     */
    @Data
    public static class WxSession {
        private String openId;
        private String unionId;
        private String sessionKey;
        private boolean mock;
    }
}
