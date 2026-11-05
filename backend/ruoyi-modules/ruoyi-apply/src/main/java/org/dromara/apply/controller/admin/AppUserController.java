package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.AppUserBo;
import org.dromara.apply.domain.vo.AppUserVo;
import org.dromara.apply.service.AppAuthService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 小程序用户
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/user")
public class AppUserController {

    private final AppAuthService appAuthService;

    /**
     * 用户列表
     */
    @SaCheckPermission("apply:user:list")
    @GetMapping("/list")
    public R<PageResult<AppUserVo>> list(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         PageQuery pageQuery) {
        return R.ok(appAuthService.adminPage(keyword, status, pageQuery));
    }

    /**
     * 修改状态 / 信用分
     */
    @SaCheckPermission("apply:user:edit")
    @Log(title = "小程序用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody AppUserBo bo) {
        appAuthService.adminUpdate(bo);
        return R.ok();
    }
}
