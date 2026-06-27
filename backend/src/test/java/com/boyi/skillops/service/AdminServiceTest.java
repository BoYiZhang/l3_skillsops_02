package com.boyi.skillops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boyi.skillops.dto.RegisterRequest;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.entity.Role;
import com.boyi.skillops.entity.Skill;
import com.boyi.skillops.entity.SkillAudit;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.SkillAuditMapper;
import com.boyi.skillops.mapper.SkillMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.vo.AdminStatsVO;
import com.boyi.skillops.vo.SkillVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private SkillAuditMapper skillAuditMapper;

    private Long userId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        // Register regular user
        RegisterRequest userRequest = new RegisterRequest();
        userRequest.setUsername("admintestuser");
        userRequest.setPassword("password123");
        userRequest.setEmail("admintestuser@test.com");
        userService.register(userRequest);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admintestuser"));
        userId = user.getId();

        // Register admin user
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("admintestadmin");
        adminRequest.setPassword("password123");
        adminRequest.setEmail("admintestadmin@test.com");
        userService.register(adminRequest);

        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admintestadmin"));
        adminId = admin.getId();

        // Assign ADMIN role to admin user
        Role adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getName, "ADMIN"));
        UserRole userRole = new UserRole();
        userRole.setUserId(adminId);
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);
    }

    @Test
    void testApproveSkill() {
        // Create skill and submit for approval
        SkillCreateRequest createRequest = new SkillCreateRequest();
        createRequest.setName("Test Approve Skill");
        createRequest.setDescription("Description for approve test");
        createRequest.setCategoryId(1L);

        SkillVO skillVO = skillService.create(createRequest, userId);
        Long skillId = skillVO.getId();

        skillService.submitForApproval(skillId, userId);

        // Approve the skill
        adminService.approve(skillId, adminId);

        // Verify status is PUBLISHED
        Skill skill = skillMapper.selectById(skillId);
        assertEquals("PUBLISHED", skill.getStatus());

        // Verify SkillAudit record exists with action=APPROVE
        SkillAudit audit = skillAuditMapper.selectOne(
                new LambdaQueryWrapper<SkillAudit>()
                        .eq(SkillAudit::getSkillId, skillId)
                        .eq(SkillAudit::getAction, "APPROVE"));
        assertNotNull(audit);
        assertEquals(adminId, audit.getAuditorId());
    }

    @Test
    void testRejectSkill() {
        // Create skill and submit for approval
        SkillCreateRequest createRequest = new SkillCreateRequest();
        createRequest.setName("Test Reject Skill");
        createRequest.setDescription("Description for reject test");
        createRequest.setCategoryId(1L);

        SkillVO skillVO = skillService.create(createRequest, userId);
        Long skillId = skillVO.getId();

        skillService.submitForApproval(skillId, userId);

        // Reject the skill
        adminService.reject(skillId, "bad quality", adminId);

        // Verify status is DRAFT
        Skill skill = skillMapper.selectById(skillId);
        assertEquals("DRAFT", skill.getStatus());

        // Verify SkillAudit record exists with action=REJECT and correct reason
        SkillAudit audit = skillAuditMapper.selectOne(
                new LambdaQueryWrapper<SkillAudit>()
                        .eq(SkillAudit::getSkillId, skillId)
                        .eq(SkillAudit::getAction, "REJECT"));
        assertNotNull(audit);
        assertEquals("bad quality", audit.getReason());
        assertEquals(adminId, audit.getAuditorId());
    }

    @Test
    void testApproveNonPendingFails() {
        // Create skill but do NOT submit — status stays DRAFT
        SkillCreateRequest createRequest = new SkillCreateRequest();
        createRequest.setName("Test Non-Pending Skill");
        createRequest.setDescription("Description for non-pending test");
        createRequest.setCategoryId(1L);

        SkillVO skillVO = skillService.create(createRequest, userId);
        Long skillId = skillVO.getId();

        // Attempt to approve a DRAFT skill — expect BusinessException with code 40002
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.approve(skillId, adminId));
        assertEquals(ErrorCode.INVALID_STATUS, ex.getErrorCode());
        assertEquals(40002, ex.getErrorCode().getCode());
    }

    @Test
    void testDelistSkill() {
        // Create skill, submit, approve (→ PUBLISHED)
        SkillCreateRequest createRequest = new SkillCreateRequest();
        createRequest.setName("Test Delist Skill");
        createRequest.setDescription("Description for delist test");
        createRequest.setCategoryId(1L);

        SkillVO skillVO = skillService.create(createRequest, userId);
        Long skillId = skillVO.getId();

        skillService.submitForApproval(skillId, userId);
        adminService.approve(skillId, adminId);

        // Verify approved first
        Skill approvedSkill = skillMapper.selectById(skillId);
        assertEquals("PUBLISHED", approvedSkill.getStatus());

        // Delist the skill
        adminService.delist(skillId, "violates policy", adminId);

        // Verify status is DELISTED
        Skill skill = skillMapper.selectById(skillId);
        assertEquals("DELISTED", skill.getStatus());

        // Verify SkillAudit record exists with action=DELIST
        SkillAudit audit = skillAuditMapper.selectOne(
                new LambdaQueryWrapper<SkillAudit>()
                        .eq(SkillAudit::getSkillId, skillId)
                        .eq(SkillAudit::getAction, "DELIST"));
        assertNotNull(audit);
        assertEquals("violates policy", audit.getReason());
        assertEquals(adminId, audit.getAuditorId());
    }

    @Test
    void testGetStats() {
        AdminStatsVO stats = adminService.getStats();

        assertNotNull(stats);
        assertNotNull(stats.getTotalSkills());
        assertTrue(stats.getTotalSkills() >= 0);
        assertNotNull(stats.getTotalUsers());
        assertTrue(stats.getTotalUsers() > 0);
        assertNotNull(stats.getTotalInstalls());
        assertTrue(stats.getTotalInstalls() >= 0);
    }
}
