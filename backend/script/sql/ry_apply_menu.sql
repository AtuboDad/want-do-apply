-- ----------------------------
-- 钱包请等等 - 管理端菜单与权限
-- 依赖: ry_vue.sql
-- 可重复执行（先删后插）
-- ----------------------------

delete from sys_role_menu where menu_id between 2000 and 2099;
delete from sys_menu where menu_id between 2000 and 2099;

-- 一级目录
insert into sys_menu values(2000, '消费审批', 0, 2, 'apply', null, '', 'N', 'Y', 'M', '0', '0', '', 'shopping', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '钱包请等等 消费审批');

-- 数据看板
insert into sys_menu values(2001, '数据看板', 2000, 1, 'dashboard', 'apply/dashboard/index', '', 'N', 'Y', 'C', '0', '0', 'apply:dashboard:view', 'dashboard', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '消费审批数据看板');

-- 审批单管理
insert into sys_menu values(2002, '审批单管理', 2000, 2, 'order', 'apply/order/index', '', 'N', 'Y', 'C', '0', '0', 'apply:order:list', 'list', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '审批单管理菜单');
insert into sys_menu values(2010, '审批单查询', 2002, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:order:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(2011, '审批单审核', 2002, 2, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:order:audit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(2012, '审批单置顶', 2002, 3, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:order:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(2013, '审批单删除', 2002, 4, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:order:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 批条管理
insert into sys_menu values(2003, '批条管理', 2000, 3, 'vote', 'apply/vote/index', '', 'N', 'Y', 'C', '0', '0', 'apply:vote:list', 'message', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '批条管理菜单');
insert into sys_menu values(2020, '批条删除', 2003, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:vote:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 评论管理
insert into sys_menu values(2004, '评论管理', 2000, 4, 'comment', 'apply/comment/index', '', 'N', 'Y', 'C', '0', '0', 'apply:comment:list', 'comment', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '评论管理菜单');
insert into sys_menu values(2030, '评论删除', 2004, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:comment:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 审批群
insert into sys_menu values(2005, '审批群', 2000, 5, 'group', 'apply/group/index', '', 'N', 'Y', 'C', '0', '0', 'apply:group:list', 'peoples', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '审批群管理菜单');
insert into sys_menu values(2040, '群解散', 2005, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:group:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 小程序用户
insert into sys_menu values(2006, '小程序用户', 2000, 6, 'user', 'apply/user/index', '', 'N', 'Y', 'C', '0', '0', 'apply:user:list', 'user', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '小程序用户管理菜单');
insert into sys_menu values(2050, '用户编辑', 2006, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:user:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 举报处理
insert into sys_menu values(2007, '举报处理', 2000, 7, 'report', 'apply/report/index', '', 'N', 'Y', 'C', '0', '0', 'apply:report:list', 'warning', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '举报处理菜单');
insert into sys_menu values(2060, '举报处理操作', 2007, 1, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:report:handle', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(2061, '举报删除', 2007, 2, '', null, '', 'N', 'Y', 'F', '0', '0', 'apply:report:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 授权给超级管理员角色(role_id = 1)
insert into sys_role_menu(role_id, menu_id) select 1, menu_id from sys_menu where menu_id between 2000 and 2099;
