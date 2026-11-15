-- 有空 · 生产环境 MySQL 8 建表脚本
-- 与 src/main/resources/sql/schema.sql 的差异：加了存储引擎、字符集与二级索引

create database if not exists youkong default character set utf8mb4 collate utf8mb4_0900_ai_ci;
use youkong;

create table if not exists shop (
    id bigint auto_increment primary key,
    name varchar(64) not null,
    open_time time not null,
    close_time time not null,
    slot_minutes int not null default 15,
    created_at timestamp not null default current_timestamp
) engine = InnoDB default charset = utf8mb4;

create table if not exists staff (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    name varchar(32) not null,
    title varchar(32) default null,
    active tinyint(1) not null default 1,
    key idx_staff_shop (shop_id)
) engine = InnoDB default charset = utf8mb4;

create table if not exists service_item (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    name varchar(64) not null,
    duration_minutes int not null,
    price_cent int not null,
    key idx_service_shop (shop_id)
) engine = InnoDB default charset = utf8mb4;

create table if not exists staff_schedule (
    id bigint auto_increment primary key,
    staff_id bigint not null,
    day_of_week int not null comment '1=周一 ... 7=周日',
    start_time time not null,
    end_time time not null,
    unique key uk_staff_day (staff_id, day_of_week)
) engine = InnoDB default charset = utf8mb4;

create table if not exists booking (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    staff_id bigint not null,
    service_id bigint not null,
    customer_name varchar(32) not null,
    customer_phone varchar(20) not null,
    start_at datetime not null,
    end_at datetime not null,
    status varchar(16) not null,
    version int not null default 0,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    key idx_booking_staff_start (staff_id, start_at),
    key idx_booking_status_created (status, created_at)
) engine = InnoDB default charset = utf8mb4;

-- 核心表：唯一索引是防超卖的唯一真相来源
create table if not exists booking_slot (
    id bigint auto_increment primary key,
    staff_id bigint not null,
    slot_start datetime not null,
    booking_id bigint default null,
    unique key uk_staff_slot (staff_id, slot_start),
    key idx_slot_booking (booking_id)
) engine = InnoDB default charset = utf8mb4;

-- 后台登录账号。初始账号由应用首次启动时写入，密码用 BCrypt 存储
create table if not exists account (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    username varchar(32) not null,
    password_hash varchar(100) not null,
    role varchar(16) not null,
    created_at datetime not null default current_timestamp,
    unique key uk_account_username (username)
) engine = InnoDB default charset = utf8mb4;

-- 临时请假 / 特殊营业时间。无记录按周排班，有记录且时间为空表示当天休息
create table if not exists staff_schedule_exception (
    id bigint auto_increment primary key,
    staff_id bigint not null,
    work_date date not null,
    start_time time default null,
    end_time time default null,
    unique key uk_staff_work_date (staff_id, work_date)
) engine = InnoDB default charset = utf8mb4;
