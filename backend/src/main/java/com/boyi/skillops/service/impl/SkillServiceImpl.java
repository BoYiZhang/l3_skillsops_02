package com.boyi.skillops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SkillServiceImpl implements SkillService {

    @Autowired private SkillMapper skillMapper;
    @Autowired private SkillVersionMapper versionMapper;
    @Autowired private SkillInstallMapper installMapper;
    @Autowired private SkillRatingMapper ratingMapper;
    @Autowired private SkillAuditMapper auditMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private UserMapper userMapper;

    @Override
    @Transactional
    public SkillVO create(SkillCreateRequest request, Long authorId) {
        Category cat = categoryMapper.selectById(request.getCategoryId());
        if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        Skill skill = new Skill();
        BeanUtils.copyProperties(request, skill);
        skill.setAuthorId(authorId);
        skill.setStatus("DRAFT");
        skill.setInstallCount(0L);
        skill.setAvgRating(0.0);
        skillMapper.insert(skill);
        return toVO(skill);
    }

    @Override
    @Transactional
    public SkillVO update(Long skillId, SkillCreateRequest request, Long userId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        if (!skill.getAuthorId().equals(userId) && !isAdmin(userId)) {
            throw new BusinessException(ErrorCode.NOT_AUTHOR);
        }
        Category cat = categoryMapper.selectById(request.getCategoryId());
        if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        BeanUtils.copyProperties(request, skill);
        skillMapper.updateById(skill);
        return toVO(skill);
    }

    @Override
    public SkillVO getById(Long skillId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        return toVO(skill);
    }

    @Override
    @Transactional
    public void submitForApproval(Long skillId, Long userId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        if (!skill.getAuthorId().equals(userId)) throw new BusinessException(ErrorCode.NOT_AUTHOR);
        if (!"DRAFT".equals(skill.getStatus())) throw new BusinessException(ErrorCode.INVALID_STATUS, "仅草稿状态可提交审核");
        skill.setStatus("PENDING_APPROVAL");
        skillMapper.updateById(skill);
    }

    @Override
    @Transactional
    public VersionVO publishVersion(Long skillId, VersionCreateRequest request, Long userId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Skill不存在");
        if (!skill.getAuthorId().equals(userId)) throw new BusinessException(ErrorCode.NOT_AUTHOR);
        if ("DELISTED".equals(skill.getStatus())) {
            skill.setStatus("PENDING_APPROVAL");
            skillMapper.updateById(skill);
        } else if (!"PUBLISHED".equals(skill.getStatus()) && !"DRAFT".equals(skill.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "当前状态不允许发布版本");
        }
        SkillVersion ver = new SkillVersion();
        ver.setSkillId(skillId);
        ver.setVersion(request.getVersion());
        ver.setChangelog(request.getChangelog());
        versionMapper.insert(ver);
        VersionVO vo = new VersionVO();
        BeanUtils.copyProperties(ver, vo);
        vo.setCreateTime(ver.getCreateTime());
        return vo;
    }

    @Override
    public PageResult<SkillVO> getMySkills(Long userId, int page, int size) {
        Page<Skill> p = new Page<>(page, size);
        Page<Skill> result = skillMapper.selectPage(p,
                new LambdaQueryWrapper<Skill>().eq(Skill::getAuthorId, userId).orderByDesc(Skill::getCreateTime));
        List<SkillVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        PageResult<SkillVO> pr = new PageResult<>();
        pr.setRecords(vos); pr.setTotal(result.getTotal()); pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return pr;
    }

    @Override
    public PageResult<VersionVO> getVersions(Long skillId, int page, int size) {
        Page<SkillVersion> p = new Page<>(page, size);
        Page<SkillVersion> result = versionMapper.selectPage(p,
                new LambdaQueryWrapper<SkillVersion>().eq(SkillVersion::getSkillId, skillId).orderByDesc(SkillVersion::getCreateTime));
        List<VersionVO> vos = result.getRecords().stream().map(v -> {
            VersionVO vo = new VersionVO();
            BeanUtils.copyProperties(v, vo);
            vo.setCreateTime(v.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        PageResult<VersionVO> pr = new PageResult<>();
        pr.setRecords(vos); pr.setTotal(result.getTotal()); pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return pr;
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

    private boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        return "admin".equals(user.getUsername());
    }
}
