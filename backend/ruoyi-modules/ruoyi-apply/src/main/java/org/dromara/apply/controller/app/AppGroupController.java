package org.dromara.apply.controller.app;

import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.bo.ApplyGroupBo;
import org.dromara.apply.domain.vo.ApplyGroupVo;
import org.dromara.apply.domain.vo.AppUserVo;
import org.dromara.apply.domain.vo.ApplyOrderVo;
import org.dromara.apply.service.ApplyGroupService;
import org.dromara.apply.service.ApplyOrderService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端 - 审批群
 *
 * @author want-do-apply
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/group")
public class AppGroupController {

    private final ApplyGroupService groupService;
    private final ApplyOrderService orderService;

    /**
     * 我加入的群
     */
    @GetMapping("/mine")
    public R<List<ApplyGroupVo>> mine() {
        return R.ok(groupService.myGroups());
    }

    /**
     * 创建群
     */
    @PostMapping
    public R<Long> create(@Validated @RequestBody ApplyGroupBo bo) {
        return R.ok(groupService.create(bo));
    }

    /**
     * 邀请码加入
     */
    @PostMapping("/join")
    public R<Long> join(@RequestParam String inviteCode) {
        return R.ok(groupService.join(inviteCode));
    }

    /**
     * 按ID加入
     */
    @PostMapping("/{id}/join")
    public R<Long> joinById(@PathVariable Long id) {
        return R.ok(groupService.joinById(id));
    }

    /**
     * 群详情
     */
    @GetMapping("/{id}")
    public R<ApplyGroupVo> detail(@PathVariable Long id) {
        return R.ok(groupService.detail(id));
    }

    /**
     * 群成员
     */
    @GetMapping("/{id}/members")
    public R<List<AppUserVo>> members(@PathVariable Long id) {
        return R.ok(groupService.members(id));
    }

    /**
     * 群内审批单
     */
    @GetMapping("/{id}/orders")
    public R<PageResult<ApplyOrderVo>> orders(@PathVariable Long id,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) String status,
                                              PageQuery pageQuery) {
        return R.ok(orderService.square(id, sort, status, pageQuery));
    }

    /**
     * 退群
     */
    @PostMapping("/{id}/quit")
    public R<Void> quit(@PathVariable Long id) {
        groupService.quit(id);
        return R.ok();
    }

    /**
     * 解散群
     */
    @PostMapping("/{id}/dissolve")
    public R<Void> dissolve(@PathVariable Long id) {
        groupService.dissolve(id);
        return R.ok();
    }
}
