package com.boyi.skillops.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.vo.AdminStatsVO;
import com.boyi.skillops.vo.CategoryVO;
import com.boyi.skillops.vo.SkillVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired private SkillMapper skillMapper;
    @Autowired private SkillAuditMapper auditMapper;
    @Autowired private SkillInstallMapper installMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private SkillVersionMapper versionMapper;
    @Autowired private SkillRatingMapper ratingMapper;

    @Override
    @Transactional
    public void approve(Long skillId, Long auditorId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        if (!"PENDING_APPROVAL".equals(skill.getStatus())) throw new BusinessException(ErrorCode.INVALID_STATUS, "仅待审核状态可通过");
        skill.setStatus("PUBLISHED");
        skillMapper.updateById(skill);
        saveAudit(skillId, auditorId, "APPROVE", null);
    }

    @Override
    @Transactional
    public void reject(Long skillId, String reason, Long auditorId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        if (!"PENDING_APPROVAL".equals(skill.getStatus())) throw new BusinessException(ErrorCode.INVALID_STATUS, "仅待审核状态可拒绝");
        skill.setStatus("DRAFT");
        skillMapper.updateById(skill);
        saveAudit(skillId, auditorId, "REJECT", reason);
    }

    @Override
    @Transactional
    public void delist(Long skillId, String reason, Long auditorId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        if (!"PUBLISHED".equals(skill.getStatus())) throw new BusinessException(ErrorCode.INVALID_STATUS, "仅已上架状态可下架");
        skill.setStatus("DELISTED");
        skillMapper.updateById(skill);
        saveAudit(skillId, auditorId, "DELIST", reason);
    }

    @Override
    public PageResult<SkillVO> getPendingSkills(int page, int size) {
        Page<Skill> p = new Page<>(page, size);
        Page<Skill> result = skillMapper.selectPage(p,
                new LambdaQueryWrapper<Skill>().eq(Skill::getStatus, "PENDING_APPROVAL").orderByDesc(Skill::getCreateTime));
        List<SkillVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return buildPage(vos, result);
    }

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

    @Override
    public CategoryVO createCategory(String name, String description) {
        Category cat = new Category();
        cat.setName(name);
        cat.setDescription(description);
        categoryMapper.insert(cat);
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(cat, vo);
        return vo;
    }

    @Override
    public CategoryVO updateCategory(Long id, String name, String description) {
        Category cat = categoryMapper.selectById(id);
        if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        cat.setName(name);
        cat.setDescription(description);
        categoryMapper.updateById(cat);
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(cat, vo);
        return vo;
    }

    @Override
    public void deleteCategory(Long id) {
        if (skillMapper.selectCount(new LambdaQueryWrapper<Skill>().eq(Skill::getCategoryId, id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类下存在Skill，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private void saveAudit(Long skillId, Long auditorId, String action, String reason) {
        SkillAudit audit = new SkillAudit();
        audit.setSkillId(skillId);
        audit.setAuditorId(auditorId);
        audit.setAction(action);
        audit.setReason(reason);
        auditMapper.insert(audit);
    }

    private SkillVO toVO(Skill skill) {
        SkillVO vo = new SkillVO();
        BeanUtils.copyProperties(skill, vo);
        Category cat = categoryMapper.selectById(skill.getCategoryId());
        if (cat != null) vo.setCategoryName(cat.getName());
        User author = userMapper.selectById(skill.getAuthorId());
        if (author != null) vo.setAuthorName(author.getUsername());
        SkillVersion latest = versionMapper.selectOne(
                new LambdaQueryWrapper<SkillVersion>().eq(SkillVersion::getSkillId, skill.getId()).orderByDesc(SkillVersion::getCreateTime).last("LIMIT 1"));
        if (latest != null) vo.setLatestVersion(latest.getVersion());
        vo.setCreateTime(skill.getCreateTime());
        vo.setUpdateTime(skill.getUpdateTime());
        return vo;
    }

    private <T> PageResult<T> buildPage(List<T> records, Page<?> result) {
        PageResult<T> pr = new PageResult<>();
        pr.setRecords(records); pr.setTotal(result.getTotal());
        pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return pr;
    }
}
