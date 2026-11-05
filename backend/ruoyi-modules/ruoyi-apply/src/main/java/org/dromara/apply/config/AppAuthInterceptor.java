package org.dromara.apply.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 小程序端登录拦截器
 * <p>
 * 只做「有没有登录」的判断，具体权限由业务层按数据归属校验。
 *
 * @author want-do-apply
 */
public class AppAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(ApplyConstants.APP_TOKEN_HEADER);
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("Authorization");
        }
        if (StringUtils.isBlank(token)) {
            token = ServletUtils.getParameter("App-Token");
        }
        Long userId = AppLoginHelper.parseToken(token);
        if (userId == null) {
            throw new org.dromara.common.core.exception.ServiceException("登录已失效，请重新登录", 401);
        }
        AppLoginHelper.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AppLoginHelper.clear();
    }
}
