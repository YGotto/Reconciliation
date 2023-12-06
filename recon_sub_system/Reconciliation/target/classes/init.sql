create database if not exists se;
use se;
drop table if exists account_backup;
drop table if exists abnormal_transactions;
drop table if exists recharge;
drop table if exists transaction_flow;
drop table if exists user;
drop table if exists user_order;

create table if not exists account_backup(
user_id     int,
date        date,
balance     decimal(8,2) not null,
state       int not null, #异常处理状态 0-无异常 1-总账异常 2-行为异常 3-异常已处理 4-账户冻结
cutoff      time,
charge      decimal(8,2) not null,
primary key(user_id, date)
);

create table if not exists abnormal_transactions(
transaction_id  int,
user_id         int not null,
seller_id       int not null,
order_id        int not null,
amount          decimal(8,2) not null, #可正可负，考虑订单退款隔天
time            datetime,
primary key(transaction_id)
);

CREATE TABLE if not exists `recharge` (
`id` int NOT NULL AUTO_INCREMENT,
`user_id` int NOT NULL,
`amount` decimal(10, 2) NOT NULL,
`date` datetime NOT NULL,
PRIMARY KEY (`id`) USING BTREE
);

CREATE TABLE if not exists `user` (
`id` int NOT NULL,
`user_name` varchar(20) NOT NULL,
`user_password` varchar(20) NOT NULL,
`type` int NOT NULL,
`name` varchar(20) NOT NULL,
`vip_value` int DEFAULT NULL,
`real_name` varchar(20) DEFAULT NULL,
`id_card` varchar(20) DEFAULT NULL,
`phone_number` varchar(20) DEFAULT NULL,
`email` varchar(20) DEFAULT NULL,
`verification_status` int DEFAULT NULL,
`payment_password` varchar(20) DEFAULT NULL,
`balance` decimal(8,2) DEFAULT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE if not exists transaction_flow (
`transaction_id` int NOT NULL AUTO_INCREMENT,
`order_id` int DEFAULT NULL,
`order_state` int DEFAULT NULL,
`create_time` timestamp NULL DEFAULT NULL,
PRIMARY KEY (`transaction_id`)
);

CREATE TABLE `user_order` (
`order_id` int NOT NULL AUTO_INCREMENT,
`buyer_id` int DEFAULT NULL,
`seller_id` int DEFAULT NULL,
`total_price` decimal(8,2) DEFAULT NULL,
`item_id` int DEFAULT NULL,
`order_state` int DEFAULT NULL,
`complaint_record` varchar(100) DEFAULT '',
primary key (`order_id`)
);


insert into user values
(1, 'A', '', 1, '', 1, '',
 '', '', '', 1, '', 70),
(2, 'B', '', 1, '', 1, '',
 '', '', '', 1, '', 85),
(3, 'C', '', 1, '', 1, '',
 '', '', '', 1, '', 100),
(4, 'D', '', 1, '', 1, '',
 '', '', '', 1, '', 85),
(5, 'E', '', 1, '', 1, '',
 '', '', '', 1, '', 75),
(6, 'F', '', 1, '', 1, '',
 '', '', '', 1, '', 75);

insert into transaction_flow values
(1, 1, 1, '2023-06-01 01:00:45'),
(2, 1, 2, '2023-06-01 02:00:45'),
(3, 1, 3, '2023-06-01 03:00:45'),
(4, 1, 4, '2023-06-01 10:00:45'),
(5, 1, 5, '2023-06-01 15:00:45'),
(6, 1, 6, '2023-06-01 19:00:45'),
(7, 1, 7, '2023-06-01 20:00:45'),
(8, 1, 8, '2023-06-01 23:00:45'),

(18, 8, 1, '2023-05-31 01:00:45'),
(19, 8, 2, '2023-05-31 02:00:45'),
(20, 8, 3, '2023-05-31 03:00:45'),
(21, 8, 4, '2023-05-31 10:00:45'),
(22, 8, 5, '2023-05-31 15:00:45'),
(23, 8, 6, '2023-05-31 19:00:45'),
(24, 8, 7, '2023-05-31 20:00:45'),
(25, 8, 8, '2023-05-31 23:00:45'),
(26, 9, 3, '2023-05-31 05:00:45'),

(9, 2, 3, '2023-06-01 14:45:11'),
(10, 2, 10, '2023-06-01 15:30:45'),
(11, 2, 0, '2023-06-01 16:45:45'),
(12, 3, 2, '2023-06-01 11:45:14'),
(13, 4, 3, '2023-06-01 11:45:14'),
(14, 5, 3, '2023-06-01 09:14:15'),
(15, 6, 3, '2023-06-01 10:25:16'),
(16, 7, 3, '2023-06-01 00:32:13'),
(17, 7, 6, '2023-06-01 14:14:15');

insert into user_order values
(1, 1, 54, 15, 14, 8, 'aaa'),
(2, 1, 84, 20, 17, 0, 'bbb'),
(3, 1, 29, 35, 9, 3, 'cca'),
(4, 2, 29, 35, 9, 3, 'cca'),
(5, 3, 14, 120, 7, 4, 'cdc'),
(6, 3, 14, 60, 7, 5, 'cdc'),
(7, 4, 20, 35, 7, 6, 'aat'),

(8, 1, 20, 30, 7, 8, 'aat'),
(9, 6, 20, 200000, 7, 3, 'aat');
insert into account_backup values
(1,'2023-5-30',40,0,null,0),
(1,'2023-5-31',50,1,null,10),
# (1,'2023-6-01',70,0,null,20),   #55
(2,'2023-5-30',200,0,null,0),
(2,'2023-5-31',100,0,null,20),
# (2,'2023-6-01',85,1,null,20),  #85
(3,'2023-5-30',200,0,null,0),
(3,'2023-5-31',200,0,null,0),
# (3,'2023-6-01',100,1,null,20),  #40
(4,'2023-5-30',95,0,null,0),
(4,'2023-5-31',75,0,'1:00:00',15),
# (4,'2023-6-01',85,0,null,10),    #85
(5,'2023-5-30',75,0,null,0),
(5,'2023-5-31',75,0,null,0),
(6,'2023-5-30',200075.00,0,null,0),
(6,'2023-5-31',75,2,null,0);
# (5,'2023-6-01',75,0,null,0);    #75

insert into recharge values
(1,1,10,'2023-6-1 23:30:20'),
(2,1,10,'2023-6-1 00:30:20'),
(3,2,10,'2023-6-1 23:30:20'),
(4,2,10,'2023-6-1 23:30:20'),
(5,3,10,'2023-6-1 23:30:20'),
(6,3,10,'2023-6-1 23:30:20'),
(7,4,15,'2023-6-1 00:00:18'),
(8,4,10,'2023-6-1 02:00:18');

insert into abnormal_transactions values
(20,1,20,8,-30,'2023-05-31 03:00:45'),
(26, 6, 20, 9, -200000,'2023-05-31 05:00:45');
