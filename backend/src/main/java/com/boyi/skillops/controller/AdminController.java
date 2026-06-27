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
import com.boyi.skillops.entity.User;
import com.boyi.skillops.mapper.CategoryMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.entity.UserRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private RoleMapper roleMapper;

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

    // ===== 用户管理 =====

    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> p = new Page<>(page, size);
        Page<User> result = userMapper.selectPage(p, new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<Map<String, Object>> vos = result.getRecords().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            List<UserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, u.getId()));
            m.put("roles", urs.stream().map(ur -> {
                com.boyi.skillops.entity.Role r = roleMapper.selectById(ur.getRoleId());
                return r != null ? r.getName() : "UNKNOWN";
            }).collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setRecords(vos); pr.setTotal(result.getTotal()); pr.setSize(result.getSize()); pr.setCurrent(result.getCurrent());
        return Result.success(pr);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.error(com.boyi.skillops.enums.ErrorCode.NOT_FOUND);
        user.setStatus(body.get("status")); // ACTIVE or DISABLED
        userMapper.updateById(user);
        return Result.success();
    }
}
