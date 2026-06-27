package com.boyi.skillops.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.MarketQueryRequest;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.service.MarketService;
import com.boyi.skillops.vo.SkillVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketServiceImpl implements MarketService {

    @Autowired private SkillMapper skillMapper;
    @Autowired private SkillVersionMapper versionMapper;
    @Autowired private SkillInstallMapper installMapper;
    @Autowired private SkillRatingMapper ratingMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private UserMapper userMapper;

    @Override
    public PageResult<SkillVO> queryMarket(MarketQueryRequest req, Long userId) {
        // 获取用户已安装的 Skill ID（用于展示已下架但仍可见的）
        List<Long> installedIds = userId != null ? installMapper.selectList(
                new LambdaQueryWrapper<SkillInstall>().eq(SkillInstall::getUserId, userId))
                .stream().map(SkillInstall::getSkillId).collect(Collectors.toList()) : Collections.emptyList();

        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<>();
        // PUBLISHED 或 (DELISTED 且用户已安装)
        qw.and(w -> w.eq(Skill::getStatus, "PUBLISHED")
                .or(w2 -> {
                    if (!installedIds.isEmpty()) {
                        w2.eq(Skill::getStatus, "DELISTED").in(Skill::getId, installedIds);
                    } else {
                        w2.eq(Skill::getStatus, "DELISTED").eq(Skill::getId, -1L); // 无安装则不加 DELISTED
                    }
                }));
        if (req.getCategoryId() != null) qw.eq(Skill::getCategoryId, req.getCategoryId());
        if (StringUtils.hasText(req.getKeyword())) {
            qw.and(w -> w.like(Skill::getName, req.getKeyword()).or().like(Skill::getDescription, req.getKeyword()));
        }
        if ("HOT".equals(req.getSortBy())) qw.orderByDesc(Skill::getInstallCount);
        else if ("RATING".equals(req.getSortBy())) qw.orderByDesc(Skill::getAvgRating);
        else qw.orderByDesc(Skill::getCreateTime);

        Page<Skill> p = new Page<>(req.getPage(), req.getSize());
        Page<Skill> result = skillMapper.selectPage(p, qw);
        List<SkillVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        PageResult<SkillVO> pr = new PageResult<>();
        pr.setRecords(vos); pr.setTotal(result.getTotal()); pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return pr;
    }

    @Override
    @Transactional
    public void install(Long skillId, Long userId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null || !"PUBLISHED".equals(skill.getStatus())) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不可用");
        SkillInstall exist = installMapper.selectOne(
                new LambdaQueryWrapper<SkillInstall>().eq(SkillInstall::getUserId, userId).eq(SkillInstall::getSkillId, skillId));
        if (exist != null) throw new BusinessException(ErrorCode.ALREADY_INSTALLED);
        SkillVersion latest = versionMapper.selectOne(
                new LambdaQueryWrapper<SkillVersion>().eq(SkillVersion::getSkillId, skillId).orderByDesc(SkillVersion::getCreateTime).last("LIMIT 1"));
        if (latest == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill无可用版本");
        SkillInstall install = new SkillInstall();
        install.setUserId(userId); install.setSkillId(skillId); install.setVersionId(latest.getId());
        installMapper.insert(install);
        skillMapper.updateInstallCount(skillId);
    }

    @Override
    public Map<String, Object> getInstallStatus(Long skillId, Long userId) {
        Map<String, Object> map = new HashMap<>();
        SkillInstall install = installMapper.selectOne(
                new LambdaQueryWrapper<SkillInstall>().eq(SkillInstall::getUserId, userId).eq(SkillInstall::getSkillId, skillId));
        map.put("installed", install != null);
        if (install != null) {
            SkillRating rating = ratingMapper.selectOne(
                    new LambdaQueryWrapper<SkillRating>().eq(SkillRating::getUserId, userId).eq(SkillRating::getSkillId, skillId));
            if (rating != null) {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("rating", rating.getRating());
                rMap.put("comment", rating.getComment());
                map.put("rating", rMap);
            } else {
                map.put("rating", null);
            }
        }
        return map;
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
}
