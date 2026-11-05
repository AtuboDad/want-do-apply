package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.vo.ApplyVoteVo;
import org.dromara.apply.service.ApplyVoteService;
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
 * 管理端 - 批条
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/vote")
public class ApplyVoteController {

    private final ApplyVoteService voteService;

    /**
     * 批条列表
     */
    @SaCheckPermission("apply:vote:list")
    @GetMapping("/list")
    public R<PageResult<ApplyVoteVo>> list(AdminQueryBo bo, PageQuery pageQuery) {
        return R.ok(voteService.adminPage(bo, pageQuery));
    }

    /**
     * 删除批条
     */
    @SaCheckPermission("apply:vote:remove")
    @Log(title = "批条", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        voteService.adminRemove(Arrays.asList(ids));
        return R.ok();
    }
}
