-- 示例数据：用 MERGE 保证可重复执行（单元测试会多次启动上下文，共用同一个内存库）
merge into shop (id, name, open_time, close_time, slot_minutes, cancel_before_minutes) key (id)
values (1, '拾光美甲 · 示例店', '10:00', '20:00', 15, 120);

merge into staff (id, shop_id, name, title, active) key (id) values
(1, 1, '小鹿', '美甲师', true),
(2, 1, '阿岚', '美甲师', true);

merge into service_item (id, shop_id, name, duration_minutes, price_cent) key (id) values
(1, 1, '基础单色', 30, 6800),
(2, 1, '法式延长', 90, 25800),
(3, 1, '手部护理', 45, 12800);

-- 1=周一 ... 7=周日
merge into staff_schedule (id, staff_id, day_of_week, start_time, end_time) key (id) values
(1, 1, 1, '10:00', '20:00'),
(2, 1, 2, '10:00', '20:00'),
(3, 1, 3, '10:00', '20:00'),
(4, 1, 4, '10:00', '20:00'),
(5, 1, 5, '10:00', '20:00'),
(6, 1, 6, '10:00', '20:00'),
(7, 1, 7, '12:00', '18:00'),
(8, 2, 1, '12:00', '21:00'),
(9, 2, 2, '12:00', '21:00'),
(10, 2, 3, '12:00', '21:00'),
(11, 2, 4, '12:00', '21:00'),
(12, 2, 5, '12:00', '21:00'),
(13, 2, 6, '10:00', '18:00'),
(14, 2, 7, '10:00', '18:00');
