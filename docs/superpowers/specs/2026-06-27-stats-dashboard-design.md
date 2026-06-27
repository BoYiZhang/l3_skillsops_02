# 运营统计大盘重构 — 设计文档

## 概述

将当前简单的运营统计页面升级为完整的运营大盘，从管理者视角展示 KPI 指标、趋势图表、分类分布、评分分析和审核概况。

## 技术方案

- **前端图表库**: ECharts 5 + vue-echarts 7（按需引入）
- **后端**: Spring Boot + MyBatis Plus，在现有 `AdminServiceImpl.getStats()` 上扩展
- **数据粒度**: 7 天 + 30 天可切换，按日聚合

---

## 1. 页面布局

```
┌──────────────────────────────────────────────────────┐
│  KPI 卡片行 (4卡片)                                    │
│  [Skills] [Users] [Installs] [Avg Rating]             │
├──────────────────────────────────────────────────────┤
│  时间范围切换:  [近7天] [近30天]                        │
├──────────────────────────┬───────────────────────────┤
│  安装趋势 (折线图)         │  用户增长 (折线图)          │
│  双线：累计 + 每日新增     │  每日新增用户              │
│  支持7天/30天切换         │  支持7天/30天切换           │
├──────────────────────────┴───────────────────────────┤
│  Skill分类分布 (环形图)    │  评分分布 (柱状图)          │
│  按分类展示 skill 数量     │  1-5星各多少个             │
├──────────────────────────┬───────────────────────────┤
│  热门Skill Top10 (横向柱状)│  审核概况                   │
│  安装量可视化             │  待审/已发布/下架/草稿      │
│                          │  各状态卡片 + 数量          │
└──────────────────────────┴───────────────────────────┘
```

### 子组件拆分

| 组件 | 职责 | 图表类型 |
|------|------|---------|
| `StatsKpiCards.vue` | 4 个 KPI 指标卡片 | 无（Element Plus el-card） |
| `TrendChart.vue` | 通用趋势折线图，props 控制数据源 | ECharts 折线图 |
| `CategoryPieChart.vue` | 分类分布环形图 | ECharts 环形饼图 |
| `RatingBarChart.vue` | 评分分布柱状图 | ECharts 柱状图 |
| `TopSkillsChart.vue` | Top10 热门 Skill 横向柱状图 | ECharts 横向柱状图 |
| `AuditSummary.vue` | 审核概况状态卡片 | 无（Element Plus el-card） |
| `StatsTab.vue` | 容器组件，组合以上子组件 | 管理时间范围状态、数据加载 |

---

## 2. 后端 API 扩展

### 2.1 接口不变

- **路径**: `GET /api/v1/admin/stats`
- **权限**: `@PreAuthorize("hasRole('ADMIN')")` 不变
- **响应类型**: `Result<AdminStatsVO>`

### 2.2 AdminStatsVO 新增字段

```java
// 现有字段不变
private long totalSkills;
private long totalUsers;
private long totalInstalls;
private List<TrendItem> installTrend;    // 扩展为30天
private List<AuthorStat> topAuthors;
private List<SkillVO> topSkills;         // 扩展为 Top10

// 新增字段
private double avgRating;                          // 全局平均评分
private List<TrendItem> userTrend;                 // 用户增长趋势
private List<CategoryStat> categoryDistribution;   // 分类分布
private List<RatingDist> ratingDistribution;       // 评分分布 1-5星
private AuditSummary auditSummary;                 // 审核概况
```

### 2.3 新增内部类

```java
// AdminStatsVO 内部
@Data @NoArgsConstructor @AllArgsConstructor
public static class CategoryStat {
    private String categoryName;
    private long count;
}

@Data @NoArgsConstructor @AllArgsConstructor
public static class RatingDist {
    private int star;    // 1-5
    private long count;
}

@Data @NoArgsConstructor @AllArgsConstructor
public static class AuditSummary {
    private long pending;      // PENDING_APPROVAL
    private long published;    // PUBLISHED
    private long delisted;     // DELISTED
    private long draft;        // DRAFT
}
```

### 2.4 SQL 查询

全部通过 MyBatis Plus LambdaQueryWrapper + 手动聚合实现，无需写 XML：

- **安装趋势 30 天**: 查 `skill_installs` 表，`create_time >= 30天前`，按 `DATE(create_time)` 分组，`COUNT(*)`
- **用户趋势 30 天**: 查 `users` 表，`create_time >= 30天前`，按 `DATE(create_time)` 分组，`COUNT(*)`
- **分类分布**: 查 `skills` 表，按 `category_id` 分组 `COUNT(*)`，关联 `categories` 取 name
- **评分分布**: 查 `skill_ratings` 表，按 `rating` 分组 `COUNT(*)`
- **审核概况**: 查 `skills` 表，按 `status` 分组 `COUNT(*)`
- **Top10**: 现有 Top5 改为 Top10（改 page size）
- **avgRating**: 查 `skills` 表，`AVG(avg_rating)`

---

## 3. 前端实现

### 3.1 新增依赖

```json
{
  "echarts": "^5.5.0",
  "vue-echarts": "^7.0.0"
}
```

### 3.2 组件数据流

```
StatsTab (容器)
  ├─ 时间范围状态: activeRange = '7d' | '30d'
  ├─ fetchStats() → store.fetchStats()
  │     ├─ 前端过滤 trend 数据按时间范围
  │     └─ 其余数据全量渲染
  ├─ StatsKpiCards    (props: stats)
  ├─ TrendChart ×2    (props: data[], title, color, range)
  ├─ CategoryPieChart (props: data[])
  ├─ RatingBarChart   (props: data[])
  ├─ TopSkillsChart   (props: skills[])
  └─ AuditSummary     (props: summary)
```

### 3.3 ECharts 按需引入策略

在 `main.js` 或独立 `echarts.js` 中注册必要组件，避免全量引入：

```js
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])
```

### 3.4 图表配色方案

统一采用 Element Plus 色系：

| 用途 | 颜色 |
|------|------|
| 主色（安装/新增） | `#1677ff` |
| 辅色（用户） | `#52c41a` |
| 辅助色 2（评分） | `#fa8c16` |
| 分类环形图 | `['#1677ff','#52c41a','#fa8c16','#722ed1','#eb2f96','#13c2c2','#f5222d','#faad14']` |
| 审核 - 待审 | `#faad14` |
| 审核 - 已发布 | `#52c41a` |
| 审核 - 下架 | `#ff4d4f` |
| 审核 - 草稿 | `#999` |

### 3.5 响应式布局

- 桌面端（>=1200px）: 双列图表布局
- 平板端（768-1200px）: 单列图表布局
- 移动端（<768px）: 单列 + KPI 卡片 2 列

---

## 4. 错误处理

- 图表数据为空时，ECharts 显示空状态占位（`showEmpty: true`）
- 后端查询异常由全局异常处理器统一返回 `Result.error()`
- 前端 `fetchStats()` 失败时，各子组件通过 `v-if` 显示空状态，不阻塞页面渲染

---

## 5. 兼容性

- **不改变现有 API 路径和权限模型**
- **AdminStatsVO 只增字段，不删不改已有字段** — 现有消费者不受影响
- **StatsTab 仍是 WorkspaceView 的 "运营统计" tab** — 路由和入口不变
- 前端 store `fetchStats()` 调用路径不变

---

## 6. 涉及文件

| 文件 | 变更类型 |
|------|---------|
| `backend/.../vo/AdminStatsVO.java` | 修改 — 新增字段和内部类 |
| `backend/.../service/impl/AdminServiceImpl.java` | 修改 — 扩展 getStats() 查询 |
| `frontend/package.json` | 修改 — 新增 echarts, vue-echarts |
| `frontend/src/main.js` | 修改 — 注册 ECharts 组件 |
| `frontend/src/components/workspace/StatsTab.vue` | 重写 — 容器组件 |
| `frontend/src/components/workspace/StatsKpiCards.vue` | 新增 |
| `frontend/src/components/workspace/TrendChart.vue` | 新增 |
| `frontend/src/components/workspace/CategoryPieChart.vue` | 新增 |
| `frontend/src/components/workspace/RatingBarChart.vue` | 新增 |
| `frontend/src/components/workspace/TopSkillsChart.vue` | 新增 |
| `frontend/src/components/workspace/AuditSummary.vue` | 新增 |
| `frontend/src/stores/workspace.js` | 不修改 |

---

## 7. 测试策略

- **后端**: 扩展 `AdminServiceTest.getStats()` 验证新增字段非空和非负
- **前端**: 各子组件渲染测试（Chart 组件 mock echarts），验证数据正确传入
- **E2E**: Admin 登录后访问运营统计，验证 6 个区域均正常渲染
