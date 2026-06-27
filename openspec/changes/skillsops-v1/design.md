# Design: SkillsOps 平台 v1

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    Frontend (Vue3)                   │
│  LoginView │ MarketView │ SkillDetailView │ Workspace│
│   Pinia stores (auth/skill/workspace)                │
└──────────────────┬──────────────────────────────────┘
                   │ HTTP REST + JWT Bearer
┌──────────────────▼──────────────────────────────────┐
│              Spring Boot 2.7.18 (Java 8)             │
│  Filter Chain: JwtFilter → SecurityFilter → MVC      │
│  Controller → Service → Mapper (MyBatis-Plus)        │
│  GlobalExceptionHandler                              │
└──────────────────┬──────────────────────────────────┘
                   │ JDBC
┌──────────────────▼──────────────────────────────────┐
│                MySQL 5.7 (l3_skillsops_02)           │
│              Flyway 6.5.7 migration                  │
└─────────────────────────────────────────────────────┘
```

## Tech Stack (Fixed)

| Component | Version | Constraint |
|-----------|---------|------------|
| Spring Boot | 2.7.18 | Java 8 上限 |
| MyBatis-Plus | 3.5.5 | |
| Flyway | 6.5.7 | MySQL 5.7，**不引入** flyway-mysql |
| jjwt | 0.11.5 | JWT 签发/校验 |
| MySQL Connector/J | 5.1.49 | MySQL 5.7 |
| H2 (test) | 1.4.200 | MySQL 5.7 兼容 |
| Vue | 3.x | |
| Vite | 5.x | |
| Element Plus | 2.x | |
| Pinia | 2.x | |
| Vitest | latest | |
| @vue/test-utils | latest | |

## Database Design

### ER Diagram

```
users ──< user_roles >── roles ──< role_permissions >── permissions
  │
  ├──< skills (author_id)          # 一个用户发布多个 Skill
  ├──< skill_installs (user_id)    # 安装记录
  └──< skill_ratings (user_id)     # 评分评价

skills ──< skill_versions (skill_id)   # 多版本
skills ──< skill_audits (skill_id)     # 审核记录
skills >── categories (category_id)    # 分类
```

### 11 Tables (all with create_time, update_time, create_user, update_user)

```sql
-- 所有表统一审计字段
create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
create_user  VARCHAR(64) NOT NULL
update_user  VARCHAR(64) NOT NULL
```

| # | Table | Key Columns | Indexes |
|---|-------|-------------|---------|
| 1 | `users` | id(PK), username(UK), password, email, status, create_time... | idx_username |
| 2 | `roles` | id(PK), name(UK), create_time... | |
| 3 | `user_roles` | id(PK), user_id(FK), role_id(FK), create_time... | idx_user_id, idx_role_id |
| 4 | `permissions` | id(PK), code(UK), name, create_time... | |
| 5 | `role_permissions` | id(PK), role_id(FK), permission_id(FK), create_time... | idx_role_id |
| 6 | `categories` | id(PK), name(UK), description, create_time... | |
| 7 | `skills` | id(PK), name, description, category_id(FK), author_id(FK), repo_url, doc_url, status, install_count, avg_rating, create_time... | idx_category, idx_author, idx_status |
| 8 | `skill_versions` | id(PK), skill_id(FK), version, changelog, create_time... | idx_skill_id, UNIQUE KEY uk_skill_version (skill_id, version) |
| 9 | `skill_installs` | id(PK), user_id(FK), skill_id(FK), version_id(FK), installed_at, create_time... | uk_user_skill_install (UK: user_id+skill_id) |
| 10 | `skill_ratings` | id(PK), user_id(FK), skill_id(FK), rating(1-5), comment, create_time... | uk_user_skill_rating (UK: user_id+skill_id) |
| 11 | `skill_audits` | id(PK), skill_id(FK), auditor_id(FK), action, reason, create_time... | idx_skill_id |

### Skill Status State Machine

`avg_rating` is updated atomically: `UPDATE skills SET avg_rating = (SELECT AVG(rating) FROM skill_ratings WHERE skill_id = ?) WHERE id = ?`

```
DRAFT ──submit──► PENDING_APPROVAL ──approve──► PUBLISHED
  ▲                    │                            │
  └──── reject ────────┘                      delist│
                                                   ▼
                                               DELISTED
```

## Security Design

### JWT Flow
```
1. POST /api/v1/auth/login → 验证 bcrypt 密码 → 签发 JWT
   Claims: { sub: userId, username, roles: ["USER"], iat, exp: now+6h }
2. 前端 store token in localStorage
3. 每个请求: Authorization: Bearer <token>
4. JwtAuthenticationFilter → 解析 token → 注入 SecurityContext
5. @PreAuthorize("hasRole('ADMIN')") 做接口级鉴权
```

### RBAC Model
- `roles` 表预置: USER, ADMIN
- `permissions` 表预置: skill:create, skill:submit, skill:edit, skill:view, skill:install, skill:rate, skill:approve, skill:reject, skill:delist, admin:categories, admin:stats
- `role_permissions`: ADMIN 拥有全部权限，USER 拥有基础权限

### Password Encryption
- Spring Security BCryptPasswordEncoder
- 注册和登录时使用

### CORS
CORS configured to allow frontend origin (http://localhost:3000) in development.

## API Design

### Unified Response Format
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### Error Codes
| Code | Constant | HTTP Status | Meaning |
|------|----------|-------------|---------|
| 200 | SUCCESS | 200 | 成功 |
| 400 | BAD_REQUEST | 400 | 参数校验失败 |
| 40001 | USERNAME_EXISTS | 400 | 用户名已存在 |
| 40002 | INVALID_STATUS | 400 | Skill 状态不允许当前操作 |
| 401 | UNAUTHORIZED | 401 | 未登录或 token 过期 |
| 403 | FORBIDDEN | 403 | 无权限 |
| 40301 | NOT_AUTHOR | 403 | 非作者无权编辑 |
| 40302 | NOT_ADMIN | 403 | 非管理员无权操作 |
| 404 | NOT_FOUND | 404 | 资源不存在 |
| 409 | CONFLICT | 409 | 重复操作 |
| 500 | INTERNAL_ERROR | 500 | 服务端异常 |

### Full API List

**Auth** (`/api/v1/auth`)
- `POST /login` → LoginResponse
- `POST /register` → Result<Void>
- Logout is client-side only (remove token from localStorage). No server endpoint needed.

**Skills** (`/api/v1/skills`)
- `POST /` → SkillVO (创建草稿)
- `PUT /{id}` → SkillVO (编辑)
- `POST /{id}/submit` → Result<Void> (提交审核)
- `POST /{id}/versions` → SkillVersionVO (发布版本)
- `GET /{id}` → SkillVO (详情)
- `GET /{id}/versions` → PageResult<SkillVersionVO> (版本历史)
- `GET /{id}/ratings` → PageResult<RatingVO> (评价列表)
- `POST /{id}/ratings` → Result<Void> (评分)

**Market** (`/api/v1/market`)
- `GET /skills` → PageResult<SkillVO> (带 filter params)
- `POST /skills/{id}/install` → Result<Void>
- `GET /skills/{id}/install-status` → `{ installed: boolean, rating: { rating, comment } | null }`

**Admin** (`/api/v1/admin`)
- `POST /skills/{id}/approve` → Result<Void>
- `POST /skills/{id}/reject` → Result<Void> (body: { reason })
- `POST /skills/{id}/delist` → Result<Void> (body: { reason })
- `GET /stats` → AdminStatsVO
- `POST /categories` → CategoryVO
- `PUT /categories/{id}` → CategoryVO
- `DELETE /categories/{id}` → Result<Void>
- `GET /pending-skills` → PageResult<SkillVO>

**Workspace** (`/api/v1/workspace`)
- `GET /my-skills` → PageResult<SkillVO>
- `GET /installed` → PageResult<InstallVO>

## Frontend Architecture

### Route Design
```
/login       → LoginView.vue       (public, no auth required for this page)
/market      → MarketView.vue       (auth required)
/skill/:id   → SkillDetailView.vue  (auth required)
/workspace   → WorkspaceView.vue    (auth required, tabs by role)
```

### Component Tree
```
App.vue
├── AppHeader.vue (nav bar: logo, market link, workspace link, user menu)
├── router-view
│   ├── LoginView.vue
│   ├── MarketView.vue
│   │   ├── SkillFilter.vue (category select, keyword input, sort select)
│   │   └── SkillCard.vue × N (name, description, author, rating, installs)
│   ├── SkillDetailView.vue
│   │   ├── SkillInfo.vue (full info + install button)
│   │   ├── VersionList.vue (version history table)
│   │   └── RatingList.vue (ratings + submit form)
│   └── WorkspaceView.vue
│       ├── MySkillsTab.vue (create/edit/submit/version for author)
│       ├── InstalledTab.vue (installed skills list)
│       ├── ReviewTab.vue [ADMIN] (pending approvals)
│       └── StatsTab.vue [ADMIN] (stats dashboard)
```

### Pinia Stores
```
useAuthStore
  state: token, user, isLoggedIn, isAdmin
  actions: login(), register(), logout(), fetchUser()

useSkillStore
  state: marketSkills, currentSkill, filters, pagination
  actions: fetchMarket(), fetchDetail(), install()

useWorkspaceStore
  state: mySkills, installedSkills, pendingSkills, stats
  actions: fetchMySkills(), fetchInstalled(), createSkill(), submitSkill()
```

## Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    Result<Void> handleBusiness(BusinessException e);  // 业务异常

    @ExceptionHandler(MethodArgumentNotValidException.class)
    Result<Void> handleValidation(MethodArgumentNotValidException e);  // 参数校验

    @ExceptionHandler(AccessDeniedException.class)
    Result<Void> handleAccessDenied(AccessDeniedException e);  // 权限

    @ExceptionHandler(Exception.class)
    Result<Void> handleUnknown(Exception e);  // 兜底
}
```

## Testing Strategy

| Layer | Framework | Scope |
|-------|-----------|-------|
| Service | JUnit5 + Mockito + H2 | 业务逻辑、状态机、RBAC |
| Controller | @WebMvcTest + MockMvc | 接口契约、参数校验 |
| Mapper | @MybatisPlusTest + H2 | SQL 正确性 |
| Frontend Unit | Vitest + @vue/test-utils | 组件渲染、store、API mock |
| E2E | @SpringBootTest + H2 (backend) / Vitest + jsdom (frontend) | 核心流程 |

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| 4h 时间不足 | 严格按优先级：核心链路 → 评价 → 管理 → 测试 |
| Flyway 与 MySQL 5.7 兼容 | 锁 6.5.7，不依赖 flyway-mysql |
| RBAC 实现过重 | 精简预置数据，不做动态权限管理页面 |
| 前端 E2E 测试时间 | 仅覆盖核心流程（登录→浏览→安装→评分） |

## Q&A

**Q: 市场是否需要登录？**
A: 是，所有接口都需要 Token，市场也需登录后访问。

**Q: 已下架的 Skill 已安装用户能看到吗？**
A: 能，`/workspace/installed` 显示所有已安装记录，不受 Skill 状态影响。

**Q: 版本号格式？**
A: 作者自由指定，不做格式约束（如 `1.0.0` 或 `v1.0` 均可）。

**Q: 安装计数如何更新？**
A: 每次安装 `skill_installs` 插入记录时，同步更新 `skills.install_count`（事务内）。

**Q: Editing a PUBLISHED skill's metadata — does it change status?**
A: No. Editing name/description/repo_url/doc_url on a PUBLISHED skill keeps it PUBLISHED without re-approval. Only the content fields change, status is unaffected.

**Q: Can a DELISTED skill get new versions? What happens?**
A: Yes. The author can publish a new version for a DELISTED skill. The skill status transitions back to PENDING_APPROVAL and requires re-review.

## Running the Project

### Prerequisites
- Java 8, Maven 3.8+, Node 24+, npm 11+
- MySQL 5.7 running on 127.0.0.1:3306 with database `l3_skillsops_02` created
- `CREATE DATABASE IF NOT EXISTS l3_skillsops_02 DEFAULT CHARSET utf8mb4;`

### Backend
```bash
cd backend
/opt/tools/apache-maven-3.8.6/bin/mvn spring-boot:run
# Server starts on :8080, Flyway auto-migrates on startup
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Dev server on :3000, proxies /api to :8080
```

### Default Admin
- Username: admin, Password: admin123
