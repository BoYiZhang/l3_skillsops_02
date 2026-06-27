# 运营统计大盘重构 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将运营统计从简单 KPI 页面升级为带 ECharts 图表的完整运营大盘，包含安装趋势、用户增长、分类分布、评分分布、审核概况等可视化。

**Architecture:** 后端在 AdminStatsVO 新增 5 个字段 + 3 个内部类，getStats() 扩展 6 个查询聚合；前端新增 echarts + vue-echarts 依赖，拆分为 6 个子组件 + 1 个容器组件，ECharts 按需引入。

**Tech Stack:** Spring Boot 2.7 + MyBatis Plus 3.5 + Vue 3.4 + Element Plus 2.7 + ECharts 5.5 + vue-echarts 7.0

## Global Constraints

- API 路径不变: `GET /api/v1/admin/stats`
- 权限不变: `@PreAuthorize("hasRole('ADMIN')")`
- 现有 VO 字段不删不改，只新增
- 色系统一: Element Plus 色系 (`#1677ff`, `#52c41a`, `#fa8c16`, etc.)
- ECharts 按需引入，不全量加载
- 组件放在 `frontend/src/components/workspace/` 目录下

---

### Task 1: 扩展 AdminStatsVO 新增内部类和字段

**Files:**
- Modify: `backend/src/main/java/com/boyi/skillops/vo/AdminStatsVO.java`

**Interfaces:**
- Produces: `AdminStatsVO.avgRating (double)`, `AdminStatsVO.userTrend (List<TrendItem>)`, `AdminStatsVO.categoryDistribution (List<CategoryStat>)`, `AdminStatsVO.ratingDistribution (List<RatingDist>)`, `AdminStatsVO.auditSummary (AuditSummary)`

- [ ] **Step 1: 修改 AdminStatsVO.java**

在现有的 `topSkills` 字段之后、`TrendItem` 内部类之前，新增以下字段和内部类。完整内容如下：

```java
package com.boyi.skillops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class AdminStatsVO {
    private long totalSkills;
    private long totalUsers;
    private long totalInstalls;
    private List<TrendItem> installTrend;
    private List<AuthorStat> topAuthors;
    private List<SkillVO> topSkills;

    // ======== 新增字段 ========
    private double avgRating;
    private List<TrendItem> userTrend;
    private List<CategoryStat> categoryDistribution;
    private List<RatingDist> ratingDistribution;
    private AuditSummary auditSummary;

    // ======== 已有内部类 ========
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorStat {
        private Long authorId;
        private String authorName;
        private long skillCount;
    }

    // ======== 新增内部类 ========
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String categoryName;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDist {
        private int star;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditSummary {
        private long pending;
        private long published;
        private long delisted;
        private long draft;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /opt/l3_skillsops_02/backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/boyi/skillops/vo/AdminStatsVO.java
git commit -m "feat: add new fields to AdminStatsVO — avgRating, userTrend, categoryDistribution, ratingDistribution, auditSummary"
```

---

### Task 2: 扩展 AdminServiceImpl.getStats() 新增聚合查询

**Files:**
- Modify: `backend/src/main/java/com/boyi/skillops/service/impl/AdminServiceImpl.java`

**Interfaces:**
- Consumes: `SkillRatingMapper` (需要注入), `AdminStatsVO.CategoryStat`, `AdminStatsVO.RatingDist`, `AdminStatsVO.AuditSummary` (from Task 1)
- Produces: `getStats()` 返回完整的 AdminStatsVO（所有新字段已填充）

- [ ] **Step 1: 注入 SkillRatingMapper**

在 AdminServiceImpl 的字段声明区域（第 30 行之后）新增：

```java
    @Autowired private SkillRatingMapper ratingMapper;
```

需要新增 import:
```java
import com.boyi.skillops.entity.SkillRating;
```

- [ ] **Step 2: 重写 getStats() 方法**

用以下代码替换现有 `getStats()` 方法（第 75-116 行）：

```java
    @Override
    public AdminStatsVO getStats() {
        AdminStatsVO stats = new AdminStatsVO();
        stats.setTotalSkills(skillMapper.selectCount(null));
        stats.setTotalUsers(userMapper.selectCount(null));
        stats.setTotalInstalls(installMapper.selectCount(null));

        // avg rating: AVG across all skills that have at least one rating
        List<Skill> allSkills = skillMapper.selectList(null);
        double avgRating = allSkills.stream()
                .filter(s -> s.getAvgRating() != null && s.getAvgRating() > 0)
                .mapToDouble(Skill::getAvgRating)
                .average()
                .orElse(0.0);
        stats.setAvgRating(Math.round(avgRating * 10.0) / 10.0);

        // top skills by install count — Top 10
        Page<Skill> topPage = new Page<>(1, 10);
        Page<Skill> topResult = skillMapper.selectPage(topPage,
                new LambdaQueryWrapper<Skill>().orderByDesc(Skill::getInstallCount));
        stats.setTopSkills(topResult.getRecords().stream().map(this::toVO).collect(Collectors.toList()));

        // top authors — Top 10
        Map<Long, Long> authorCount = allSkills.stream()
                .collect(Collectors.groupingBy(Skill::getAuthorId, Collectors.counting()));
        List<AdminStatsVO.AuthorStat> authors = authorCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    User u = userMapper.selectById(e.getKey());
                    return new AdminStatsVO.AuthorStat(e.getKey(), u != null ? u.getUsername() : "unknown", e.getValue());
                })
                .collect(Collectors.toList());
        stats.setTopAuthors(authors);

        // install trend: last 30 days
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime thirtyDaysAgo = today.minusDays(29).atStartOfDay();
        List<SkillInstall> recentInstalls = installMapper.selectList(
                new LambdaQueryWrapper<SkillInstall>().ge(SkillInstall::getCreateTime, thirtyDaysAgo));
        Map<java.time.LocalDate, Long> installDailyCount = recentInstalls.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getCreateTime().toLocalDate(),
                        Collectors.counting()));
        List<AdminStatsVO.TrendItem> installTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            installTrend.add(new AdminStatsVO.TrendItem(
                    d.toString(),  // yyyy-MM-dd
                    installDailyCount.getOrDefault(d, 0L)));
        }
        stats.setInstallTrend(installTrend);

        // user trend: last 30 days
        List<User> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, thirtyDaysAgo));
        Map<java.time.LocalDate, Long> userDailyCount = allUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreateTime().toLocalDate(),
                        Collectors.counting()));
        List<AdminStatsVO.TrendItem> userTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            userTrend.add(new AdminStatsVO.TrendItem(
                    d.toString(),
                    userDailyCount.getOrDefault(d, 0L)));
        }
        stats.setUserTrend(userTrend);

        // category distribution
        Map<Long, Long> catCount = allSkills.stream()
                .collect(Collectors.groupingBy(Skill::getCategoryId, Collectors.counting()));
        List<AdminStatsVO.CategoryStat> catDist = catCount.entrySet().stream()
                .map(e -> {
                    Category cat = categoryMapper.selectById(e.getKey());
                    return new AdminStatsVO.CategoryStat(
                            cat != null ? cat.getName() : "未分类", e.getValue());
                })
                .collect(Collectors.toList());
        stats.setCategoryDistribution(catDist);

        // rating distribution: 1-5 stars
        List<SkillRating> allRatings = ratingMapper.selectList(null);
        Map<Integer, Long> ratingCount = allRatings.stream()
                .collect(Collectors.groupingBy(SkillRating::getRating, Collectors.counting()));
        List<AdminStatsVO.RatingDist> ratingDist = new ArrayList<>();
        for (int star = 1; star <= 5; star++) {
            ratingDist.add(new AdminStatsVO.RatingDist(star, ratingCount.getOrDefault(star, 0L)));
        }
        stats.setRatingDistribution(ratingDist);

        // audit summary — count by status
        long pending = skillMapper.selectCount(
                new LambdaQueryWrapper<Skill>().eq(Skill::getStatus, "PENDING_APPROVAL"));
        long published = skillMapper.selectCount(
                new LambdaQueryWrapper<Skill>().eq(Skill::getStatus, "PUBLISHED"));
        long delisted = skillMapper.selectCount(
                new LambdaQueryWrapper<Skill>().eq(Skill::getStatus, "DELISTED"));
        long draft = skillMapper.selectCount(
                new LambdaQueryWrapper<Skill>().eq(Skill::getStatus, "DRAFT"));
        stats.setAuditSummary(new AdminStatsVO.AuditSummary(pending, published, delisted, draft));

        return stats;
    }
```

- [ ] **Step 3: 编译验证**

```bash
cd /opt/l3_skillsops_02/backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/boyi/skillops/service/impl/AdminServiceImpl.java
git commit -m "feat: extend getStats() with 30-day trends, category/rating distribution, audit summary"
```

---

### Task 3: 扩展后端测试

**Files:**
- Modify: `backend/src/test/java/com/boyi/skillops/service/AdminServiceTest.java`

**Interfaces:**
- Consumes: `SkillRatingMapper` (from Task 2), `SkillInstallMapper`, `AdminStatsVO` 新字段 (from Task 1)

- [ ] **Step 1: 注入新 Mapper 并扩展 testGetStats**

在 AdminServiceTest 类中新增注入（第 31 行附近）:

```java
    @Autowired private SkillRatingMapper ratingMapper;
    @Autowired private SkillInstallMapper installMapper;
```

扩展 `testGetStats()` 方法（替换第 142-148 行）:

```java
    // ========== testGetStats ==========
    @Test
    void testGetStats() {
        // create a skill and give it a rating to populate stats
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("StatsSkill_" + classSuffix);
        sReq.setDescription("For stats test");
        sReq.setCategoryId(1L);
        com.boyi.skillops.vo.SkillVO vo = skillService.create(sReq, userId);

        // insert a rating
        SkillRating rating = new SkillRating();
        rating.setSkillId(vo.getId());
        rating.setUserId(userId);
        rating.setRating(5);
        ratingMapper.insert(rating);

        // update skill avg_rating
        Skill skill = skillMapper.selectById(vo.getId());
        skill.setAvgRating(5.0);
        skillMapper.updateById(skill);

        // insert an install
        SkillInstall install = new SkillInstall();
        install.setSkillId(vo.getId());
        install.setUserId(userId);
        install.setVersionId(1L);
        installMapper.insert(install);

        AdminStatsVO stats = adminService.getStats();
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalSkills()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getTotalUsers()).isGreaterThanOrEqualTo(2);
        assertThat(stats.getTotalInstalls()).isGreaterThanOrEqualTo(1);

        // new fields
        assertThat(stats.getAvgRating()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.getInstallTrend()).isNotEmpty();
        assertThat(stats.getUserTrend()).isNotNull();
        assertThat(stats.getCategoryDistribution()).isNotNull();
        assertThat(stats.getRatingDistribution()).hasSize(5);
        assertThat(stats.getAuditSummary()).isNotNull();
        assertThat(stats.getAuditSummary().getDraft()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAuditSummary().getPending()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAuditSummary().getPublished()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAuditSummary().getDelisted()).isGreaterThanOrEqualTo(0);
    }
```

新增 import:
```java
import com.boyi.skillops.entity.SkillInstall;
import com.boyi.skillops.entity.SkillRating;
```

- [ ] **Step 2: 运行测试验证**

```bash
cd /opt/l3_skillsops_02/backend && mvn test -Dtest=AdminServiceTest#testGetStats -q
```

Expected: Tests run: 1, Failures: 0

- [ ] **Step 3: 运行全部测试确保无回归**

```bash
cd /opt/l3_skillsops_02/backend && mvn test -q
```

Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/boyi/skillops/service/AdminServiceTest.java
git commit -m "test: extend getStats test for new fields — avgRating, trends, distributions, audit summary"
```

---

### Task 4: 安装 ECharts 依赖并注册

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/src/main.js`
- Create: `frontend/src/utils/echarts.js`

**Interfaces:**
- Produces: ECharts 全局可用，VChart 组件可在任何 .vue 文件中使用

- [ ] **Step 1: 安装依赖**

```bash
cd /opt/l3_skillsops_02/frontend && npm install echarts@^5.5.0 vue-echarts@^7.0.0
```

Expected: packages added to package.json and node_modules

- [ ] **Step 2: 创建 echarts.js 按需引入配置**

创建 `frontend/src/utils/echarts.js`:

```js
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
])
```

- [ ] **Step 3: 在 main.js 中注册 vue-echarts 并引入 echarts 配置**

修改 `frontend/src/main.js`，在 `import router from './router'` 之后新增:

```js
import './utils/echarts'
import VChart from 'vue-echarts'
```

在 `app.use(ElementPlus)` 之后新增:

```js
app.component('VChart', VChart)
```

- [ ] **Step 4: 验证编译**

```bash
cd /opt/l3_skillsops_02/frontend && npm run build 2>&1 | tail -5
```

Expected: Build succeeds without errors

- [ ] **Step 5: Commit**

```bash
cd /opt/l3_skillsops_02/frontend && git add package.json package-lock.json src/main.js src/utils/echarts.js
git commit -m "feat: add echarts + vue-echarts with on-demand imports"
```

---

### Task 5: 创建 StatsKpiCards 组件

**Files:**
- Create: `frontend/src/components/workspace/StatsKpiCards.vue`

**Interfaces:**
- Consumes: props: `stats` (AdminStatsVO 对象)
- Produces: 4 个 KPI 卡片 UI

- [ ] **Step 1: 创建 StatsKpiCards.vue**

```vue
<template>
  <el-row :gutter="20" class="kpi-cards">
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--blue">
        <div class="kpi-icon kpi-icon--blue"><el-icon size="28"><Document /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.totalSkills ?? 0 }}</div>
          <div class="kpi-label">Skill 总数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--green">
        <div class="kpi-icon kpi-icon--green"><el-icon size="28"><User /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.totalUsers ?? 0 }}</div>
          <div class="kpi-label">用户总数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--orange">
        <div class="kpi-icon kpi-icon--orange"><el-icon size="28"><Download /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.totalInstalls ?? 0 }}</div>
          <div class="kpi-label">安装总数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--purple">
        <div class="kpi-icon kpi-icon--purple"><el-icon size="28"><Star /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.avgRating?.toFixed(1) ?? '0.0' }}</div>
          <div class="kpi-label">平均评分</div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { Document, User, Download, Star } from '@element-plus/icons-vue'
defineProps({ stats: { type: Object, default: () => ({}) } })
</script>

<style scoped>
.kpi-cards { margin-bottom: 20px; }
.kpi-card :deep(.el-card__body) {
  display: flex; align-items: center; gap: 16px; padding: 20px;
}
.kpi-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.kpi-icon--blue   { background: #e6f4ff; color: #1677ff; }
.kpi-icon--green  { background: #f6ffed; color: #52c41a; }
.kpi-icon--orange { background: #fff7e6; color: #fa8c16; }
.kpi-icon--purple { background: #f9f0ff; color: #722ed1; }
.kpi-value { font-size: 28px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.kpi-label { font-size: 13px; color: #999; margin-top: 2px; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/workspace/StatsKpiCards.vue
git commit -m "feat: add StatsKpiCards component with 4 KPI cards"
```

---

### Task 6: 创建 TrendChart 通用趋势图组件

**Files:**
- Create: `frontend/src/components/workspace/TrendChart.vue`

**Interfaces:**
- Consumes: props: `title (String)`, `data (Array<{date,count}>)`, `color (String)`, `range ('7d'|'30d')`
- Produces: ECharts 折线图

- [ ] **Step 1: 创建 TrendChart.vue**

```vue
<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">{{ title }}</span>
    </template>
    <v-chart :option="option" style="height: 280px" autoresize />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  data: { type: Array, default: () => [] },
  color: { type: String, default: '#1677ff' },
  range: { type: String, default: '7d' },
})

const option = computed(() => {
  const limit = props.range === '7d' ? 7 : 30
  const sliced = props.data.slice(-limit)
  const dates = sliced.map(d => d.date)
  const values = sliced.map(d => d.count)

  return {
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: p => `${p[0].axisValue}<br/><b>${p[0].value}</b> 次`,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: props.range === '30d' ? 45 : 0, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: props.color, width: 2 },
      itemStyle: { color: props.color },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: props.color + '40' },
            { offset: 1, color: props.color + '05' },
          ],
        },
      },
    }],
  }
})
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/workspace/TrendChart.vue
git commit -m "feat: add TrendChart component — reusable ECharts line chart"
```

---

### Task 7: 创建 CategoryPieChart 分类环形图组件

**Files:**
- Create: `frontend/src/components/workspace/CategoryPieChart.vue`

**Interfaces:**
- Consumes: props: `data (Array<{categoryName, count}>)`
- Produces: ECharts 环形饼图

- [ ] **Step 1: 创建 CategoryPieChart.vue**

```vue
<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">📂 Skill 分类分布</span>
    </template>
    <v-chart v-if="data?.length" :option="option" style="height: 280px" autoresize />
    <el-empty v-else description="暂无分类数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const COLORS = ['#1677ff','#52c41a','#fa8c16','#722ed1','#eb2f96','#13c2c2','#f5222d','#faad14']

const option = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    textStyle: { fontSize: 12 },
  },
  color: COLORS,
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    center: ['38%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
      scaleSize: 8,
    },
    data: props.data.map(d => ({ name: d.categoryName, value: d.count })),
  }],
}))
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/workspace/CategoryPieChart.vue
git commit -m "feat: add CategoryPieChart component — donut chart"
```

---

### Task 8: 创建 RatingBarChart 评分分布组件

**Files:**
- Create: `frontend/src/components/workspace/RatingBarChart.vue`

**Interfaces:**
- Consumes: props: `data (Array<{star, count}>)`
- Produces: ECharts 柱状图

- [ ] **Step 1: 创建 RatingBarChart.vue**

```vue
<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">⭐ 评分分布</span>
    </template>
    <v-chart v-if="data?.length" :option="option" style="height: 280px" autoresize />
    <el-empty v-else description="暂无评分数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const STAR_COLORS = ['#f5222d','#fa8c16','#faad14','#a0d911','#52c41a']

const option = computed(() => ({
  grid: { top: 20, right: 20, bottom: 30, left: 40 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: props.data.map(d => d.star + ' 星'),
    axisLabel: { fontSize: 13 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f0f0' } },
  },
  series: [{
    type: 'bar',
    barWidth: '50%',
    data: props.data.map((d, i) => ({
      value: d.count,
      itemStyle: { color: STAR_COLORS[i], borderRadius: [4, 4, 0, 0] },
    })),
    label: { show: true, position: 'top', fontSize: 13, fontWeight: 600 },
  }],
}))
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/workspace/RatingBarChart.vue
git commit -m "feat: add RatingBarChart component — star rating bar chart"
```

---

### Task 9: 创建 TopSkillsChart 热门 Skill 组件

**Files:**
- Create: `frontend/src/components/workspace/TopSkillsChart.vue`

**Interfaces:**
- Consumes: props: `skills (Array<SkillVO>)`
- Produces: ECharts 横向柱状图

- [ ] **Step 1: 创建 TopSkillsChart.vue**

```vue
<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">🏆 热门 Skill Top 10</span>
    </template>
    <v-chart v-if="skills?.length" :option="option" style="height: 360px" autoresize />
    <el-empty v-else description="暂无数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  skills: { type: Array, default: () => [] },
})

const option = computed(() => {
  const names = props.skills.map((s, i) => `${i + 1}. ${s.name}`).reverse()
  const values = [...props.skills].map(s => s.installCount ?? 0).reverse()

  return {
    grid: { top: 10, right: 30, bottom: 20, left: 120 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: p => `${p[0].name}<br/>安装量: <b>${p[0].value}</b>`,
    },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 12 },
    },
    series: [{
      type: 'bar',
      barWidth: '60%',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: ['#f53f3f','#ff7d00','#f7ba1e','#1677ff','#1677ff',
                  '#1677ff','#1677ff','#1677ff','#1677ff','#1677ff'][9 - i],
          borderRadius: [0, 4, 4, 0],
        },
      })),
      label: { show: true, position: 'right', fontSize: 12, fontWeight: 600 },
    }],
  }
})
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/workspace/TopSkillsChart.vue
git commit -m "feat: add TopSkillsChart component — horizontal bar chart"
```

---

### Task 10: 创建 AuditSummary 审核概况组件

**Files:**
- Create: `frontend/src/components/workspace/AuditSummary.vue`

**Interfaces:**
- Consumes: props: `summary (Object: {pending, published, delisted, draft})`
- Produces: 4 个状态卡片

- [ ] **Step 1: 创建 AuditSummary.vue**

```vue
<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">📋 审核概况</span>
    </template>
    <div class="audit-grid">
      <div class="audit-item">
        <div class="audit-dot audit-dot--warning"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.pending ?? 0 }}</div>
          <div class="audit-label">待审核</div>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-dot audit-dot--success"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.published ?? 0 }}</div>
          <div class="audit-label">已发布</div>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-dot audit-dot--danger"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.delisted ?? 0 }}</div>
          <div class="audit-label">已下架</div>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-dot audit-dot--default"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.draft ?? 0 }}</div>
          <div class="audit-label">草稿</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ summary: { type: Object, default: () => ({}) } })
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
.audit-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.audit-item { display: flex; align-items: center; gap: 12px; padding: 16px; background: #f7f8fa; border-radius: 8px; }
.audit-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.audit-dot--warning { background: #faad14; }
.audit-dot--success { background: #52c41a; }
.audit-dot--danger  { background: #ff4d4f; }
.audit-dot--default { background: #999; }
.audit-value { font-size: 24px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.audit-label { font-size: 13px; color: #999; }

@media (max-width: 768px) {
  .audit-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/workspace/AuditSummary.vue
git commit -m "feat: add AuditSummary component — review status cards"
```

---

### Task 11: 重写 StatsTab 容器组件

**Files:**
- Modify: `frontend/src/components/workspace/StatsTab.vue`

**Interfaces:**
- Consumes: `StatsKpiCards`, `TrendChart`, `CategoryPieChart`, `RatingBarChart`, `TopSkillsChart`, `AuditSummary` (from Tasks 5-10)
- Produces: 完整的运营大盘页面

- [ ] **Step 1: 重写 StatsTab.vue**

```vue
<template>
  <div class="stats-dashboard" v-loading="loading">
    <!-- KPI 卡片 -->
    <StatsKpiCards :stats="stats" />

    <!-- 时间范围切换 -->
    <div class="range-bar">
      <el-radio-group v-model="activeRange" size="small">
        <el-radio-button value="7d">近 7 天</el-radio-button>
        <el-radio-button value="30d">近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 趋势图行 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <TrendChart
          title="📊 安装趋势"
          :data="stats?.installTrend ?? []"
          :range="activeRange"
          color="#1677ff"
        />
      </el-col>
      <el-col :xs="24" :md="12">
        <TrendChart
          title="👥 用户增长"
          :data="stats?.userTrend ?? []"
          :range="activeRange"
          color="#52c41a"
        />
      </el-col>
    </el-row>

    <!-- 分类 & 评分 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <CategoryPieChart :data="stats?.categoryDistribution ?? []" />
      </el-col>
      <el-col :xs="24" :md="12">
        <RatingBarChart :data="stats?.ratingDistribution ?? []" />
      </el-col>
    </el-row>

    <!-- 热门 Skill & 审核概况 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <TopSkillsChart :skills="stats?.topSkills ?? []" />
      </el-col>
      <el-col :xs="24" :md="8">
        <AuditSummary :summary="stats?.auditSummary" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'
import StatsKpiCards from './StatsKpiCards.vue'
import TrendChart from './TrendChart.vue'
import CategoryPieChart from './CategoryPieChart.vue'
import RatingBarChart from './RatingBarChart.vue'
import TopSkillsChart from './TopSkillsChart.vue'
import AuditSummary from './AuditSummary.vue'

const store = useWorkspaceStore()
const stats = ref(null)
const loading = ref(false)
const activeRange = ref('7d')

onMounted(async () => {
  loading.value = true
  try { stats.value = await store.fetchStats() } finally { loading.value = false }
})
</script>

<style scoped>
.stats-dashboard { padding: 4px 0; }
.range-bar { margin-bottom: 16px; text-align: right; }
</style>
```

- [ ] **Step 2: 编译验证**

```bash
cd /opt/l3_skillsops_02/frontend && npm run build 2>&1 | tail -5
```

Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/workspace/StatsTab.vue
git commit -m "feat: rewrite StatsTab as dashboard with 6 chart components"
```

---

### Task 12: 端到端验证

**Files:**
- Verify: 所有已修改/新增的文件

- [ ] **Step 1: 运行后端全部测试**

```bash
cd /opt/l3_skillsops_02/backend && mvn test -q
```

Expected: All tests pass, no regressions

- [ ] **Step 2: 前端编译**

```bash
cd /opt/l3_skillsops_02/frontend && npm run build 2>&1 | tail -10
```

Expected: Build succeeds with no errors

- [ ] **Step 3: 启动后端并验证 API**

```bash
cd /opt/l3_skillsops_02/backend && mvn spring-boot:run &
# Wait for startup, then:
curl -s -H "Authorization: Bearer <admin_token>" http://localhost:8080/api/v1/admin/stats | python3 -m json.tool | head -40
```

Expected: JSON 包含所有新增字段 `avgRating`, `userTrend`, `categoryDistribution`, `ratingDistribution`, `auditSummary`

- [ ] **Step 4: Commit (如有微调)**

```bash
git add . && git commit -m "chore: final adjustments after e2e verification"
```
