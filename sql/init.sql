-- =============================================
-- 动态数据源示例：数据库初始化脚本
-- 用户名 root  密码 123456
-- =============================================

-- 创建主库（master）
CREATE DATABASE IF NOT EXISTS db_master
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 创建从库（slave）
CREATE DATABASE IF NOT EXISTS db_slave
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- =============================================
-- 主库建表 + 测试数据
-- =============================================
USE db_master;

CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `email`       VARCHAR(100)                         COMMENT '邮箱',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 主库写入不同的测试数据，方便观察数据源切换效果
INSERT INTO `user` (`username`, `email`) VALUES
    ('master_张三', 'zhangsan@master.com'),
    ('master_李四', 'lisi@master.com');

-- =============================================
-- 从库建表 + 测试数据
-- =============================================
USE db_slave;

CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `email`       VARCHAR(100)                         COMMENT '邮箱',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 从库写入不同的测试数据，方便观察数据源切换效果
INSERT INTO `user` (`username`, `email`) VALUES
    ('slave_王五', 'wangwu@slave.com'),
    ('slave_赵六', 'zhaoliu@slave.com');
