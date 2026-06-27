# SkillsOps — Skills 运营平台

团队内部 Skills 发现、安装、评价和运营管理平台。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 2.7.18 + Java 8 + MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 5.7 + Flyway 6.5.7 |
| 认证 | JWT (jjwt 0.11.5) + BCrypt + RBAC |
| 前端 | Vue 3 + Vite + Element Plus + Pinia |
| 测试 | JUnit 5 + H2 + Spring Boot Test / Vitest |

## 功能

- **用户认证**：注册/登录/JWT Token（6h 过期）
- **Skill 管理**：创建→审核→上架→版本发布，完整生命周期
- **Skill 市场**：分类/关键词搜索、按热门/评分/最新排序、分页
- **安装**：一键安装，记录安装人到具体版本
- **评价体系**：每人每 Skill 一次评分(1-5)，可修改，实时平均分
- **管理员**：审核上架/拒绝(附理由)、下架、分类管理、运营统计
- **工作台**：我发布的/我安装的/待审核/运营统计

## 快速启动

### 环境要求
- Java 8 + Maven 3.8+
- Node 24 + npm 11+
- MySQL 5.7 (127.0.0.1:3306, root/无密码)
- 数据库: `l3_skillsops_01`

### 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS l3_skillsops_01 DEFAULT CHARSET utf8mb4;
```

### 启动后端
```bash
cd backend
/opt/tools/apache-maven-3.8.6/bin/mvn spring-boot:run
# 启动在 http://localhost:8080
# Flyway 自动建表，Admin 自动初始化
```

### 启动前端
```bash
cd frontend
npm install
npm run dev
# 启动在 http://localhost:3000
# 自动代理 /api 到 :8080
```

### 默认账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | 自行注册 | — |

## 运行测试
```bash
# 后端测试 (H2 内存数据库)
cd backend
/opt/tools/apache-maven-3.8.6/bin/mvn test

# 前端测试
cd frontend
npm run test:run
```

## 项目结构
```
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/boyi/skillops/
│   │   ├── config/                   # MyBatis-Plus、Flyway 配置
│   │   ├── controller/               # REST 控制器 (6个)
│   │   ├── service/                  # 业务服务 (6个)
│   │   ├── mapper/                   # MyBatis Mapper (11个)
│   │   ├── entity/                   # 数据库实体 (12个)
│   │   ├── dto/                      # 请求 DTO (6个)
│   │   ├── vo/                       # 响应 VO (7个)
│   │   ├── enums/                    # 枚举
│   │   ├── security/                 # JWT、Spring Security
│   │   ├── exception/                # 全局异常处理
│   │   └── common/                   # Result、PageResult
│   └── src/main/resources/db/migration/
│       ├── V1__init_schema.sql       # 11张表 DDL
│       └── V2__seed_data.sql         # 种子数据
├── frontend/                         # Vue3 前端
│   └── src/
│       ├── views/                    # 页面 (4个)
│       ├── components/               # 组件 (10个)
│       ├── stores/                   # Pinia 状态管理 (3个)
│       ├── router/                   # 路由
│       └── utils/                    # Axios 封装
├── openspec/changes/skillsops-v1/    # OpenSpec 设计文档
├── doc/                              # 文档与截图
└── .gitignore
```

## API 概览

| 模块 | 端点 | 说明 |
|------|------|------|
| Auth | POST /api/v1/auth/register | 注册 |
| Auth | POST /api/v1/auth/login | 登录 |
| Skills | POST /api/v1/skills | 创建 Skill |
| Skills | PUT /api/v1/skills/{id} | 编辑 |
| Skills | POST /api/v1/skills/{id}/submit | 提交审核 |
| Skills | POST /api/v1/skills/{id}/versions | 发布版本 |
| Skills | GET /api/v1/skills/{id} | 详情 |
| Market | GET /api/v1/market/skills | 市场列表 |
| Market | POST /api/v1/market/skills/{id}/install | 安装 |
| Ratings | POST/PUT /api/v1/skills/{id}/ratings | 评分 |
| Admin | POST /api/v1/admin/skills/{id}/approve | 审核通过 |
| Admin | POST /api/v1/admin/skills/{id}/reject | 审核拒绝 |
| Admin | POST /api/v1/admin/skills/{id}/delist | 下架 |
| Admin | GET /api/v1/admin/stats | 运营统计 |
| Workspace | GET /api/v1/workspace/my-skills | 我的发布 |
| Workspace | GET /api/v1/workspace/installed | 我的安装 |
