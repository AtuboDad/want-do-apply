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

    private final RestClient restClient = RestClient.create();

    /**
     * 用 code 换取 openid
     *
     * @param code wx.login 返回的临时凭证
     * @return 会话信息
     */
    public WxSession code2Session(String code) {
        if (mock || StringUtils.isBlank(appId) || StringUtils.isBlank(secret)) {
            // 调试模式：单用户固定 openid；需要多账号时前端传 dev:xxx
            String suffix = code.startsWith("dev:") ? code.substring(4) : "default";
            WxSession session = new WxSession();
            session.setOpenId("mock_" + DigestUtil.md5Hex(suffix).substring(0, 16));
            session.setUnionId(null);
            session.setSessionKey("mock-session-key");
            session.setMock(true);
            return session;
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
