# Proposal: SkillsOps 平台 v1

## Why

团队内部积累了大量可复用 Skills，散落在各人电脑和仓库中，缺乏统一的发现、评价和运营机制。需要构建一个 Skills 运营平台。

## What Changes

从零构建完整的 Skills 运营平台，包含：

- **用户认证**：注册/登录/JWT Token（6h 过期），所有接口需 Token
- **Skill 发布与管理**：创建→提交审核→上架→版本发布 全生命周期
- **Skill 市场**：支持分类/关键词/排序/分页浏览，一键安装
- **评价体系**：每人每 Skill 一次评分(1-5) + 评价，允许修改
- **运营管理**：审核上架/下架、分类管理、运营统计

## Capabilities

| Capability | 描述 |
|------------|------|
| `auth` | 用户注册、登录、退出，JWT Token 认证 |
| `skill-management` | Skill CRUD、生命周期、版本管理 |
| `skill-market` | 市场浏览、搜索筛选、安装 |
| `rating` | 评分与评价系统 |
| `admin-operations` | 审核、下架、分类管理、统计 |
| `workspace` | 个人工作台（我的发布/安装） |

## Impact

- 新建项目，无存量系统兼容性影响
- 数据库 l3_skillsops_01，11 张表
- 前后端分离，RESTful API
