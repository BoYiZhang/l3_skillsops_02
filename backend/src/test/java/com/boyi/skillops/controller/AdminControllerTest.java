package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.entity.Category;
import com.boyi.skillops.entity.Role;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.mapper.CategoryMapper;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.vo.AdminStatsVO;
import com.boyi.skillops.vo.CategoryVO;
import com.boyi.skillops.vo.SkillVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AdminService adminService;

    @MockBean
    private CategoryMapper categoryMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private UserRoleMapper userRoleMapper;

    @MockBean
    private RoleMapper roleMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.generateToken(1L, "admin", Arrays.asList("ADMIN", "USER"));
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
    }

    // ==================== Categories CRUD (delegates to CategoryMapper directly) ====================

    @Test
    void testListCategoriesSuccess() throws Exception {
        Category c1 = new Category();
        c1.setId(1L);
        c1.setName("Automation");
        c1.setDescription("Automation tools");

        Category c2 = new Category();
        c2.setId(2L);
        c2.setName("AI");
        c2.setDescription("AI tools");

        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2));

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Automation"))
                .andExpect(jsonPath("$.data[0].description").value("Automation tools"))
                .andExpect(jsonPath("$.data[1].name").value("AI"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testListCategoriesEmpty() throws Exception {
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== Approval endpoints ====================

    @Test
    void testApproveSuccess() throws Exception {
        doNothing().when(adminService).approve(1L, 1L);

        mockMvc.perform(post("/api/v1/admin/skills/1/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService).approve(1L, 1L);
    }

    @Test
    void testRejectSuccess() throws Exception {
        doNothing().when(adminService).reject(1L, "bad quality", 1L);

        mockMvc.perform(post("/api/v1/admin/skills/1/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"bad quality\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService).reject(1L, "bad quality", 1L);
    }

    @Test
    void testDelistSuccess() throws Exception {
        doNothing().when(adminService).delist(1L, "violation", 1L);

        mockMvc.perform(post("/api/v1/admin/skills/1/delist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"violation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService).delist(1L, "violation", 1L);
    }

    @Test
    void testRejectMissingReason() throws Exception {
        doNothing().when(adminService).reject(1L, null, 1L);

        mockMvc.perform(post("/api/v1/admin/skills/1/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService).reject(1L, null, 1L);
    }

    @Test
    void testApproveUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skills/1/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ==================== Pending Skills ====================

    @Test
    void testPendingSkillsSuccess() throws Exception {
        SkillVO vo = new SkillVO();
        vo.setId(1L);
        vo.setName("PendingSkill");
        vo.setStatus("PENDING");

        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(vo));
        pageResult.setTotal(1L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(adminService.getPendingSkills(1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("PendingSkill"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testPendingSkillsPagination() throws Exception {
        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(0L);
        pageResult.setSize(5L);
        pageResult.setCurrent(2L);

        when(adminService.getPendingSkills(2, 5)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .param("page", "2")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    // ==================== Stats ====================

    @Test
    void testStatsSuccess() throws Exception {
        AdminStatsVO stats = new AdminStatsVO();
        stats.setTotalSkills(100L);
        stats.setTotalUsers(50L);
        stats.setTotalInstalls(300L);
        stats.setAvgRating(4.2);

        AdminStatsVO.TrendItem trendItem = new AdminStatsVO.TrendItem("2026-01", 15L);
        stats.setInstallTrend(Arrays.asList(trendItem));

        AdminStatsVO.AuthorStat authorStat = new AdminStatsVO.AuthorStat(1L, "admin", 10L);
        stats.setTopAuthors(Arrays.asList(authorStat));

        AdminStatsVO.AuditSummary auditSummary = new AdminStatsVO.AuditSummary(5L, 80L, 3L, 12L);
        stats.setAuditSummary(auditSummary);

        when(adminService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSkills").value(100))
                .andExpect(jsonPath("$.data.totalUsers").value(50))
                .andExpect(jsonPath("$.data.totalInstalls").value(300))
                .andExpect(jsonPath("$.data.avgRating").value(4.2))
                .andExpect(jsonPath("$.data.installTrend[0].date").value("2026-01"))
                .andExpect(jsonPath("$.data.installTrend[0].count").value(15))
                .andExpect(jsonPath("$.data.topAuthors[0].authorName").value("admin"))
                .andExpect(jsonPath("$.data.auditSummary.pending").value(5))
                .andExpect(jsonPath("$.data.auditSummary.published").value(80))
                .andExpect(jsonPath("$.data.auditSummary.delisted").value(3))
                .andExpect(jsonPath("$.data.auditSummary.draft").value(12));
    }

    // ==================== Category Management (via adminService) ====================

    @Test
    void testCreateCategorySuccess() throws Exception {
        CategoryVO vo = new CategoryVO();
        vo.setId(1L);
        vo.setName("AI");
        vo.setDescription("AI tools");

        when(adminService.createCategory("AI", "AI tools")).thenReturn(vo);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AI\",\"description\":\"AI tools\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("AI"))
                .andExpect(jsonPath("$.data.description").value("AI tools"));

        verify(adminService).createCategory("AI", "AI tools");
    }

    @Test
    void testUpdateCategorySuccess() throws Exception {
        CategoryVO vo = new CategoryVO();
        vo.setId(1L);
        vo.setName("Updated");
        vo.setDescription("Updated description");

        when(adminService.updateCategory(1L, "Updated", "Updated description")).thenReturn(vo);

        mockMvc.perform(put("/api/v1/admin/categories/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"description\":\"Updated description\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated"))
                .andExpect(jsonPath("$.data.description").value("Updated description"));

        verify(adminService).updateCategory(1L, "Updated", "Updated description");
    }

    @Test
    void testDeleteCategorySuccess() throws Exception {
        doNothing().when(adminService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/admin/categories/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService).deleteCategory(1L);
    }

    // ==================== User Management ====================

    @Test
    void testListUsersSuccess() throws Exception {
        // Build mock users
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        user1.setStatus("ACTIVE");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        user2.setStatus("ACTIVE");

        Page<User> userPage = new Page<>(1, 20);
        userPage.setRecords(Arrays.asList(user1, user2));
        userPage.setTotal(2L);
        userPage.setSize(20L);
        userPage.setCurrent(1L);

        // Mock userRoleMapper: alice has ADMIN role, bob has USER role
        UserRole ur1 = new UserRole();
        ur1.setId(1L);
        ur1.setUserId(1L);
        ur1.setRoleId(1L);

        UserRole ur2 = new UserRole();
        ur2.setId(2L);
        ur2.setUserId(2L);
        ur2.setRoleId(2L);

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");

        when(userMapper.selectPage(any(Page.class), any()))
                .thenReturn(userPage);
        when(userRoleMapper.selectList(any()))
                .thenReturn(Arrays.asList(ur1), Arrays.asList(ur2));
        when(roleMapper.selectById(1L)).thenReturn(adminRole);
        when(roleMapper.selectById(2L)).thenReturn(userRole);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("alice"))
                .andExpect(jsonPath("$.data.records[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.records[0].roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.records[1].username").value("bob"))
                .andExpect(jsonPath("$.data.records[1].roles[0]").value("USER"));
    }

    @Test
    void testListUsersEmpty() throws Exception {
        Page<User> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(0L);
        emptyPage.setSize(20L);
        emptyPage.setCurrent(1L);

        when(userMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    @Test
    void testToggleUserStatusSuccess() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setStatus("ACTIVE");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(put("/api/v1/admin/users/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testToggleUserStatusNotFound() throws Exception {
        when(userMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/admin/users/999/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    // ==================== Auth: Non-admin access ====================

    @Test
    void testNonAdminAccess() throws Exception {
        // User with only USER role should get 403 on any admin endpoint
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNonAdminAccessPendingSkills() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNonAdminAccessUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNonAdminAccessStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
