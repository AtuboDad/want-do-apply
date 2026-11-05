package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.ApplyCommentBo;
import org.dromara.apply.domain.vo.ApplyCommentVo;
import org.dromara.apply.service.ApplyCommentService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端 - 评论
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/comment")
public class AppCommentController {

    private final ApplyCommentService commentService;

    /**
     * 评论树
     */
    @GetMapping("/list")
    public R<List<ApplyCommentVo>> list(@RequestParam Long orderId) {
        return R.ok(commentService.list(orderId));
    }

    /**
     * 发表评论
     */
    @PostMapping
    public R<Long> add(@Validated @RequestBody ApplyCommentBo bo) {
        return R.ok(commentService.add(bo));
    }

    /**
     * 删除自己的评论
     */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        commentService.removeMine(id);
        return R.ok();
    }
}
