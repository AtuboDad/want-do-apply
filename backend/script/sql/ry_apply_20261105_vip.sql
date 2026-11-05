-- ----------------------------
-- 2026-11-05 增量（会员头像）：app_user 增加会员角标开关
-- 直接对已有库执行，可重复跑（幂等）
-- ----------------------------

-- 会员专属头像（preset:av21 ~ preset:av26）由小程序包内提供，
-- 用了就算会员；角标是否展示由本人开关控制，这里默认展示。
set @has_col := (
  select count(*) from information_schema.columns
  where table_schema = database() and table_name = 'app_user' and column_name = 'show_vip'
);
set @ddl := if(@has_col = 0,
  'alter table app_user add column show_vip char(1) default ''1'' comment ''是否展示会员角标（1展示 0隐藏）'' after avatar',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- 存量用户默认展示（新列 default 已覆盖，这里兜一层空值）
update app_user set show_vip = '1' where show_vip is null or show_vip = '';
