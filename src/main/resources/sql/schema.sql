create table if not exists shop (
    id bigint auto_increment primary key,
    name varchar(64) not null,
    open_time time not null,
    close_time time not null,
    slot_minutes int not null default 15,
    created_at timestamp not null default current_timestamp
);

create table if not exists staff (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    name varchar(32) not null,
    title varchar(32),
    active boolean not null default true
);

create table if not exists service_item (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    name varchar(64) not null,
    duration_minutes int not null,
    price_cent int not null
);

create table if not exists staff_schedule (
    id bigint auto_increment primary key,
    staff_id bigint not null,
    day_of_week int not null,
    start_time time not null,
    end_time time not null,
    constraint uk_staff_day unique (staff_id, day_of_week)
);

create table if not exists booking (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    staff_id bigint not null,
    service_id bigint not null,
    customer_name varchar(32) not null,
    customer_phone varchar(20) not null,
    start_at timestamp not null,
    end_at timestamp not null,
    status varchar(16) not null,
    version int not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists booking_slot (
    id bigint auto_increment primary key,
    staff_id bigint not null,
    slot_start timestamp not null,
    booking_id bigint,
    constraint uk_staff_slot unique (staff_id, slot_start)
);

create table if not exists account (
    id bigint auto_increment primary key,
    shop_id bigint not null,
    username varchar(32) not null,
    password_hash varchar(100) not null,
    role varchar(16) not null,
    created_at timestamp not null default current_timestamp,
    constraint uk_account_username unique (username)
);
