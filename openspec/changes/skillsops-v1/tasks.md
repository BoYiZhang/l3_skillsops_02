# SkillsOps 平台 v1 — 实现计划

> **For agentic workers:** 使用 superpowers:subagent-driven-development 逐任务执行。每步一个 checkbox，小步提交。

**Goal:** 构建完整的 Skills 运营平台：用户认证 → Skill 发布/审核/上架 → 市场浏览/安装 → 评分评价 → 管理员运营。

**Architecture:** Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 后端，Vue3 + Vite + Element Plus + Pinia 前端，MySQL 5.7 + Flyway 6.5.7 数据层，JWT 认证，RBAC 权限。

**Tech Stack:** Java 8, Spring Boot 2.7.18, MyBatis-Plus 3.5.5, Flyway 6.5.7, jjwt 0.11.5, MySQL 5.7, H2 1.4.200 (test), Vue3, Vite, Element Plus, Pinia, Vitest, JUnit5

## Global Constraints

- Java 8 source/target
- MySQL 5.7, Flyway 6.5.7 (NO flyway-mysql)
- H2 1.4.200 for tests
- JWT expiration: 6 hours
- ALL tables have: create_time, update_time, create_user, update_user
- ALL HTTP requests require valid JWT token (except /api/v1/auth/login, /api/v1/auth/register)
- Password: BCrypt encrypted
- API prefix: /api/v1/
- Response format: Result<T> { code, message, data }
- Git repo: git@github.com:BoYiZhang/l3_skillsops_02.git, branch: feature/dev
- Backend package: com.boyi.skillops
- All SQL scripts in Flyway migration only (no standalone SQL files in doc/)

---

## Phase 1: Project Scaffolding & Git Setup

### Task 1.1: Git init & remote setup

**Files:** (none — git metadata)

- [ ] **Step 1: Init git repo**
```bash
cd /opt/l3_skillsops_02
git init
git checkout -b feature/dev
```

- [ ] **Step 2: Add .gitignore**
```gitignore
### Java ###
**/target/
*.class
*.jar
*.war
*.log
.idea/
*.iml
.settings/
.project
.classpath

### Node ###
node_modules/
dist/
.env.local
*.local

### OS ###
.DS_Store
Thumbs.db

### Temp ###
*.swp
*.swo
*~
```

- [ ] **Step 3: Add remote and commit**
```bash
git add .gitignore
git commit -m "chore: init repo with .gitignore"
git remote add origin git@github.com:BoYiZhang/l3_skillsops_02.git
```

### Task 1.2: Backend Spring Boot scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/boyi/skillops/SkillOpsApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Write pom.xml with all dependencies**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>
    <groupId>com.boyi</groupId>
    <artifactId>skillops</artifactId>
    <version>1.0.0</version>
    <name>skillsops</name>

    <properties>
        <java.version>1.8</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <flyway.version>6.5.7</flyway.version>
        <jjwt.version>0.11.5</jjwt.version>
        <h2.version>1.4.200</h2.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.49</version>
        </dependency>

        <!-- Flyway (NO flyway-mysql module) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>${flyway.version}</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>${h2.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write main application class**
```java
package com.boyi.skillops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SkillOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkillOpsApplication.class, args);
    }
}
```

- [ ] **Step 3: Write application.yml**
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/l3_skillsops_02?useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password:
    driver-class-name: com.mysql.jdbc.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

mybatis-plus:
  # XML mapper location (unused — all queries use annotation-based MyBatis-Plus)
  # mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.boyi.skillops.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

app:
  jwt:
    secret: c2tpbGxvcHMtand0LXNlY3JldC1rZXktMjAyNi1wbGVhc2UtY2hhbmdlLWluLXByb2R1Y3Rpb24=
    expiration-hours: 6
```

- [ ] **Step 4: Verify build**
```bash
cd /opt/l3_skillsops_02/backend
/opt/tools/apache-maven-3.8.6/bin/mvn compile
```

- [ ] **Step 5: Commit**
```bash
git add backend/
git commit -m "chore: init Spring Boot 2.7.18 backend scaffold"
```

### Task 1.3: Frontend Vite + Vue3 scaffold

**Files:**
- Create: `frontend/package.json`, `frontend/vite.config.js`, `frontend/index.html`
- Create: `frontend/src/main.js`, `frontend/src/App.vue`
- Create: `frontend/src/router/index.js`, `frontend/src/stores/`

- [ ] **Step 1: Write package.json**
```json
{
  "name": "skillsops-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest",
    "test:run": "vitest run"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.7.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.4.0",
    "vitest": "^1.6.0",
    "@vue/test-utils": "^2.4.0",
    "jsdom": "^24.0.0"
  }
}
```

- [ ] **Step 2: Write vite.config.js**
```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Write index.html, main.js, App.vue** (minimal scaffold)
- [ ] **Step 4: Install deps and verify**
```bash
cd /opt/l3_skillsops_02/frontend
npm install
npm run dev -- --host 0.0.0.0  # quick check, then Ctrl+C
```

- [ ] **Step 5: Commit**
```bash
git add frontend/
git commit -m "chore: init Vue3 + Vite + Element Plus frontend scaffold"
```

---

## Phase 2: Database & Entity Layer

### Task 2.1: Flyway V1 — init schema (DDL)

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: Write V1__init_schema.sql** (all 11 tables with audit fields)
```sql
-- ============================================
-- SkillsOps v1.0.0 Initial Schema
-- ============================================

-- Users & RBAC
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Categories
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Skills
CREATE TABLE skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT DEFAULT NULL,
    category_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    repo_url VARCHAR(512) DEFAULT NULL,
    doc_url VARCHAR(512) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    install_count BIGINT NOT NULL DEFAULT 0,
    avg_rating DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    INDEX idx_category (category_id),
    INDEX idx_author (author_id),
    INDEX idx_status (status),
    INDEX idx_install_count (install_count),
    INDEX idx_avg_rating (avg_rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skill_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    version VARCHAR(32) NOT NULL,
    changelog TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    INDEX idx_skill_id (skill_id),
    UNIQUE KEY uk_skill_version (skill_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skill_installs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_user_skill_install (user_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skill_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    UNIQUE KEY uk_user_skill_rating (user_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skill_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    auditor_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    update_user VARCHAR(64) NOT NULL DEFAULT 'system',
    INDEX idx_skill_id (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Commit**
```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat: add Flyway V1 init schema — 11 tables with audit fields"
```

### Task 2.2: Flyway V2 — seed data

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__seed_data.sql`

- [ ] **Step 1: Write V2__seed_data.sql**
```sql
-- Seed roles
INSERT INTO roles (name) VALUES ('ADMIN');
INSERT INTO roles (name) VALUES ('USER');

-- Seed permissions
INSERT INTO permissions (code, name) VALUES
('skill:create', '创建Skill'),
('skill:submit', '提交审核'),
('skill:edit', '编辑Skill'),
('skill:view', '查看Skill'),
('skill:approve', '审核通过'),
('skill:reject', '审核拒绝'),
('skill:delist', '下架Skill'),
('admin:categories', '管理分类'),
('admin:stats', '查看统计');

-- Assign all permissions to ADMIN (role_id=1)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- Assign basic permissions to USER (role_id=2)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE code IN ('skill:create', 'skill:submit', 'skill:edit', 'skill:view');

-- Seed categories
INSERT INTO categories (name, description) VALUES
('自动化脚本', '自动化运维、部署、测试脚本'),
('工作流模板', '流程编排和工作流模板'),
('效率工具', '日常开发效率提升工具'),
('数据处理', '数据清洗、转换、分析工具'),
('监控告警', '监控和告警相关工具');

-- Seed admin user (password: admin123, bcrypt encoded)
-- BCrypt hash for 'admin123'
INSERT INTO users (username, password, email, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'admin@skillops.local', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
```

- [ ] **Step 2: Commit**
```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat: add Flyway V2 seed data — roles, permissions, categories, admin user"
```

### Task 2.3: Entity classes

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/entity/User.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/Role.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/Permission.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/UserRole.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/RolePermission.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/Category.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/Skill.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/SkillVersion.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/SkillInstall.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/SkillRating.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/SkillAudit.java`
- Create: `backend/src/main/java/com/boyi/skillops/entity/BaseEntity.java` (audit fields base class)

- [ ] **Step 1: Write BaseEntity.java**
```java
package com.boyi.skillops.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUser;
}
```

- [ ] **Step 2a: Write RBAC entity classes** extending BaseEntity with `@TableName`, `@TableId`, Lombok `@Data`
```java
// User.java
@TableName("users")
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String email;
    private String status;
}

// Role.java
@TableName("roles")
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
}

// Permission.java
@TableName("permissions")
@Data
@EqualsAndHashCode(callSuper = true)
public class Permission extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
}

// UserRole.java
@TableName("user_roles")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRole extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
}

// RolePermission.java
@TableName("role_permissions")
@Data
@EqualsAndHashCode(callSuper = true)
public class RolePermission extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long permissionId;
}
```

- [ ] **Step 2b: Write Skill domain entity classes** extending BaseEntity with `@TableName`, `@TableId`, Lombok `@Data`
```java
// Skill.java
@TableName("skills")
@Data
@EqualsAndHashCode(callSuper = true)
public class Skill extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private Long authorId;
    private String repoUrl;
    private String docUrl;
    private String status;
    private Long installCount;
    private Double avgRating;
}

// SkillVersion.java
@TableName("skill_versions")
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillVersion extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skillId;
    private String version;
    private String changelog;
}

// SkillInstall.java
@TableName("skill_installs")
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillInstall extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skillId;
    private Long versionId;
}

// SkillRating.java
@TableName("skill_ratings")
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillRating extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skillId;
    private Integer rating;
    private String comment;
}

// SkillAudit.java
@TableName("skill_audits")
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillAudit extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skillId;
    private Long auditorId;
    private String action;
    private String reason;
}
```

- [ ] **Step 2c: Write Category entity class**
```java
// Category.java
@TableName("categories")
@Data
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
}
```
- [ ] **Step 3: Write all Mapper interfaces** extending `BaseMapper<T>` with `@Mapper`
- [ ] **Step 4: MyBatis-Plus auto-fill handler**
  - Create: `backend/src/main/java/com/boyi/skillops/config/MyMetaObjectHandler.java`
- [ ] **Step 5: Commit**
```bash
git add backend/src/main/java/com/boyi/skillops/entity/ backend/src/main/java/com/boyi/skillops/mapper/ backend/src/main/java/com/boyi/skillops/config/
git commit -m "feat: add entity classes, mappers, and auto-fill config"
```

---

## Phase 3: Security & Auth

### Task 3.1: JWT utilities & Spring Security config

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/security/JwtTokenProvider.java`
- Create: `backend/src/main/java/com/boyi/skillops/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/boyi/skillops/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/boyi/skillops/security/UserDetailsServiceImpl.java`

- [ ] **Step 1: Write JwtTokenProvider**
```java
package com.boyi.skillops.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {
    private final Key key;
    private final int expirationHours;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-hours}") int expirationHours) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationHours = expirationHours;
    }

    public String generateToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationHours * 3600000L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", String.join(",", roles))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return Long.valueOf(claims.getSubject());
    }
}
```

- [ ] **Step 2: Write JwtAuthenticationFilter**
  - extends `OncePerRequestFilter`
  - Extract `Authorization: Bearer <token>` header
  - Validate token, create `UsernamePasswordAuthenticationToken`
  - Set `SecurityContextHolder`

- [ ] **Step 3: Write SecurityConfig**
  - Disable CSRF, stateless session
  - Permit `/api/v1/auth/login`, `/api/v1/auth/register`
  - All other `/api/**` require authentication
  - Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter

- [ ] **Step 3a: Add CORS configuration in SecurityConfig**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

- [ ] **Step 4: Write UserDetailsServiceImpl**
  - Load user + roles from database

- [ ] **Step 5: Commit**

### Task 3.2: Common utilities (Result, ErrorCode, Exception)

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/common/Result.java`
- Create: `backend/src/main/java/com/boyi/skillops/enums/ErrorCode.java`
- Create: `backend/src/main/java/com/boyi/skillops/exception/BusinessException.java`
- Create: `backend/src/main/java/com/boyi/skillops/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Write ErrorCode enum**
```java
package com.boyi.skillops.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数校验失败"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    INVALID_STATUS(40002, "当前状态不允许此操作"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_AUTHOR(40301, "非作者无权操作"),
    NOT_ADMIN(40302, "非管理员无权操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突"),
    INTERNAL_ERROR(500, "服务端异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Write Result.java**
```java
package com.boyi.skillops.common;

import com.boyi.skillops.enums.ErrorCode;
import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() { return success(null); }

    public static <T> Result<T> error(ErrorCode errorCode) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        return r;
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 3: Write BusinessException and GlobalExceptionHandler**
- [ ] **Step 4: Commit**

### Task 3.3: Auth API (register, login, logout)

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/controller/AuthController.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/UserService.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/impl/UserServiceImpl.java`
- Create: `backend/src/main/java/com/boyi/skillops/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/boyi/skillops/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/boyi/skillops/vo/LoginResponse.java`

- [ ] **Step 1: Write DTOs**
```java
// LoginRequest.java
package com.boyi.skillops.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}

// RegisterRequest.java
package com.boyi.skillops.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 64)
    private String username;
    @NotBlank @Size(min = 6, max = 128)
    private String password;
    private String email;
}

// LoginResponse.java
package com.boyi.skillops.vo;
import lombok.Data;
import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private List<String> roles;
}
```

- [ ] **Step 2: Write AuthController**
  - `POST /api/v1/auth/register` — params validated, bcrypt password, assign USER role, return success
  - `POST /api/v1/auth/login` — authenticate, return JWT + user info
  - `POST /api/v1/auth/logout` — (stateless, just return success; client removes token)

- [ ] **Step 3: Write UserService/Impl**
  - `register(RegisterRequest)` — check username unique, bcrypt encode, save user + user_role
  - `login(LoginRequest)` — verify credentials, generate JWT

- [ ] **Step 4: Test manually with curl**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"123456","email":"test@test.com"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"123456"}'
```

- [ ] **Step 5: Commit**
```bash
git add backend/src/main/java/com/boyi/skillops/controller/ backend/src/main/java/com/boyi/skillops/dto/ backend/src/main/java/com/boyi/skillops/vo/ backend/src/main/java/com/boyi/skillops/service/
git commit -m "feat: implement auth API — register, login with JWT, logout"
```

---

## Phase 4: Core Business — Skill Management

### Task 4.1: Skill CRUD & lifecycle service

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/service/SkillService.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/impl/SkillServiceImpl.java`
- Create: `backend/src/main/java/com/boyi/skillops/dto/SkillCreateRequest.java`
- Create: `backend/src/main/java/com/boyi/skillops/vo/SkillVO.java`

- [ ] **Step 1: Write DTO and VO**
```java
// SkillCreateRequest.java
@Data
public class SkillCreateRequest {
    @NotBlank private String name;
    @NotBlank private String description;
    @NotNull private Long categoryId;
    private String repoUrl;
    private String docUrl;
}

// SkillVO.java
@Data
public class SkillVO {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private String repoUrl;
    private String docUrl;
    private String status;        // DRAFT, PENDING_APPROVAL, PUBLISHED, DELISTED
    private Long installCount;
    private Double avgRating;
    private String latestVersion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

// VersionCreateRequest.java
@Data
public class VersionCreateRequest {
    @NotBlank(message = "版本号不能为空")
    private String version;
    private String changelog;
}

// VersionVO.java
@Data
public class VersionVO {
    private Long id;
    private Long skillId;
    private String version;
    private String changelog;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: Write SkillService interface**
```java
public interface SkillService {
    SkillVO create(SkillCreateRequest request, Long authorId);
    SkillVO update(Long skillId, SkillCreateRequest request, Long userId);
    SkillVO getById(Long skillId);
    void submitForApproval(Long skillId, Long userId);
    VersionVO publishVersion(Long skillId, VersionCreateRequest request, Long userId);
    PageResult<SkillVO> getMySkills(Long userId, Page page);
    // ... more methods added in later tasks
}
```

- [ ] **Step 3a: Write SkillServiceImpl — basic CRUD**
  - `create`: validate category exists, set status=DRAFT, author=currentUser, save via mapper
  - `update`: check author==currentUser or isAdmin, update fields (name, description, category, urls)
  - `getById`: query skill with MyBatis-Plus, populate categoryName and authorName via join, return SkillVO

- [ ] **Step 3b: Write SkillServiceImpl — submitForApproval with state machine**
  - `submitForApproval`: load skill, check status==DRAFT (throw INVALID_STATUS otherwise), check author==currentUser or isAdmin, set status=PENDING_APPROVAL, save
  - State machine validation: DRAFT→PENDING_APPROVAL, PENDING_APPROVAL→PUBLISHED/DRAFT, PUBLISHED→DELISTED

- [ ] **Step 3c: Write SkillServiceImpl — publishVersion + getVersions**
  - `publishVersion`: check skill status==PUBLISHED, validate version not duplicate, create SkillVersion record
  - `getVersions`: query skill_versions by skillId, ordered by create_time desc
  - `getMySkills`: paginated query for skills where authorId==currentUser, with category join

- [ ] **Step 4: Write SkillController**
  - `POST /api/v1/skills` → create
  - `PUT /api/v1/skills/{id}` → update
  - `GET /api/v1/skills/{id}` → detail
  - `POST /api/v1/skills/{id}/submit` → submit
  - `POST /api/v1/skills/{id}/versions` → publish version
  - `GET /api/v1/skills/{id}/versions` → version history

- [ ] **Step 5: Commit**

---

## Phase 5: Market & Install

### Task 5.1: Market browsing & install

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/controller/MarketController.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/MarketService.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/impl/MarketServiceImpl.java`
- Create: `backend/src/main/java/com/boyi/skillops/dto/MarketQueryRequest.java`
- Create: `backend/src/main/java/com/boyi/skillops/common/PageResult.java`

- [ ] **Step 1: Write PageResult.java**
```java
package com.boyi.skillops.common;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.records = page.getRecords();
        r.total = page.getTotal();
        r.size = page.getSize();
        r.current = page.getCurrent();
        return r;
    }
}
```

- [ ] **Step 2: Write MarketQueryRequest**
```java
@Data
public class MarketQueryRequest {
    private Long categoryId;
    private String keyword;
    private String sortBy;  // HOT (install count), RATING (avg_rating), NEWEST (create_time)
    private long page = 1;
    private long size = 12;
}
```

- [ ] **Step 3: Write MarketController**
  - `GET /api/v1/market/skills` — query only PUBLISHED skills, filter/sort/paginate
  - `POST /api/v1/market/skills/{id}/install` — check not already installed, create install record, increment skill.install_count (transactional)
  - `GET /api/v1/market/skills/{id}/install-status` — return `{ installed: boolean, rating: { rating: Int, comment: String } | null }` for current user

- [ ] **Step 4: Write MarketServiceImpl**
  - `queryMarket`: MyBatis-Plus QueryWrapper/LambdaQueryWrapper, dynamic filter + sort
  - `install`: validate skill exists and PUBLISHED, check duplicate install, use @Transactional
  - Find latest version for the skill

- [ ] **Step 5: Commit**

---

## Phase 6: Rating System

### Task 6.1: Rating & review API

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/controller/RatingController.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/RatingService.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/impl/RatingServiceImpl.java`
- Create: `backend/src/main/java/com/boyi/skillops/dto/RatingRequest.java`
- Create: `backend/src/main/java/com/boyi/skillops/vo/RatingVO.java`

- [ ] **Step 1: Write RatingRequest and RatingVO**
```java
// RatingRequest.java
@Data
public class RatingRequest {
    @NotNull @Min(1) @Max(5)
    private Integer rating;
    private String comment;
}

// RatingVO.java
@Data
public class RatingVO {
    private Long id;
    private Long userId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: Write RatingController**
  - `POST /api/v1/skills/{id}/ratings` — first-time rating (insert)
  - `PUT /api/v1/skills/{id}/ratings` — update existing rating (modification)
  - `GET /api/v1/skills/{id}/ratings` — list ratings (paginated)

- [ ] **Step 3: Write RatingServiceImpl**
  - `rate`: check user has installed this skill (required!), upsert rating (INSERT ... ON DUPLICATE KEY UPDATE pattern or check-first-then-save)
  - After save: recalculate avg_rating for the skill
  - `getRatings`: paginated list with username join

- [ ] **Step 4: Commit**

---

## Phase 7: Admin Operations

### Task 7.1: Admin API — approve, reject, delist, stats, categories

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/controller/AdminController.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/AdminService.java`
- Create: `backend/src/main/java/com/boyi/skillops/service/impl/AdminServiceImpl.java`
- Create: `backend/src/main/java/com/boyi/skillops/vo/AdminStatsVO.java`
- Create: `backend/src/main/java/com/boyi/skillops/dto/AuditRequest.java`

- [ ] **Step 1: Write AuditRequest and AdminStatsVO**
```java
// AuditRequest.java
@Data
public class AuditRequest {
    @NotBlank private String action;  // APPROVE, REJECT, DELIST
    private String reason;
}

// AdminStatsVO.java
@Data
public class AdminStatsVO {
    private long totalSkills;
    private long totalUsers;
    private long totalInstalls;
    private List<TrendItem> installTrend;  // last 30 days
    private List<AuthorStat> topAuthors;
    private List<SkillVO> topSkills;

    @Data
    public static class TrendItem {
        private String date;
        private long count;
    }
    @Data
    public static class AuthorStat {
        private Long authorId;
        private String authorName;
        private long skillCount;
    }
}
```

- [ ] **Step 2: Write AdminController** (all protected by `@PreAuthorize("hasRole('ADMIN')")`)
  - `POST /api/v1/admin/skills/{id}/approve`
  - `POST /api/v1/admin/skills/{id}/reject` (body: reason)
  - `POST /api/v1/admin/skills/{id}/delist` (body: reason)
  - `GET /api/v1/admin/pending-skills` — skills with PENDING_APPROVAL status
  - `GET /api/v1/admin/stats` — aggregated statistics
  - `POST /api/v1/admin/categories` — create category
  - `PUT /api/v1/admin/categories/{id}` — update category
  - `DELETE /api/v1/admin/categories/{id}` — delete category

- [ ] **Step 3: Write AdminServiceImpl**
  - `approve`: validate status=PENDING_APPROVAL, update to PUBLISHED, create audit record
  - `reject`: validate status=PENDING_APPROVAL, update to DRAFT, create audit record with reason
  - `delist`: validate status=PUBLISHED, update to DELISTED, create audit record
  - `getStats`: aggregate queries from count tables
  - Category management: CRUD

- [ ] **Step 4: Commit**

---

## Phase 8: Workspace API

### Task 8.1: Workspace controller

**Files:**
- Create: `backend/src/main/java/com/boyi/skillops/controller/WorkspaceController.java`
- Create: `backend/src/main/java/com/boyi/skillops/vo/InstallVO.java`

- [ ] **Step 1: Write InstallVO**
```java
@Data
public class InstallVO {
    private Long id;
    private Long skillId;
    private String skillName;
    private String skillDescription;
    private String version;
    private String status;  // current skill status
    private LocalDateTime installedAt;
}
```

- [ ] **Step 2: Write WorkspaceController**
  - `GET /api/v1/workspace/my-skills` — user's authored skills (all statuses)
  - `GET /api/v1/workspace/installed` — user's installed skills

- [ ] **Step 3: Commit**

---

## Phase 9: Frontend Implementation

### Task 9.1: Router, stores, axios setup

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/auth.js`
- Create: `frontend/src/stores/skill.js`
- Create: `frontend/src/stores/workspace.js`
- Create: `frontend/src/utils/request.js` (axios interceptor)

- [ ] **Step 1: Write request.js with axios interceptor**
  - baseURL: `/api/v1`
  - Request interceptor: attach `Authorization: Bearer <token>` from localStorage
  - Response interceptor: on 401 → redirect to /login

- [ ] **Step 2: Write router/index.js**
  - `/login`, `/market`, `/skill/:id`, `/workspace`
  - Navigation guard: check token, redirect to /login if absent (except /login itself)

- [ ] **Step 3: Write auth store**
  - state: { token, user, roles, isLoggedIn, isAdmin }
  - actions: login, register, logout
  - persist token to localStorage

- [ ] **Step 4: Write skill store and workspace store (skeleton)**

- [ ] **Step 5: Commit**

### Task 9.2: LoginView + AppHeader

**Files:**
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/components/layout/AppHeader.vue`

- [ ] **Step 1: Write LoginView.vue**
  - Login form: username + password + submit
  - Register form: username + password + email + submit
  - Toggle between login/register mode
  - On success: store token, redirect to /market
  - Element Plus el-form with validation

- [ ] **Step 2: Write AppHeader.vue**
  - Logo/title → links to /market
  - Nav: 市场, 工作台
  - User dropdown: username display, 退出
  - Show on all pages except /login

- [ ] **Step 3: Commit**

### Task 9.3: MarketView + SkillCard + SkillFilter

**Files:**
- Create: `frontend/src/views/MarketView.vue`
- Create: `frontend/src/components/skill/SkillCard.vue`
- Create: `frontend/src/components/skill/SkillFilter.vue`

- [ ] **Step 1: Write SkillFilter.vue**
  - Category dropdown (fetch from API)
  - Keyword search input
  - Sort select: 热门(HOT), 最新(NEWEST), 评分(RATING)
  - Search button

- [ ] **Step 2: Write SkillCard.vue**
  - Props: skill object
  - Display: name, description (truncated), category tag, author, avg_rating (stars), install_count, latest version
  - Click → navigate to /skill/:id

- [ ] **Step 3: Write MarketView.vue**
  - SkillFilter on top
  - Grid/List of SkillCard
  - el-pagination
  - Fetch skills from market API on mount and filter change

- [ ] **Step 4: Commit**

### Task 9.4: SkillDetailView + SkillInfo + VersionList + RatingList

**Files:**
- Create: `frontend/src/views/SkillDetailView.vue`
- Create: `frontend/src/components/skill/SkillInfo.vue`
- Create: `frontend/src/components/skill/VersionList.vue`
- Create: `frontend/src/components/skill/RatingList.vue`

- [ ] **Step 1: Write SkillInfo.vue**
  - Full skill info display
  - Install button (calls install API, shows success message, disables if already installed)
  - Admin: show approve/reject/delist buttons if PENDING_APPROVAL or PUBLISHED
  - Author: show edit button

- [ ] **Step 2: Write VersionList.vue**
  - Table: version, changelog, publish date
  - Paginated

- [ ] **Step 3: Write RatingList.vue**
  - List of ratings: username, star rating, comment, date
  - Submit/edit form for current user (if installed): 1-5 star picker + comment textarea
  - Paginated

- [ ] **Step 4: Write SkillDetailView.vue**
  - Fetch skill detail by route param :id
  - Layout: SkillInfo → VersionList → RatingList

- [ ] **Step 5: Commit**

### Task 9.5: WorkspaceView with tabs

**Files:**
- Create: `frontend/src/views/WorkspaceView.vue`
- Create: `frontend/src/components/workspace/MySkillsTab.vue`
- Create: `frontend/src/components/workspace/InstalledTab.vue`
- Create: `frontend/src/components/workspace/ReviewTab.vue`
- Create: `frontend/src/components/workspace/StatsTab.vue`

- [ ] **Step 1: Write WorkspaceView.vue**
  - el-tabs: "我的发布", "我的安装", (ADMIN)"待审核", (ADMIN)"运营统计"
  - Switch tab content based on role

- [ ] **Step 2: Write MySkillsTab.vue**
  - List user's skills with status badge
  - "创建 Skill" button → dialog form (name, description, category, repo/doc url)
  - "编辑" button → edit dialog
  - "提交审核" button (for DRAFT status)
  - "发布新版本" button (for PUBLISHED) → dialog (version, changelog)

- [ ] **Step 3: Write InstalledTab.vue**
  - List installed skills with name, version, status
  - Click to navigate to /skill/:id

- [ ] **Step 4: Write ReviewTab.vue** (ADMIN only)
  - List PENDING_APPROVAL skills
  - "通过" / "拒绝(附理由)" actions

- [ ] **Step 5: Write StatsTab.vue** (ADMIN only)
  - Stat cards: total skills, total users, total installs
  - Simple trend chart or table

- [ ] **Step 6: Commit**

---

## Phase 10: Testing

### Task 10.1: Backend service tests

**Files:**
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/test/java/com/boyi/skillops/service/UserServiceTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/service/SkillServiceTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/service/MarketServiceTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/service/RatingServiceTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/service/AdminServiceTest.java`

- [ ] **Step 1: Write application-test.yml**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true
    driver-class-name: org.h2.Driver
    username: sa
    password:
  flyway:
    enabled: false
  sql:
    init:
      mode: always
      schema-locations: classpath:db/migration/V1__init_schema.sql
      data-locations: classpath:db/migration/V2__seed_data.sql
  h2:
    console:
      enabled: true
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```
- [ ] **Step 2: Write tests with @SpringBootTest + @Transactional, test data setup, assertion on Result**
- [ ] **Step 3: Run tests**
```bash
cd /opt/l3_skillsops_02/backend
/opt/tools/apache-maven-3.8.6/bin/mvn test
```

- [ ] **Step 4: Commit**

### Task 10.2: Backend controller tests

**Files:**
- Create: `backend/src/test/java/com/boyi/skillops/controller/AuthControllerTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/controller/SkillControllerTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/controller/MarketControllerTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/controller/AdminControllerTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/controller/RatingControllerTest.java`
- Create: `backend/src/test/java/com/boyi/skillops/controller/WorkspaceControllerTest.java`

- [ ] **Step 1: Write @WebMvcTest tests with MockMvc + @MockBean services**
- [ ] **Step 2: Run and commit**

### Task 10.3: Frontend unit tests

**Files:**
- Create: `frontend/src/__tests__/stores/auth.test.js`
- Create: `frontend/src/__tests__/components/SkillCard.test.js`
- Create: `frontend/src/__tests__/components/SkillFilter.test.js`
- Create: `frontend/src/__tests__/views/LoginView.test.js`

- [ ] **Step 1: Write store tests (Pinia + vitest)**
- [ ] **Step 2: Write component tests (@vue/test-utils + jsdom)**
- [ ] **Step 3: Run tests**
```bash
cd /opt/l3_skillsops_02/frontend
npm run test:run
```

- [ ] **Step 4: Commit**

### Task 10.4: E2E integration tests

**Files:**
- Create: `backend/src/test/java/com/boyi/skillops/e2e/SkillsOpsE2ETest.java`
- Create: `frontend/src/__tests__/e2e/core-flow.test.js`

- [ ] **Step 1: Backend E2E — full flow test with @SpringBootTest + TestRestTemplate**
  - Register user → Login → Create skill → Submit → Admin approve → Market query → Install → Rate
- [ ] **Step 2: Frontend E2E — mount app, simulate core flow**
- [ ] **Step 3: Commit**

---

## Phase 11: Documentation & Screenshots

### Task 11.1: Screenshots & doc

**Files:**
- Create: `doc/screenshots/` (images)

- [ ] **Step 1: Start backend + frontend**
```bash
# Terminal 1
cd /opt/l3_skillsops_02/backend
/opt/tools/apache-maven-3.8.6/bin/mvn spring-boot:run

# Terminal 2
cd /opt/l3_skillsops_02/frontend
npm run dev
```

- [ ] **Step 2: Capture screenshots of all core pages (login, market, detail, workspace)**
- [ ] **Step 3: Write doc/README.md with project overview, setup instructions**
- [ ] **Step 4: Commit**

---

## Execution Order (by priority)

**Must complete:**
1. Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 9.1-9.4

**Secondary (do as time allows):**
2. Phase 6 → Phase 7 → Phase 8 → Phase 9.5 → Phase 10 → Phase 11

**Core flow verification:** Register → Login → Create Skill → Submit → Admin Approve → Browse Market → Install → Rate
