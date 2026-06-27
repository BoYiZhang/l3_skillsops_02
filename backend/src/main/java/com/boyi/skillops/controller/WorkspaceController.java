package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.common.Result;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.vo.InstallVO;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.entity.SkillInstall;
import com.boyi.skillops.entity.Skill;
import com.boyi.skillops.entity.SkillVersion;
import com.boyi.skillops.mapper.SkillInstallMapper;
import com.boyi.skillops.mapper.SkillMapper;
import com.boyi.skillops.mapper.SkillVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    @Autowired private SkillService skillService;
    @Autowired private SkillInstallMapper installMapper;
    @Autowired private SkillMapper skillMapper;
    @Autowired private SkillVersionMapper versionMapper;

    @GetMapping("/my-skills")
    public Result<PageResult<SkillVO>> mySkills(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return Result.success(skillService.getMySkills(userId, page, size));
    }

    @GetMapping("/installed")
    public Result<PageResult<InstallVO>> installed(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        Page<SkillInstall> p = new Page<>(page, size);
        Page<SkillInstall> result = installMapper.selectPage(p,
                new LambdaQueryWrapper<SkillInstall>().eq(SkillInstall::getUserId, userId).orderByDesc(SkillInstall::getCreateTime));
        List<InstallVO> vos = result.getRecords().stream().map(i -> {
            InstallVO vo = new InstallVO();
            vo.setId(i.getId()); vo.setSkillId(i.getSkillId());
            Skill skill = skillMapper.selectById(i.getSkillId());
            if (skill != null) {
                vo.setSkillName(skill.getName());
                vo.setSkillDescription(skill.getDescription());
                vo.setStatus(skill.getStatus());
            }
            SkillVersion v = versionMapper.selectById(i.getVersionId());
            if (v != null) vo.setVersion(v.getVersion());
            vo.setCreateTime(i.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        PageResult<InstallVO> pr = new PageResult<>();
        pr.setRecords(vos); pr.setTotal(result.getTotal());
        pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return Result.success(pr);
    }
}
