package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.vo.DashboardVo;
import org.dromara.apply.service.DashboardService;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 数据看板
 *
 * @author want-do-apply
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 看板总览
     */
    @SaCheckPermission("apply:dashboard:view")
    @GetMapping("/overview")
    public R<DashboardVo> overview() {
        return R.ok(dashboardService.overview());
    }
}
