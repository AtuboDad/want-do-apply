package org.dromara.apply.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.vo.ApplyCommentVo;
import org.dromara.apply.service.ApplyCommentService;
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
 * 管理端 - 评论
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/apply/comment")
public class ApplyCommentController {

    private final ApplyCommentService commentService;

    /**
     * 评论列表
     */
    @SaCheckPermission("apply:comment:list")
    @GetMapping("/list")
    public R<PageResult<ApplyCommentVo>> list(AdminQueryBo bo, PageQuery pageQuery) {
        return R.ok(commentService.adminPage(bo, pageQuery));
    }

    /**
     * 删除评论
     */
    @SaCheckPermission("apply:comment:remove")
    @Log(title = "评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        commentService.adminRemove(Arrays.asList(ids));
        return R.ok();
    }
}
