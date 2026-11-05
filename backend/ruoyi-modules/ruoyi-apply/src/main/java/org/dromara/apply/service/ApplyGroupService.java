package org.dromara.apply.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.domain.AppUser;
import org.dromara.apply.domain.ApplyGroup;
import org.dromara.apply.domain.ApplyGroupMember;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.bo.AdminQueryBo;
import org.dromara.apply.domain.bo.ApplyGroupBo;
import org.dromara.apply.domain.vo.ApplyGroupVo;
import org.dromara.apply.domain.vo.AppUserVo;
import org.dromara.apply.mapper.AppUserMapper;
import org.dromara.apply.mapper.ApplyGroupMapper;
import org.dromara.apply.mapper.ApplyGroupMemberMapper;
import org.dromara.apply.mapper.ApplyOrderMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批群服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class ApplyGroupService {

    private final ApplyGroupMapper groupMapper;
    private final ApplyGroupMemberMapper memberMapper;
    private final AppUserMapper appUserMapper;
    private final ApplyOrderMapper orderMapper;

    /**
     * 创建审批群
     *
     * @param bo 群信息
     * @return 群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApplyGroupBo bo) {
        Long userId = AppLoginHelper.requireUserId();
        Long owned = groupMapper.selectCount(Wrappers.<ApplyGroup>lambdaQuery()
            .eq(ApplyGroup::getOwnerId, userId)
            .eq(ApplyGroup::getStatus, "0"));
        if (owned != null && owned >= 3) {
            throw new ServiceException("最多创建 3 个审批群");
        }

        ApplyGroup group = new ApplyGroup();
        group.setName(bo.getName().trim());
        group.setOwnerId(userId);
        group.setInviteCode(genInviteCode());
        group.setNotice(StringUtils.blankToDefault(bo.getNotice(), ""));
        group.setMemberCount(1);
        group.setStatus("0");
        group.setDelFlag("0");
        group.setCreateBy(userId);
        group.setUpdateBy(userId);
        groupMapper.insert(group);

        ApplyGroupMember member = new ApplyGroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole("1");
        memberMapper.insert(member);
        return group.getId();
    }

    /**
     * 通过邀请码加入
     *
     * @param inviteCode 邀请码
     * @return 群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long join(String inviteCode) {
        Long userId = AppLoginHelper.requireUserId();
        if (StringUtils.isBlank(inviteCode)) {
            throw new ServiceException("邀请码不能为空");
        }
        ApplyGroup group = groupMapper.selectOne(Wrappers.<ApplyGroup>lambdaQuery()
            .eq(ApplyGroup::getInviteCode, inviteCode.trim().toUpperCase()));
        if (group == null || !"0".equals(group.getStatus())) {
            throw new ServiceException("邀请码无效或群已解散");
        }
        return joinGroup(group, userId);
    }

    /**
     * 按ID加入（群广场页）
     *
     * @param groupId 群ID
     * @return 群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long joinById(Long groupId) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyGroup group = groupMapper.selectById(groupId);
        if (group == null || !"0".equals(group.getStatus())) {
            throw new ServiceException("审批群不存在或已解散");
        }
        return joinGroup(group, userId);
    }

    private Long joinGroup(ApplyGroup group, Long userId) {
        Long exist = memberMapper.selectCount(Wrappers.<ApplyGroupMember>lambdaQuery()
            .eq(ApplyGroupMember::getGroupId, group.getId())
            .eq(ApplyGroupMember::getUserId, userId));
        if (exist != null && exist > 0) {
            return group.getId();
        }
        ApplyGroupMember member = new ApplyGroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole("0");
        memberMapper.insert(member);
        groupMapper.update(null, Wrappers.<ApplyGroup>lambdaUpdate()
            .setSql("member_count = member_count + 1")
            .eq(ApplyGroup::getId, group.getId()));
        return group.getId();
    }

    /**
     * 我加入的群
     *
     * @return 群列表
     */
    public List<ApplyGroupVo> myGroups() {
        Long userId = AppLoginHelper.requireUserId();
        List<ApplyGroupMember> members = memberMapper.selectList(Wrappers.<ApplyGroupMember>lambdaQuery()
            .eq(ApplyGroupMember::getUserId, userId));
        if (members.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = members.stream().map(ApplyGroupMember::getGroupId).collect(Collectors.toList());
        List<ApplyGroup> groups = groupMapper.selectList(Wrappers.<ApplyGroup>lambdaQuery()
            .in(ApplyGroup::getId, ids)
            .eq(ApplyGroup::getStatus, "0")
            .orderByDesc(ApplyGroup::getCreateTime));
        List<ApplyGroupVo> vos = groups.stream()
            .map(g -> MapstructUtils.convert(g, ApplyGroupVo.class))
            .collect(Collectors.toList());
        fillOwnerAndFlag(vos, userId);
        return vos;
    }

    /**
     * 群详情
     *
     * @param id 群ID
     * @return 群信息
     */
    public ApplyGroupVo detail(Long id) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new ServiceException("审批群不存在");
        }
        ApplyGroupVo vo = MapstructUtils.convert(group, ApplyGroupVo.class);
        fillOwnerAndFlag(List.of(vo), userId);
        return vo;
    }

    /**
     * 群成员
     *
     * @param groupId 群ID
     * @return 成员用户列表
     */
    public List<AppUserVo> members(Long groupId) {
        AppLoginHelper.requireUserId();
        List<ApplyGroupMember> members = memberMapper.selectList(Wrappers.<ApplyGroupMember>lambdaQuery()
            .eq(ApplyGroupMember::getGroupId, groupId));
        if (members.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> uids = members.stream().map(ApplyGroupMember::getUserId).collect(Collectors.toList());
        return appUserMapper.selectVoByIds(uids);
    }

    /**
     * 退出群
     *
     * @param groupId 群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void quit(Long groupId) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new ServiceException("审批群不存在");
        }
        if (userId.equals(group.getOwnerId())) {
            throw new ServiceException("群主不能退出，请先解散群");
        }
        memberMapper.delete(Wrappers.<ApplyGroupMember>lambdaQuery()
            .eq(ApplyGroupMember::getGroupId, groupId)
            .eq(ApplyGroupMember::getUserId, userId));
        groupMapper.update(null, Wrappers.<ApplyGroup>lambdaUpdate()
            .setSql("member_count = greatest(member_count - 1, 1)")
            .eq(ApplyGroup::getId, groupId));
    }

    /**
     * 解散群（群主）
     *
     * @param groupId 群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void dissolve(Long groupId) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyGroup group = groupMapper.selectById(groupId);
        if (group == null || !userId.equals(group.getOwnerId())) {
            throw new ServiceException("只有群主可以解散");
        }
        groupMapper.update(null, Wrappers.<ApplyGroup>lambdaUpdate()
            .set(ApplyGroup::getStatus, "1")
            .eq(ApplyGroup::getId, groupId));
    }

    /**
     * 群内的审批单数量
     *
     * @param groupId 群ID
     * @return 数量
     */
    public Long orderCount(Long groupId) {
        return orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getGroupId, groupId)
            .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_PENDING));
    }

    // ==================== 管理端 ====================

    /**
     * 管理端分页
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyGroupVo> adminPage(AdminQueryBo bo, PageQuery pageQuery) {
        var wrapper = Wrappers.<ApplyGroup>lambdaQuery();
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            wrapper.like(ApplyGroup::getName, bo.getKeyword());
        }
        if (StringUtils.isNotBlank(bo.getStatus())) {
            wrapper.eq(ApplyGroup::getStatus, bo.getStatus());
        }
        if (bo.getUserId() != null) {
            wrapper.eq(ApplyGroup::getOwnerId, bo.getUserId());
        }
        wrapper.orderByDesc(ApplyGroup::getCreateTime);
        IPage<ApplyGroup> page = groupMapper.selectPage(pageQuery.build(), wrapper);
        List<ApplyGroupVo> vos = page.getRecords().stream()
            .map(g -> MapstructUtils.convert(g, ApplyGroupVo.class))
            .collect(Collectors.toList());
        fillOwnerAndFlag(vos, null);
        return PageResult.build(vos, page.getTotal());
    }

    /**
     * 管理端删除（解散）
     *
     * @param ids 主键集合
     */
    public void adminRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        groupMapper.update(null, Wrappers.<ApplyGroup>lambdaUpdate()
            .set(ApplyGroup::getStatus, "1")
            .in(ApplyGroup::getId, ids));
    }

    // ==================== 内部 ====================

    private void fillOwnerAndFlag(List<ApplyGroupVo> vos, Long userId) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ownerIds = vos.stream().map(ApplyGroupVo::getOwnerId).distinct().collect(Collectors.toList());
        List<AppUser> owners = appUserMapper.selectByIds(ownerIds);
        var ownerMap = owners.stream().collect(Collectors.toMap(AppUser::getId, AppUser::getNickName, (a, b) -> a));

        List<Long> joinedIds = new ArrayList<>();
        if (userId != null) {
            List<Long> groupIds = vos.stream().map(ApplyGroupVo::getId).collect(Collectors.toList());
            joinedIds = memberMapper.selectList(Wrappers.<ApplyGroupMember>lambdaQuery()
                    .eq(ApplyGroupMember::getUserId, userId)
                    .in(ApplyGroupMember::getGroupId, groupIds))
                .stream().map(ApplyGroupMember::getGroupId).collect(Collectors.toList());
        }
        for (ApplyGroupVo vo : vos) {
            vo.setOwnerNickName(ownerMap.getOrDefault(vo.getOwnerId(), "未知"));
            vo.setJoined(joinedIds.contains(vo.getId()));
            vo.setIsOwner(userId != null && userId.equals(vo.getOwnerId()));
        }
    }

    private String genInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", 6);
            Long exist = groupMapper.selectCount(Wrappers.<ApplyGroup>lambdaQuery()
                .eq(ApplyGroup::getInviteCode, code));
            if (exist == null || exist == 0) {
                return code;
            }
        }
        throw new ServiceException("邀请码生成失败，请重试");
    }
}
