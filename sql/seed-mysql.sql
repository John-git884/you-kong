-- 演示数据（MySQL 版）。H2 用的是 merge into，MySQL 用 INSERT ... ON DUPLICATE KEY，
-- 两边语义一致：可重复执行，不会插重复。
--
--   mysql -u root -p youkong < sql/seed-mysql.sql

insert into shop (id, name, open_time, close_time, slot_minutes, cancel_before_minutes)
values (1, '拾光美甲 · 示例店', '10:00', '20:00', 15, 120)
on duplicate key update name = values(name);

insert into staff (id, shop_id, name, title, active) values
(1, 1, '小鹿', '美甲师', true),
(2, 1, '阿岚', '美甲师', true)
on duplicate key update name = values(name);

insert into service_item (id, shop_id, name, duration_minutes, price_cent) values
(1, 1, '基础单色', 30, 6800),
(2, 1, '法式延长', 90, 25800),
(3, 1, '手部护理', 45, 12800)
on duplicate key update name = values(name);

insert into staff_schedule (id, staff_id, day_of_week, start_time, end_time) values
(1, 1, 1, '10:00', '20:00'), (2, 1, 2, '10:00', '20:00'), (3, 1, 3, '10:00', '20:00'),
(4, 1, 4, '10:00', '20:00'), (5, 1, 5, '10:00', '20:00'), (6, 1, 6, '10:00', '20:00'),
(7, 1, 7, '12:00', '18:00'),
(8, 2, 1, '12:00', '21:00'), (9, 2, 2, '12:00', '21:00'), (10, 2, 3, '12:00', '21:00'),
(11, 2, 4, '12:00', '21:00'), (12, 2, 5, '12:00', '21:00'),
(13, 2, 6, '10:00', '18:00'), (14, 2, 7, '10:00', '18:00')
on duplicate key update start_time = values(start_time);
