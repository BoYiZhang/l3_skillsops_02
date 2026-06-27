package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.vo.AdminStatsVO;
import com.boyi.skillops.vo.CategoryVO;
import com.boyi.skillops.vo.SkillVO;

public interface AdminService {
    void approve(Long skillId, Long auditorId);
    void reject(Long skillId, String reason, Long auditorId);
    void delist(Long skillId, String reason, Long auditorId);
    PageResult<SkillVO> getPendingSkills(int page, int size);
    AdminStatsVO getStats();
    CategoryVO createCategory(String name, String description);
    CategoryVO updateCategory(Long id, String name, String description);
    void deleteCategory(Long id);
}
