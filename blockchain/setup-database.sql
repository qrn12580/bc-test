-- 区块链安全管理系统数据库初始化脚本
-- Blockchain Security Management System Database Setup Script

-- 设置客户端字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection=utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS blockchain_voting 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'blockchain'@'localhost' IDENTIFIED BY 'password';
CREATE USER IF NOT EXISTS 'blockchain'@'%' IDENTIFIED BY 'password';

-- 授予权限
GRANT ALL PRIVILEGES ON blockchain_voting.* TO 'blockchain'@'localhost';
GRANT ALL PRIVILEGES ON blockchain_voting.* TO 'blockchain'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 使用数据库
USE blockchain_voting;

-- 设置数据库会话字符集
SET NAMES utf8mb4;

-- 显示创建结果
SELECT 'Database blockchain_voting created successfully' AS Status;
SELECT 'User blockchain created and granted privileges' AS Status;

-- 显示数据库信息
SHOW DATABASES LIKE 'blockchain_voting';
SELECT User, Host FROM mysql.user WHERE User = 'blockchain';

-- 创建一些基础表（如果需要预先创建）
-- 注意：Spring Boot JPA会自动创建表，这里只是示例

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认配置
INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('system.version', '1.0.0', 'System Version'),
('security.trust.threshold', '0.6', 'Node Trust Threshold'),
('security.key.rotation.days', '30', 'Key Rotation Period in Days'),
('network.default.condition', 'NORMAL', 'Default Network Condition'),
('security.auto.blacklist', 'true', 'Enable Auto Blacklist'),
('system.initialized', 'false', 'System Initialization Status');

-- 创建管理员用户表
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) DEFAULT 'ADMIN',
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认管理员用户（密码：admin123，实际使用时应该加密）
INSERT IGNORE INTO admin_users (username, password_hash, email, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLkxNvtYdR9c5Fy9.Fy6', 'admin@blockchain.local', 'SUPER_ADMIN'),
('security', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLkxNvtYdR9c5Fy9.Fy6', 'security@blockchain.local', 'SECURITY_ADMIN');

-- 创建系统日志表
CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_level VARCHAR(20) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    message TEXT NOT NULL,
    details TEXT,
    user_id BIGINT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_level (log_level),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建安全事件表
CREATE TABLE IF NOT EXISTS security_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    source VARCHAR(100),
    target VARCHAR(100),
    description TEXT,
    event_data JSON,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    resolved_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_created_at (created_at),
    INDEX idx_is_resolved (is_resolved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 显示创建的表
SHOW TABLES;

-- 显示表结构
DESCRIBE system_config;
DESCRIBE admin_users;
DESCRIBE system_logs;
DESCRIBE security_events;

-- 插入初始化日志（使用英文避免编码问题）
INSERT INTO system_logs (log_level, category, message, details) VALUES
('INFO', 'SYSTEM', 'Database initialization completed', 'Basic tables and configurations created');

INSERT INTO security_events (event_type, severity, source, description, event_data) VALUES
('SYSTEM_INIT', 'INFO', 'DATABASE', 'Database security initialization completed', JSON_OBJECT('tables_created', 4, 'default_configs', 6));

-- 显示初始化结果
SELECT 'Database initialization completed successfully!' AS Result;
SELECT COUNT(*) AS ConfigCount FROM system_config;
SELECT COUNT(*) AS AdminCount FROM admin_users;

-- 显示数据库大小
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables 
WHERE table_schema = 'blockchain_voting'
GROUP BY table_schema; 