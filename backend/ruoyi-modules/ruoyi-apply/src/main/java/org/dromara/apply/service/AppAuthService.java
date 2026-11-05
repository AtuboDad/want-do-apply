package org.dromara.apply.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.apply.domain.AppUser;
import org.dromara.apply.domain.bo.AppLoginBo;
import org.dromara.apply.domain.vo.AppLoginVo;
import org.dromara.apply.domain.vo.AppUserVo;
import org.dromara.apply.mapper.AppUserMapper;
import org.dromara.apply.util.AppLoginHelper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 小程序登录服务
 *
 * @author want-do-apply
 */
@Service
@RequiredArgsConstructor
public class AppAuthService {

    private final AppUserMapper appUserMapper;
    private final WxMiniService wxMiniService;

    /**
     * 微信登录：code 换 openid，不存在则注册
     *
     * @param bo 登录参数
     * @return 登录结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AppLoginVo login(AppLoginBo bo) {
        WxMiniService.WxSession session = wxMiniService.code2Session(bo.getCode());
        if (StringUtils.isBlank(session.getOpenId())) {
            throw new ServiceException("微信登录失败：未获取到用户标识");
        }

        AppUser user = appUserMapper.selectOne(
            Wrappers.<AppUser>lambdaQuery().eq(AppUser::getOpenId, session.getOpenId()));

        boolean newUser = false;
        if (user == null) {
            user = new AppUser();
            user.setOpenId(session.getOpenId());
            user.setUnionId(session.getUnionId());
            user.setNickName(StringUtils.isNotBlank(bo.getNickName())
                ? bo.getNickName()
                : "审批员" + (System.currentTimeMillis() % 100000));
            user.setAvatar(StringUtils.isNotBlank(bo.getAvatar())
                ? bo.getAvatar() : randomPresetAvatar());
            user.setGender(StringUtils.blankToDefault(bo.getGender(), "0"));
            user.setPhone("");
            user.setCreditScore(100);
            user.setApplyCount(0);
            user.setVoteCount(0);
            user.setAdoptedCount(0);
            user.setStatus("0");
            user.setBanRemark("");
            user.setLastLoginTime(LocalDateTime.now());
            user.setDelFlag("0");
            appUserMapper.insert(user);
            newUser = true;
        } else {
            if ("1".equals(user.getStatus())) {
                throw new ServiceException("账号已被限制：" + StringUtils.blankToDefault(user.getBanRemark(), "违反社区规范"));
            }
            // 用户名/头像以用户最新授权为准（仅当本次有值）
            boolean changed = false;
            if (StringUtils.isNotBlank(bo.getNickName()) && !bo.getNickName().equals(user.getNickName())) {
                user.setNickName(bo.getNickName());
                changed = true;
            }
            if (StringUtils.isNotBlank(bo.getAvatar()) && !bo.getAvatar().equals(user.getAvatar())) {
                user.setAvatar(bo.getAvatar());
                changed = true;
            }
            // 老用户没头像的，登录时补一个默认萌粒头像，别再显示灰底小人
            if (StringUtils.isBlank(user.getAvatar())) {
                user.setAvatar(randomPresetAvatar());
                changed = true;
            }
            user.setLastLoginTime(LocalDateTime.now());
            user.setUpdateBy(user.getId());
            if (changed) {
                appUserMapper.updateById(user);
            } else {
                appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                    .set(AppUser::getLastLoginTime, LocalDateTime.now())
                    .eq(AppUser::getId, user.getId()));
            }
        }

        String token = AppLoginHelper.createToken(user.getId());
        AppLoginVo vo = new AppLoginVo();
        vo.setToken(token);
        vo.setExpireAt(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
        vo.setUser(MapstructUtils.convert(user, AppUserVo.class));
        vo.setNewUser(newUser);
        return vo;
    }

    /**
     * 当前登录用户实体
     *
     * @return 用户
     */
    public AppUser currentUser() {
        Long uid = AppLoginHelper.requireUserId();
        AppUser user = appUserMapper.selectById(uid);
        if (user == null) {
            throw new ServiceException("用户不存在", 401);
        }
        if ("1".equals(user.getStatus())) {
            throw new ServiceException("账号已被限制：" + StringUtils.blankToDefault(user.getBanRemark(), "违反社区规范"));
        }
        return user;
    }

    /**
     * 更新昵称头像
     *
     * @param nickName 昵称
     * @param avatar   头像
     */
    public void updateProfile(String nickName, String avatar, String showVip) {
        Long uid = AppLoginHelper.requireUserId();
        if (StringUtils.isBlank(nickName) && StringUtils.isBlank(avatar) && StringUtils.isBlank(showVip)) {
            return;
        }
        if (StringUtils.isNotBlank(avatar) && !isAllowedAvatar(avatar)) {
            throw new ServiceException("头像地址不合法");
        }
        boolean hasShowVip = StringUtils.isNotBlank(showVip);
        if (hasShowVip && !"0".equals(showVip) && !"1".equals(showVip)) {
            throw new ServiceException("会员角标设置不合法");
        }
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
            .set(StringUtils.isNotBlank(nickName), AppUser::getNickName, nickName)
            .set(StringUtils.isNotBlank(avatar), AppUser::getAvatar, avatar)
            .set(hasShowVip, AppUser::getShowVip, showVip)
            .eq(AppUser::getId, uid));
    }

    /**
     * 默认头像：8 张萌粒风插画里随机一张。
     * 存的是 preset:avNN 标记，小程序端直接读包内图片，不走网络。
     */
    private String randomPresetAvatar() {
        return "preset:av0" + (ThreadLocalRandom.current().nextInt(8) + 1);
    }

    /**
     * 头像只接受三种形式：
     * preset:av01 —— 我们自己的预设头像（前端读包内图片）
     * /upload/xxx.jpg —— 用户上传/微信头像，落在本服务
     * http(s)://... —— 微信头像直链
     * 挡住任意字符串写库，也避免出现 javascript: 之类的脏值。
     */
    private boolean isAllowedAvatar(String avatar) {
        if (avatar.length() > 255) {
            return false;
        }
        if (avatar.startsWith("preset:")) {
            String id = avatar.substring("preset:".length());
            return StringUtils.isNotBlank(id) && id.matches("[A-Za-z0-9_-]{1,32}");
        }
        return avatar.startsWith("/upload/") || avatar.startsWith("http://") || avatar.startsWith("https://");
    }

    // ==================== 管理端 ====================

    /**
     * 管理端用户分页
     *
     * @param keyword   昵称关键字
     * @param status    状态
     * @param pageQuery 分页
     * @return 分页数据
     */
    public org.dromara.common.core.domain.PageResult<AppUserVo> adminPage(
        String keyword, String status, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        var wrapper = Wrappers.<AppUser>lambdaQuery();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(AppUser::getNickName, keyword);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AppUser::getStatus, status);
        }
        wrapper.orderByDesc(AppUser::getCreateTime);
        var page = appUserMapper.selectVoPage(pageQuery.build(), wrapper);
        return org.dromara.common.core.domain.PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 管理端修改用户状态
     *
     * @param bo 编辑参数
     */
    public void adminUpdate(org.dromara.apply.domain.bo.AppUserBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        AppUser user = appUserMapper.selectById(bo.getId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
            .set(AppUser::getStatus, bo.getStatus())
            .set(AppUser::getBanRemark, StringUtils.blankToDefault(bo.getBanRemark(), ""))
            .set(bo.getCreditScore() != null, AppUser::getCreditScore, bo.getCreditScore())
            .set(StringUtils.isNotBlank(bo.getNickName()), AppUser::getNickName, bo.getNickName())
            .eq(AppUser::getId, bo.getId()));
    }
}
