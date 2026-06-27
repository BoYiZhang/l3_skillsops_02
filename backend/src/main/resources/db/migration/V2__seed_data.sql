INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');

INSERT INTO permissions (code, name) VALUES
('skill:create', '创建Skill'),
('skill:submit', '提交审核'),
('skill:edit', '编辑Skill'),
('skill:view', '查看Skill'),
('skill:install', '安装Skill'),
('skill:rate', '评分Skill'),
('skill:approve', '审核通过'),
('skill:reject', '审核拒绝'),
('skill:delist', '下架Skill'),
('admin:categories', '管理分类'),
('admin:stats', '查看统计');

INSERT INTO role_permissions (role_id, permission_id) SELECT 1, id FROM permissions;
INSERT INTO role_permissions (role_id, permission_id) SELECT 2, id FROM permissions WHERE code IN ('skill:create','skill:submit','skill:edit','skill:view','skill:install','skill:rate');

INSERT INTO categories (name, description) VALUES
('自动化脚本', '自动化运维、部署、测试脚本'),
('工作流模板', '流程编排和工作流模板'),
('效率工具', '日常开发效率提升工具'),
('数据处理', '数据清洗、转换、分析工具'),
('监控告警', '监控和告警相关工具');

-- admin user: admin/admin123 (BCrypt)
INSERT INTO users (username, password, email, status) VALUES
('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYN76uO', 'admin@skillops.local', 'ACTIVE');
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
