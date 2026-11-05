-- ----------------------------
-- 2026-11-05 增量：审批方式 + 存量用户默认头像
-- 直接对已有库执行，可重复跑（幂等）
-- ----------------------------

-- 1. 审批单增加「审批方式」：0=单人审批（一人表态即出结果），1=多人审批（时长内汇总意见）
set @has_col := (
  select count(*) from information_schema.columns
  where table_schema = database() and table_name = 'apply_order' and column_name = 'approve_mode'
);
set @ddl := if(@has_col = 0,
  'alter table apply_order add column approve_mode char(1) default ''1'' comment ''审批方式（0单人 1多人）'' after visibility',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- 2. 存量用户补默认头像：preset:avNN 由小程序读包内图片，按 id 轮转保证每人不一样
--    （预设头像已扩到 20 张，新库直接按 20 轮转；老库只补空头像的，不动已有选择）
update app_user u
join (
  select id, row_number() over (order by id) as rn
  from app_user
  where del_flag = '0'
) t on t.id = u.id
set u.avatar = concat('preset:av', lpad(((t.rn - 1) % 20) + 1, 2, '0'))
where u.avatar is null or u.avatar = '';

-- 3. 购买理由上限 500 -> 1000（长理由要写得下，varchar(500) 会直接截断报错）
alter table apply_order modify column reason varchar(1000) default '' comment '购买理由';
