package org.dromara.apply.controller.app;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.AppUser;
import org.dromara.apply.domain.bo.AppLoginBo;
import org.dromara.apply.domain.vo.AppLoginVo;
import org.dromara.apply.domain.vo.AppProfileVo;
import org.dromara.apply.domain.vo.AppUserVo;
import org.dromara.apply.service.AppAuthService;
import org.dromara.apply.service.ApplyOrderService;
import org.dromara.apply.service.WxMiniService;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 小程序端 - 登录与账号
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/auth")
public class AppAuthController {

    private final AppAuthService appAuthService;
    private final ApplyOrderService orderService;
    private final WxMiniService wxMiniService;

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public R<AppLoginVo> login(@Validated @RequestBody AppLoginBo bo) {
        return R.ok(appAuthService.login(bo));
    }

    /**
     * 小程序端运行配置
     */
    @GetMapping("/config")
    public R<Map<String, Object>> config() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("mockLogin", wxMiniService.isMock());
        map.put("appName", "钱包请等等");
        map.put("dailyApplyLimit", org.dromara.apply.constant.ApplyConstants.DAILY_APPLY_LIMIT);
        return R.ok(map);
    }

    /**
     * 当前登录用户
     */
    @GetMapping("/profile")
    public R<AppProfileVo> profile() {
        return R.ok(orderService.profile());
    }

    /**
     * 我的资料
     */
    @GetMapping("/me")
    public R<AppUserVo> me() {
        AppUser user = appAuthService.currentUser();
        return R.ok(MapstructUtils.convert(user, AppUserVo.class));
    }

    /**
     * 更新昵称头像
     */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestParam(required = false) String nickName,
                                 @RequestParam(required = false) String avatar,
                                 @RequestParam(required = false) String showVip) {
        appAuthService.updateProfile(nickName, avatar, showVip);
        return R.ok();
    }

    /**
     * 校验登录态
     */
    @GetMapping("/check")
    public R<Long> check() {
        return R.ok(AppLoginHelper.requireUserId());
    }
}
