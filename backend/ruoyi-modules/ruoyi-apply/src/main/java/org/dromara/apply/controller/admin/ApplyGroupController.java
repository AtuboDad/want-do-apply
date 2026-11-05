package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.vo.ApplyGroupVo;
import org.dromara.apply.service.ApplyGroupService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 管理端 - 审批群
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/group")
public class ApplyGroupController {

    private final ApplyGroupService groupService;

    /**
     * 群列表
     */
    @SaCheckPermission("apply:group:list")
    @GetMapping("/list")
    public R<PageResult<ApplyGroupVo>> list(AdminQueryBo bo, PageQuery pageQuery) {
        return R.ok(groupService.adminPage(bo, pageQuery));
    }

    /**
     * 解散群
     */
    @SaCheckPermission("apply:group:remove")
    @Log(title = "审批群", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        groupService.adminRemove(Arrays.asList(ids));
        return R.ok();
    }
}
