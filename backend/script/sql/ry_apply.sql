-- ----------------------------
-- 钱包请等等 - 消费审批业务表
-- 依赖: ry_vue.sql (RuoYi-Vue-Plus 基础库)
-- 字符集: utf8mb4
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 小程序用户表
-- ----------------------------
drop table if exists app_user;
create table app_user (
    id                bigint(20)      not null                   comment '用户ID',
    open_id           varchar(64)     not null                   comment '微信openid',
    union_id          varchar(64)     default null               comment '微信unionid',
    nick_name         varchar(64)     default ''                 comment '昵称',
    avatar            varchar(500)    default ''                 comment '头像地址',
    gender            char(1)         default '0'                comment '性别（0未知 1男 2女）',
    phone             varchar(20)     default ''                 comment '手机号',
    credit_score      int(11)         default 100                comment '信用分',
    apply_count       int(11)         default 0                  comment '发起审批数',
    vote_count        int(11)         default 0                  comment '批阅数',
    adopted_count     int(11)         default 0                  comment '批条被采纳数',
    status            char(1)         default '0'                comment '状态（0正常 1封禁）',
    ban_remark        varchar(200)    default ''                 comment '封禁原因',
    last_login_time   datetime                                   comment '最后登录时间',
    del_flag          char(1)         default '0'                comment '删除标志（0存在 1删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (id),
    unique key uk_app_user_openid (open_id),
    key idx_app_user_status (status),
    key idx_app_user_create_time (create_time)
) engine=innodb comment = '小程序用户表';

-- ----------------------------
-- 2. 审批群表
-- ----------------------------
drop table if exists apply_group;
create table apply_group (
    id                bigint(20)      not null                   comment '群ID',
    name              varchar(50)     not null                   comment '群名称',
    owner_id          bigint(20)      not null                   comment '群主用户ID',
    invite_code       varchar(16)     not null                   comment '邀请码',
    notice            varchar(200)    default ''                 comment '群公告',
    member_count      int(11)         default 1                  comment '成员数',
    status            char(1)         default '0'                comment '状态（0正常 1解散）',
    del_flag          char(1)         default '0'                comment '删除标志（0存在 1删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (id),
    unique key uk_apply_group_code (invite_code),
    key idx_apply_group_owner (owner_id)
) engine=innodb comment = '审批群表';

-- ----------------------------
-- 3. 审批群成员表
-- ----------------------------
drop table if exists apply_group_member;
create table apply_group_member (
    id                bigint(20)      not null                   comment '主键',
    group_id          bigint(20)      not null                   comment '群ID',
    user_id           bigint(20)      not null                   comment '用户ID',
    role              char(1)         default '0'                comment '角色（0成员 1群主）',
    create_time       datetime                                   comment '加入时间',
    primary key (id),
    unique key uk_group_member (group_id, user_id),
    key idx_group_member_user (user_id)
) engine=innodb comment = '审批群成员表';

-- ----------------------------
-- 4. 审批单表
-- ----------------------------
drop table if exists apply_order;
create table apply_order (
    id                bigint(20)      not null                   comment '审批单ID',
    user_id           bigint(20)      not null                   comment '发起人ID',
    group_id          bigint(20)      default null               comment '审批群ID（为空表示公开广场）',
    title             varchar(100)    not null                   comment '商品名称',
    price             decimal(10,2)   default 0.00               comment '商品价格',
    reason            varchar(500)    default ''                 comment '购买理由',
    substitute        varchar(200)    default ''                 comment '替代品（为空表示没有）',
    extra_note        varchar(500)    default ''                 comment '发起人补充说明',
    images            varchar(1000)   default ''                 comment '图片，多个用逗号分隔',
    category          varchar(30)     default 'other'            comment '分类',
    visibility        char(1)         default '0'                comment '可见范围（0公开 1群内）',
    approve_mode      char(1)         default '1'                comment '审批方式（0单人 1多人）',
    duration_hours    int(11)         default 24                 comment '审批时长（小时）',
    status            char(1)         default '0'                comment '状态（0待审批 1已通过 2已驳回 3已撤销 4已过期）',
    pass_count        int(11)         default 0                  comment '通过票数',
    reject_count      int(11)         default 0                  comment '驳回票数',
    vote_count        int(11)         default 0                  comment '已批阅人数',
    target_votes      int(11)         default 0                  comment '已废弃：早期按票数结案，现改为到点少数服从多数',
    comment_count     int(11)         default 0                  comment '评论数',
    hot_score         int(11)         default 0                  comment '热度分',
    expire_time       datetime                                   comment '审批截止时间',
    settle_time       datetime                                   comment '结算时间',
    audit_status      char(1)         default '1'                comment '审核状态（0待审 1通过 2下架）',
    audit_remark      varchar(200)    default ''                 comment '审核备注',
    is_top            char(1)         default '0'                comment '是否置顶（0否 1是）',
    del_flag          char(1)         default '0'                comment '删除标志（0存在 1删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (id),
    key idx_apply_order_user (user_id),
    key idx_apply_order_group (group_id),
    key idx_apply_order_status (status),
    key idx_apply_order_visibility (visibility),
    key idx_apply_order_create_time (create_time)
) engine=innodb comment = '审批单表';

-- ----------------------------
-- 5. 批条（投票）表
-- ----------------------------
drop table if exists apply_vote;
create table apply_vote (
    id                bigint(20)      not null                   comment '批条ID',
    order_id          bigint(20)      not null                   comment '审批单ID',
    user_id           bigint(20)      not null                   comment '批阅人ID',
    result            char(1)         not null                   comment '审批结论（1通过 2驳回）',
    reason            varchar(200)    not null                   comment '批条理由',
    like_count        int(11)         default 0                  comment '点赞数',
    del_flag          char(1)         default '0'                comment '删除标志（0存在 1删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id),
    unique key uk_apply_vote_order_user (order_id, user_id, del_flag),
    key idx_apply_vote_user (user_id),
    key idx_apply_vote_order (order_id)
) engine=innodb comment = '批条表';

-- ----------------------------
-- 6. 评论表
-- ----------------------------
drop table if exists apply_comment;
create table apply_comment (
    id                bigint(20)      not null                   comment '评论ID',
    order_id          bigint(20)      not null                   comment '审批单ID',
    user_id           bigint(20)      not null                   comment '评论人ID',
    parent_id         bigint(20)      default 0                  comment '父评论ID（0为顶层）',
    reply_user_id     bigint(20)      default null               comment '被回复人ID',
    content           varchar(500)    not null                   comment '评论内容',
    like_count        int(11)         default 0                  comment '点赞数',
    del_flag          char(1)         default '0'                comment '删除标志（0存在 1删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id),
    key idx_apply_comment_order (order_id),
    key idx_apply_comment_user (user_id),
    key idx_apply_comment_parent (parent_id)
) engine=innodb comment = '审批单评论表';

-- ----------------------------
-- 7. 收藏表
-- ----------------------------
drop table if exists apply_favorite;
create table apply_favorite (
    id                bigint(20)      not null                   comment '主键',
    user_id           bigint(20)      not null                   comment '用户ID',
    order_id          bigint(20)      not null                   comment '审批单ID',
    create_time       datetime                                   comment '收藏时间',
    primary key (id),
    unique key uk_favorite_user_order (user_id, order_id),
    key idx_favorite_order (order_id)
) engine=innodb comment = '审批单收藏表';

-- ----------------------------
-- 8. 举报表
-- ----------------------------
drop table if exists apply_report;
create table apply_report (
    id                bigint(20)      not null                   comment '举报ID',
    target_type       char(1)         not null                   comment '对象类型（1审批单 2批条 3评论）',
    target_id         bigint(20)      not null                   comment '对象ID',
    reporter_id       bigint(20)      not null                   comment '举报人ID',
    reason_type       varchar(30)     default 'other'            comment '举报类型',
    content           varchar(500)    default ''                 comment '举报说明',
    status            char(1)         default '0'                comment '状态（0待处理 1已处理 2已驳回）',
    handle_by         bigint(20)      default null               comment '处理人',
    handle_time       datetime                                   comment '处理时间',
    handle_remark     varchar(200)    default ''                 comment '处理意见',
    del_flag          char(1)         default '0'                comment '删除标志（0存在 1删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id),
    key idx_report_status (status),
    key idx_report_target (target_type, target_id)
) engine=innodb comment = '举报表';

-- ----------------------------
-- 9. 消息通知表
-- ----------------------------
drop table if exists app_notice;
create table app_notice (
    id                bigint(20)      not null                   comment '通知ID',
    user_id           bigint(20)      not null                   comment '接收人ID',
    type              char(1)         default '1'                comment '类型（1收到批条 2审批结果 3评论回复 4群邀请 5系统消息）',
    title             varchar(100)    default ''                 comment '标题',
    content           varchar(300)    default ''                 comment '内容',
    biz_id            bigint(20)      default null               comment '业务ID',
    is_read           char(1)         default '0'                comment '是否已读（0未读 1已读）',
    create_time       datetime                                   comment '创建时间',
    primary key (id),
    key idx_notice_user_read (user_id, is_read)
) engine=innodb comment = '消息通知表';

-- ----------------------------
-- 10. 批条点赞表
-- ----------------------------
drop table if exists apply_vote_like;
create table apply_vote_like (
    id                bigint(20)      not null                   comment '主键',
    vote_id           bigint(20)      not null                   comment '批条ID',
    user_id           bigint(20)      not null                   comment '用户ID',
    create_time       datetime                                   comment '点赞时间',
    primary key (id),
    unique key uk_vote_like (vote_id, user_id)
) engine=innodb comment = '批条点赞表';
