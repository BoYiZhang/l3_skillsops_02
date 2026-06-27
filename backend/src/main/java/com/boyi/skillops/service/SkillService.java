package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface SkillService {
    SkillVO create(SkillCreateRequest request, Long authorId);
    SkillVO update(Long skillId, SkillCreateRequest request, Long userId);
    SkillVO getById(Long skillId);
    void submitForApproval(Long skillId, Long userId);
    VersionVO publishVersion(Long skillId, VersionCreateRequest request, Long userId);
    PageResult<SkillVO> getMySkills(Long userId, int page, int size);
    PageResult<VersionVO> getVersions(Long skillId, int page, int size);
}
