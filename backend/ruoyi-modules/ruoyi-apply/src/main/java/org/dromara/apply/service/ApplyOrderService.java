package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.constant.ApplyConstants;
import org.dromara.apply.domain.ApplyFavorite;
import org.dromara.apply.domain.ApplyGroup;
import org.dromara.apply.domain.ApplyGroupMember;
import org.dromara.apply.domain.ApplyOrder;
import org.dromara.apply.domain.AppUser;
import org.dromara.apply.domain.bo.AdminOrderQueryBo;
import org.dromara.apply.domain.bo.ApplyOrderBo;
import org.dromara.apply.domain.vo.AppOrderDetailVo;
import org.dromara.apply.domain.vo.AppProfileVo;
import org.dromara.apply.domain.vo.ApplyCommentVo;
import org.dromara.apply.domain.vo.ApplyOrderVo;
import org.dromara.apply.domain.vo.ApplyVoteVo;
import org.dromara.apply.mapper.AppUserMapper;
import org.dromara.apply.mapper.ApplyCommentMapper;
import org.dromara.apply.mapper.ApplyFavoriteMapper;
import org.dromara.apply.mapper.ApplyGroupMapper;
import org.dromara.apply.mapper.ApplyGroupMemberMapper;
import org.dromara.apply.mapper.ApplyOrderMapper;
import org.dromara.apply.mapper.ApplyVoteMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批单服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class ApplyOrderService {

    private final ApplyOrderMapper orderMapper;
    private final ApplyVoteMapper voteMapper;
    private final ApplyCommentMapper commentMapper;
    private final ApplyFavoriteMapper favoriteMapper;
    private final ApplyGroupMapper groupMapper;
    private final ApplyGroupMemberMapper groupMemberMapper;
    private final AppUserMapper appUserMapper;

    // ==================== 小程序端 ====================

    /**
     * 发起审批
     *
     * @param bo 表单
     * @return 审批单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApplyOrderBo bo) {
        Long userId = AppLoginHelper.requireUserId();

        // 每日发单额度
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Long todayCount = orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getUserId, userId)
            .ge(ApplyOrder::getCreateTime, dayStart));
        if (todayCount >= ApplyConstants.DAILY_APPLY_LIMIT) {
            throw new ServiceException("今天已经发了 " + ApplyConstants.DAILY_APPLY_LIMIT + " 单啦，明天再来吧");
        }

        String visibility = StringUtils.blankToDefault(bo.getVisibility(), ApplyConstants.VISIBILITY_PUBLIC);
        Long groupId = bo.getGroupId();
        if (ApplyConstants.VISIBILITY_GROUP.equals(visibility)) {
            if (groupId == null) {
                throw new ServiceException("请选择要发到哪个审批群");
            }
            Long joined = groupMemberMapper.selectCount(Wrappers.<ApplyGroupMember>lambdaQuery()
                .eq(ApplyGroupMember::getGroupId, groupId)
                .eq(ApplyGroupMember::getUserId, userId));
            if (joined == null || joined == 0) {
                throw new ServiceException("你还没有加入这个审批群");
            }
        } else {
            groupId = null;
        }

        ApplyOrder order = MapstructUtils.convert(bo, ApplyOrder.class);

        // 审批时长由发起人自由设置，到点后按「少数服从多数」结算，不再按票数提前结案
        int hours = bo.getDurationHours() == null
            ? ApplyConstants.DEFAULT_DURATION_HOURS : bo.getDurationHours();
        if (hours < ApplyConstants.MIN_DURATION_HOURS || hours > ApplyConstants.MAX_DURATION_HOURS) {
            throw new ServiceException("审批时长要在 " + ApplyConstants.MIN_DURATION_HOURS
                + "-" + ApplyConstants.MAX_DURATION_HOURS + " 小时之间");
        }

        order.setUserId(userId);
        order.setGroupId(groupId);
        order.setVisibility(visibility);
        order.setCategory(StringUtils.blankToDefault(bo.getCategory(), "other"));
        order.setImages(StringUtils.blankToDefault(bo.getImages(), ""));
        order.setSubstitute(StringUtils.blankToDefault(bo.getSubstitute(), ""));
        order.setExtraNote("");
        order.setDurationHours(hours);
        order.setStatus(ApplyConstants.ORDER_PENDING);
        order.setPassCount(0);
        order.setRejectCount(0);
        order.setVoteCount(0);
        order.setTargetVotes(0);
        order.setCommentCount(0);
        order.setHotScore(0);
        order.setExpireTime(LocalDateTime.now().plusHours(hours));
        order.setAuditStatus(ApplyConstants.AUDIT_PASS);
        order.setAuditRemark("");
        order.setIsTop("0");
        order.setDelFlag("0");
        order.setCreateBy(userId);
        order.setUpdateBy(userId);
        orderMapper.insert(order);

        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
            .setSql("apply_count = apply_count + 1")
            .eq(AppUser::getId, userId));

        return order.getId();
    }

    /**
     * 广场 / 群内列表
     *
     * @param groupId   群ID，为空看广场
     * @param sort      排序 new/hot/pending
     * @param status    状态筛选
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyOrderVo> square(Long groupId, String sort, String status, PageQuery pageQuery) {
        Long userId = AppLoginHelper.getUserId();
        if (groupId != null) {
            assertGroupMember(groupId, userId);
        }
        IPage<ApplyOrderVo> page = orderMapper.selectSquarePage(
            pageQuery.build(), groupId, userId == null ? -1L : userId,
            StringUtils.blankToDefault(sort, "new"), status);
        List<ApplyOrderVo> records = page.getRecords();
        decorate(records, userId);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 审批单详情
     *
     * @param id 审批单ID
     * @return 详情（含批条、评论）
     */
    public AppOrderDetailVo detail(Long id) {
        Long userId = AppLoginHelper.getUserId();
        ApplyOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("审批单不存在或已删除");
        }
        if (ApplyConstants.VISIBILITY_GROUP.equals(order.getVisibility())) {
            assertGroupMember(order.getGroupId(), userId);
        }
        if (ApplyConstants.AUDIT_OFF.equals(order.getAuditStatus())
            && (userId == null || !userId.equals(order.getUserId()))) {
            throw new ServiceException("该内容已被下架");
        }

        ApplyOrderVo vo = MapstructUtils.convert(order, ApplyOrderVo.class);
        vo.setImageList(splitImages(order.getImages()));
        vo.setRemainSeconds(remainSeconds(order));
        AppUser author = appUserMapper.selectById(order.getUserId());
        if (author != null) {
            vo.setUserNickName(author.getNickName());
            vo.setUserAvatar(author.getAvatar());
        }
        if (order.getGroupId() != null) {
            ApplyGroup group = groupMapper.selectById(order.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getName());
            }
        }
        Boolean mine = userId != null && userId.equals(order.getUserId());
        vo.setMine(mine);

        if (userId != null) {
            ApplyVoteVo mineVote = voteMapper.selectVoOne(Wrappers.<org.dromara.apply.domain.ApplyVote>lambdaQuery()
                .eq(org.dromara.apply.domain.ApplyVote::getOrderId, id)
                .eq(org.dromara.apply.domain.ApplyVote::getUserId, userId)
                .last("limit 1"), false);
            vo.setMyVoted(mineVote != null);
            vo.setMyResult(mineVote == null ? null : mineVote.getResult());
            vo.setFavorited(favoriteMapper.selectCount(Wrappers.<ApplyFavorite>lambdaQuery()
                .eq(ApplyFavorite::getUserId, userId)
                .eq(ApplyFavorite::getOrderId, id)) > 0);
        }

        List<ApplyVoteVo> votes = voteMapper.selectByOrderId(id, userId == null ? -1L : userId, "hot");
        List<ApplyCommentVo> flatComments = commentMapper.selectByOrderId(id);

        AppOrderDetailVo detail = new AppOrderDetailVo();
        detail.setOrder(vo);
        detail.setVotes(votes);
        detail.setComments(buildCommentTree(flatComments));
        return detail;
    }

    /**
     * 撤销自己的审批单
     *
     * @param id 审批单ID
     */
    public void cancel(Long id) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyOrder order = orderMapper.selectById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException("审批单不存在");
        }
        if (!ApplyConstants.ORDER_PENDING.equals(order.getStatus())) {
            throw new ServiceException("已经有结果的审批单不能撤销");
        }
        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .set(ApplyOrder::getStatus, ApplyConstants.ORDER_CANCELED)
            .set(ApplyOrder::getSettleTime, LocalDateTime.now())
            .eq(ApplyOrder::getId, id));
    }

    /**
     * 追加 / 修改补充说明（仅发起人，且还在审批中）
     *
     * @param id   审批单ID
     * @param note 补充说明
     */
    public void updateNote(Long id, String note) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyOrder order = orderMapper.selectById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException("审批单不存在");
        }
        if (!ApplyConstants.ORDER_PENDING.equals(order.getStatus())) {
            throw new ServiceException("这单已经结案了，写不了补充说明");
        }
        String text = note == null ? "" : note.trim();
        if (text.isEmpty()) {
            throw new ServiceException("补充说明不能为空");
        }
        if (text.length() > ApplyConstants.EXTRA_NOTE_MAX_LENGTH) {
            throw new ServiceException("补充说明最多 " + ApplyConstants.EXTRA_NOTE_MAX_LENGTH + " 字");
        }
        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .set(ApplyOrder::getExtraNote, text)
            .eq(ApplyOrder::getId, id));
    }

    /**
     * 删除自己的审批单（逻辑删除）
     *
     * @param id 审批单ID
     */
    public void removeMine(Long id) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyOrder order = orderMapper.selectById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new ServiceException("审批单不存在");
        }
        orderMapper.deleteById(id);
    }

    /**
     * 收藏 / 取消收藏
     *
     * @param orderId 审批单ID
     * @return true 已收藏，false 已取消
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long orderId) {
        Long userId = AppLoginHelper.requireUserId();
        ApplyOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("审批单不存在");
        }
        ApplyFavorite exist = favoriteMapper.selectOne(Wrappers.<ApplyFavorite>lambdaQuery()
            .eq(ApplyFavorite::getUserId, userId)
            .eq(ApplyFavorite::getOrderId, orderId));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            return false;
        }
        ApplyFavorite fav = new ApplyFavorite();
        fav.setUserId(userId);
        fav.setOrderId(orderId);
        favoriteMapper.insert(fav);
        return true;
    }

    /**
     * 我的：发起的 / 批过的 / 收藏的
     *
     * @param type      apply/vote/favorite
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyOrderVo> mine(String type, PageQuery pageQuery) {
        Long userId = AppLoginHelper.requireUserId();
        String t = StringUtils.blankToDefault(type, "apply");
        IPage<ApplyOrderVo> page = orderMapper.selectMinePage(pageQuery.build(), userId, t);
        decorate(page.getRecords(), userId);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 个人中心统计
     *
     * @return 统计信息
     */
    public AppProfileVo profile() {
        AppUser user = appUserMapper.selectById(AppLoginHelper.requireUserId());
        if (user == null) {
            throw new ServiceException("用户不存在", 401);
        }
        AppProfileVo vo = new AppProfileVo();
        vo.setUser(MapstructUtils.convert(user, org.dromara.apply.domain.vo.AppUserVo.class));

        vo.setApplyTotal(orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getUserId, user.getId())));
        vo.setPassedTotal(orderMapper.selectCount(Wrappers.<ApplyOrder>lambdaQuery()
            .eq(ApplyOrder::getUserId, user.getId())
            .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_PASSED)));
        vo.setVoteTotal(voteMapper.selectCount(Wrappers.<org.dromara.apply.domain.ApplyVote>lambdaQuery()
            .eq(org.dromara.apply.domain.ApplyVote::getUserId, user.getId())));
        vo.setAdoptedTotal(Long.valueOf(user.getAdoptedCount() == null ? 0 : user.getAdoptedCount()));
        vo.setFavoriteTotal(favoriteMapper.selectCount(Wrappers.<ApplyFavorite>lambdaQuery()
            .eq(ApplyFavorite::getUserId, user.getId())));

        // 我驳回成功的金额 = 我投过驳回票且最终被驳回的单金额合计
        List<Object> rejectedIds = voteMapper.selectObjs(Wrappers.<org.dromara.apply.domain.ApplyVote>lambdaQuery()
            .select(org.dromara.apply.domain.ApplyVote::getOrderId)
            .eq(org.dromara.apply.domain.ApplyVote::getUserId, user.getId())
            .eq(org.dromara.apply.domain.ApplyVote::getResult, ApplyConstants.VOTE_REJECT));
        BigDecimal saved = BigDecimal.ZERO;
        if (!rejectedIds.isEmpty()) {
            List<ApplyOrder> rejected = orderMapper.selectList(Wrappers.<ApplyOrder>lambdaQuery()
                .in(ApplyOrder::getId, rejectedIds)
                .eq(ApplyOrder::getStatus, ApplyConstants.ORDER_REJECTED));
            for (ApplyOrder o : rejected) {
                if (o.getPrice() != null) {
                    saved = saved.add(o.getPrice());
                }
            }
        }
        vo.setSavedAmount(saved);
        return vo;
    }

    // ==================== 管理端 ====================

    /**
     * 管理端分页
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页数据
     */
    public PageResult<ApplyOrderVo> adminPage(AdminOrderQueryBo bo, PageQuery pageQuery) {
        IPage<ApplyOrderVo> page = orderMapper.selectAdminPage(pageQuery.build(), bo);
        for (ApplyOrderVo vo : page.getRecords()) {
            vo.setImageList(splitImages(vo.getImages()));
        }
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 管理端详情
     *
     * @param id 审批单ID
     * @return 详情
     */
    public ApplyOrderVo adminDetail(Long id) {
        ApplyOrderVo vo = orderMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("审批单不存在");
        }
        vo.setImageList(splitImages(vo.getImages()));
        AppUser author = appUserMapper.selectById(vo.getUserId());
        if (author != null) {
            vo.setUserNickName(author.getNickName());
            vo.setUserAvatar(author.getAvatar());
        }
        return vo;
    }

    /**
     * 内容审核 / 下架
     *
     * @param id         审批单ID
     * @param auditStatus 0待审 1通过 2下架
     * @param remark     备注
     */
    public void audit(Long id, String auditStatus, String remark) {
        ApplyOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("审批单不存在");
        }
        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .set(ApplyOrder::getAuditStatus, auditStatus)
            .set(ApplyOrder::getAuditRemark, StringUtils.blankToDefault(remark, ""))
            .eq(ApplyOrder::getId, id));
    }

    /**
     * 置顶 / 取消置顶
     *
     * @param id    审批单ID
     * @param isTop 0/1
     */
    public void top(Long id, String isTop) {
        orderMapper.update(null, Wrappers.<ApplyOrder>lambdaUpdate()
            .set(ApplyOrder::getIsTop, isTop)
            .eq(ApplyOrder::getId, id));
    }

    /**
     * 管理端删除
     *
     * @param ids 主键集合
     */
    public void adminRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        orderMapper.deleteByIds(ids);
    }

    // ==================== 内部方法 ====================

    /**
     * 校验用户是否在群内
     */
    private void assertGroupMember(Long groupId, Long userId) {
        if (groupId == null) {
            return;
        }
        ApplyGroup group = groupMapper.selectById(groupId);
        if (group == null || "1".equals(group.getStatus())) {
            throw new ServiceException("审批群不存在或已解散");
        }
        if (userId == null) {
            throw new ServiceException("请先登录后查看群内审批", 401);
        }
        Long cnt = groupMemberMapper.selectCount(Wrappers.<ApplyGroupMember>lambdaQuery()
            .eq(ApplyGroupMember::getGroupId, groupId)
            .eq(ApplyGroupMember::getUserId, userId));
        if (cnt == null || cnt == 0) {
            throw new ServiceException("你还没有加入这个审批群");
        }
    }

    /**
     * 列表装饰：图片、倒计时、是否收藏
     */
    private void decorate(List<ApplyOrderVo> records, Long userId) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> orderIds = records.stream().map(ApplyOrderVo::getId).collect(Collectors.toList());
        List<Long> favIds = new ArrayList<>();
        if (userId != null) {
            List<ApplyFavorite> favs = favoriteMapper.selectList(Wrappers.<ApplyFavorite>lambdaQuery()
                .eq(ApplyFavorite::getUserId, userId)
                .in(ApplyFavorite::getOrderId, orderIds));
            favIds = favs.stream().map(ApplyFavorite::getOrderId).collect(Collectors.toList());
        }
        for (ApplyOrderVo vo : records) {
            vo.setImageList(splitImages(vo.getImages()));
            vo.setRemainSeconds(remainSeconds(vo.getExpireTime(), vo.getStatus()));
            vo.setFavorited(favIds.contains(vo.getId()));
            if (vo.getMine() == null) {
                vo.setMine(userId != null && userId.equals(vo.getUserId()));
            }
        }
    }

    private List<String> splitImages(String images) {
        if (StringUtils.isBlank(images)) {
            return new ArrayList<>();
        }
        return Arrays.stream(images.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private Long remainSeconds(ApplyOrder order) {
        return remainSeconds(order.getExpireTime(), order.getStatus());
    }

    private Long remainSeconds(LocalDateTime expireTime, String status) {
        if (expireTime == null || !ApplyConstants.ORDER_PENDING.equals(status)) {
            return 0L;
        }
        long seconds = Duration.between(LocalDateTime.now(), expireTime).getSeconds();
        return Math.max(seconds, 0L);
    }

    /**
     * 平铺评论转两层树
     */
    private List<ApplyCommentVo> buildCommentTree(List<ApplyCommentVo> flat) {
        if (flat == null || flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<ApplyCommentVo>> childrenMap = flat.stream()
            .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
            .collect(Collectors.groupingBy(ApplyCommentVo::getParentId));
        List<ApplyCommentVo> roots = flat.stream()
            .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
            .collect(Collectors.toList());
        for (ApplyCommentVo root : roots) {
            root.setChildren(childrenMap.getOrDefault(root.getId(), new ArrayList<>()));
        }
        return roots;
    }
}
