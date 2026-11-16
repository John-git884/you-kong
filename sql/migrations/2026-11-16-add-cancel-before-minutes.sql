-- 存量数据库升级脚本：店铺表增加「提前取消」窗口
--
-- 什么时候需要执行？
--   全新部署不需要 —— sql/init-mysql.sql 建表时已经带上这一列。
--   只有「库里已经有 shop 表」的情况才需要，否则应用启动后查询会报
--   Unknown column 'cancel_before_minutes' in 'field list'。
--
-- 执行方式：
--   mysql -u root -p youkong < sql/migrations/2026-11-16-add-cancel-before-minutes.sql

alter table shop
    add column cancel_before_minutes int not null default 120
        comment '开场前多少分钟内不允许顾客自助取消，0 表示不限制';

-- 存量门店先统一按 2 小时处理；不想限制的话把它改成 0
-- update shop set cancel_before_minutes = 0;
