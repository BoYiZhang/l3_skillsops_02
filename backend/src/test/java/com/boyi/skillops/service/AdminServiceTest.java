package com.boyi.skillops.service;

import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.vo.AdminStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AdminServiceTest {

    @Autowired private AdminService adminService;
    @Autowired private SkillService skillService;
    @Autowired private UserService userService;
    @Autowired private SkillMapper skillMapper;
    @Autowired private SkillAuditMapper auditMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private SkillRatingMapper ratingMapper;
    @Autowired private SkillInstallMapper installMapper;

    private Long userId;
    private Long adminId;
    private String classSuffix;

    @BeforeEach
    void setUp() {
        classSuffix = String.valueOf(System.nanoTime());

        // Register regular user
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("admtestuser_" + classSuffix);
        req.setPassword("123456");
        userService.register(req);
        userId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admtestuser_" + classSuffix)).getId();

        // Register admin user
        com.boyi.skillops.dto.RegisterRequest areq = new com.boyi.skillops.dto.RegisterRequest();
        areq.setUsername("admtestadmin_" + classSuffix);
        areq.setPassword("admin123");
        userService.register(areq);
        adminId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admtestadmin_" + classSuffix)).getId();

        // Assign ADMIN role
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getName, "ADMIN"));
        UserRole ur = new UserRole();
        ur.setUserId(adminId);
        ur.setRoleId(adminRole.getId());
        userRoleMapper.insert(ur);
    }

    private Long createAndSubmitSkill() {
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("AdminSkill_" + classSuffix);
        sReq.setDescription("For admin operations");
        sReq.setCategoryId(1L);
        com.boyi.skillops.vo.SkillVO vo = skillService.create(sReq, userId);
        skillService.submitForApproval(vo.getId(), userId);
        return vo.getId();
    }

    // ========== testApproveSkill ==========
    @Test
    void testApproveSkill() {
        Long skillId = createAndSubmitSkill();
        adminService.approve(skillId, adminId);

        Skill skill = skillMapper.selectById(skillId);
        assertThat(skill.getStatus()).isEqualTo("PUBLISHED");

        SkillAudit audit = auditMapper.selectOne(new LambdaQueryWrapper<SkillAudit>()
                .eq(SkillAudit::getSkillId, skillId)
                .eq(SkillAudit::getAction, "APPROVE"));
        assertThat(audit).isNotNull();
        assertThat(audit.getAuditorId()).isEqualTo(adminId);
    }

    // ========== testRejectSkill ==========
    @Test
    void testRejectSkill() {
        Long skillId = createAndSubmitSkill();
        adminService.reject(skillId, "bad quality", adminId);

        Skill skill = skillMapper.selectById(skillId);
        assertThat(skill.getStatus()).isEqualTo("DRAFT");

        SkillAudit audit = auditMapper.selectOne(new LambdaQueryWrapper<SkillAudit>()
                .eq(SkillAudit::getSkillId, skillId)
                .eq(SkillAudit::getAction, "REJECT"));
        assertThat(audit).isNotNull();
        assertThat(audit.getReason()).isEqualTo("bad quality");
    }

    // ========== testApproveNonPendingFails ==========
    @Test
    void testApproveNonPendingFails() {
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("DraftSkill_" + classSuffix);
        sReq.setDescription("Never submitted");
        sReq.setCategoryId(1L);
        com.boyi.skillops.vo.SkillVO vo = skillService.create(sReq, userId);
        // stays DRAFT - no submitForApproval

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.approve(vo.getId(), adminId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40002);
    }

    // ========== testDelistSkill ==========
    @Test
    void testDelistSkill() {
        Long skillId = createAndSubmitSkill();
        adminService.approve(skillId, adminId); // now PUBLISHED
        adminService.delist(skillId, "violates policy", adminId);

        Skill skill = skillMapper.selectById(skillId);
        assertThat(skill.getStatus()).isEqualTo("DELISTED");

        SkillAudit audit = auditMapper.selectOne(new LambdaQueryWrapper<SkillAudit>()
                .eq(SkillAudit::getSkillId, skillId)
                .eq(SkillAudit::getAction, "DELIST"));
        assertThat(audit).isNotNull();
        assertThat(audit.getReason()).isEqualTo("violates policy");
    }

    // ========== testGetStats ==========
    @Test
    void testGetStats() {
        // create a skill and give it a rating to populate stats
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("StatsSkill_" + classSuffix);
        sReq.setDescription("For stats test");
        sReq.setCategoryId(1L);
        com.boyi.skillops.vo.SkillVO vo = skillService.create(sReq, userId);

        // insert a rating
        SkillRating rating = new SkillRating();
        rating.setSkillId(vo.getId());
        rating.setUserId(userId);
        rating.setRating(5);
        ratingMapper.insert(rating);

        // update skill avg_rating
        Skill skill = skillMapper.selectById(vo.getId());
        skill.setAvgRating(5.0);
        skillMapper.updateById(skill);

        // insert an install
        SkillInstall install = new SkillInstall();
        install.setSkillId(vo.getId());
        install.setUserId(userId);
        install.setVersionId(1L);
        installMapper.insert(install);

        AdminStatsVO stats = adminService.getStats();
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalSkills()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getTotalUsers()).isGreaterThanOrEqualTo(2);
        assertThat(stats.getTotalInstalls()).isGreaterThanOrEqualTo(1);

        // new fields
        assertThat(stats.getAvgRating()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.getInstallTrend()).isNotEmpty();
        assertThat(stats.getUserTrend()).isNotNull();
        assertThat(stats.getCategoryDistribution()).isNotNull();
        assertThat(stats.getRatingDistribution()).hasSize(5);
        assertThat(stats.getAuditSummary()).isNotNull();
        assertThat(stats.getAuditSummary().getDraft()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAuditSummary().getPending()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAuditSummary().getPublished()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAuditSummary().getDelisted()).isGreaterThanOrEqualTo(0);
    }
}
