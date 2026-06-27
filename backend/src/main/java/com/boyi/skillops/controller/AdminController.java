package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.common.Result;
import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.vo.CategoryVO;
import com.boyi.skillops.vo.SkillVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.boyi.skillops.entity.Category;
import com.boyi.skillops.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private CategoryMapper categoryMapper;

    @GetMapping("/categories")
    public Result<List<CategoryVO>> listCategories() {
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<>());
        List<CategoryVO> vos = list.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId()); vo.setName(c.getName()); vo.setDescription(c.getDescription());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }

    @PostMapping("/skills/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        adminService.approve(id, userId);
        return Result.success();
    }

    @PostMapping("/skills/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        adminService.reject(id, body.get("reason"), userId);
        return Result.success();
    }

    @PostMapping("/skills/{id}/delist")
    public Result<Void> delist(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        adminService.delist(id, body.get("reason"), userId);
        return Result.success();
    }

    @GetMapping("/pending-skills")
    public Result<PageResult<SkillVO>> pendingSkills(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(adminService.getPendingSkills(page, size));
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.success(adminService.getStats());
    }

    @PostMapping("/categories")
    public Result<CategoryVO> createCategory(@RequestBody Map<String, String> body) {
        return Result.success(adminService.createCategory(body.get("name"), body.get("description")));
    }

    @PutMapping("/categories/{id}")
    public Result<CategoryVO> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(adminService.updateCategory(id, body.get("name"), body.get("description")));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.success();
    }
}
