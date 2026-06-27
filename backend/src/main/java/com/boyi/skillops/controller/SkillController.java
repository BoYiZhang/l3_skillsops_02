package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.common.Result;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    @Autowired private SkillService skillService;

    @PostMapping
    public Result<SkillVO> create(@Valid @RequestBody SkillCreateRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return Result.success(skillService.create(request, userId));
    }

    @PutMapping("/{id}")
    public Result<SkillVO> update(@PathVariable Long id, @Valid @RequestBody SkillCreateRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return Result.success(skillService.update(id, request, userId));
    }

    @GetMapping("/{id}")
    public Result<SkillVO> getById(@PathVariable Long id) {
        return Result.success(skillService.getById(id));
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        skillService.submitForApproval(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/versions")
    public Result<VersionVO> publishVersion(@PathVariable Long id, @Valid @RequestBody VersionCreateRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return Result.success(skillService.publishVersion(id, request, userId));
    }

    @GetMapping("/{id}/versions")
    public Result<PageResult<VersionVO>> getVersions(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(skillService.getVersions(id, page, size));
    }
}
