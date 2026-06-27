package com.boyi.skillops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.MarketQueryRequest;
import com.boyi.skillops.dto.RegisterRequest;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.Role;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.SkillInstallMapper;
import com.boyi.skillops.mapper.SkillMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.vo.SkillVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MarketServiceTest {

    @Autowired
    private MarketService marketService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private SkillInstallMapper skillInstallMapper;

    private Long adminId;
    private Long userId;

    @BeforeEach
    void setUp() {
        // Register a regular user for use in tests
        RegisterRequest userReq = new RegisterRequest();
        userReq.setUsername("mktuser_" + System.nanoTime());
        userReq.setPassword("password123");
        userReq.setEmail("mktuser@test.com");
        userService.register(userReq);
        User regularUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, userReq.getUsername()));
        userId = regularUser.getId();

        // Register an admin user and assign ADMIN role
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("mktadmin_" + System.nanoTime());
        adminReq.setPassword("password123");
        adminReq.setEmail("mktadmin@test.com");
        userService.register(adminReq);
        User adminUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, adminReq.getUsername()));
        adminId = adminUser.getId();

        // Assign ADMIN role to the admin user (register() only grants USER role)
        Role adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getName, "ADMIN"));
        UserRole adminUserRole = new UserRole();
        adminUserRole.setUserId(adminId);
        adminUserRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(adminUserRole);
    }

    /**
     * Creates a fully PUBLISHED skill with a version attached.
     * Flow: create -> submitForApproval -> approve -> publishVersion
     */
    private Long setupPublishedSkill(String name, String description, Long categoryId) {
        SkillCreateRequest createReq = new SkillCreateRequest();
        createReq.setName(name);
        createReq.setDescription(description);
        createReq.setCategoryId(categoryId);
        createReq.setRepoUrl("https://github.com/test/repo");
        createReq.setDocUrl("https://docs.test.com");

        SkillVO skillVO = skillService.create(createReq, userId);
        Long skillId = skillVO.getId();

        skillService.submitForApproval(skillId, userId);
        adminService.approve(skillId, adminId);

        VersionCreateRequest versionReq = new VersionCreateRequest();
        versionReq.setVersion("1.0.0");
        versionReq.setChangelog("Initial release");
        skillService.publishVersion(skillId, versionReq, userId);

        return skillId;
    }

    /**
     * Creates a skill in DRAFT state only (no submission or approval).
     */
    private Long setupDraftSkill(String name, String description, Long categoryId) {
        SkillCreateRequest createReq = new SkillCreateRequest();
        createReq.setName(name);
        createReq.setDescription(description);
        createReq.setCategoryId(categoryId);
        createReq.setRepoUrl("https://github.com/test/repo");
        createReq.setDocUrl("https://docs.test.com");

        SkillVO skillVO = skillService.create(createReq, userId);
        return skillVO.getId();
    }

    @Test
    void testQueryMarketOnlyPublished() {
        // Create one PUBLISHED skill and one DRAFT skill
        Long publishedSkillId = setupPublishedSkill(
                "Published Skill Alpha", "A published skill for market", 1L);
        Long draftSkillId = setupDraftSkill(
                "Draft Skill Beta", "A draft skill not in market", 1L);

        MarketQueryRequest req = new MarketQueryRequest();
        PageResult<SkillVO> result = marketService.queryMarket(req);

        List<SkillVO> records = result.getRecords();
        boolean hasPublished = records.stream().anyMatch(vo -> vo.getId().equals(publishedSkillId));
        boolean hasDraft = records.stream().anyMatch(vo -> vo.getId().equals(draftSkillId));

        assertTrue(hasPublished, "queryMarket() must include PUBLISHED skill");
        assertFalse(hasDraft, "queryMarket() must exclude DRAFT skill");
    }

    @Test
    void testQueryByCategory() {
        // Create two PUBLISHED skills in different categories
        Long skillCat1Id = setupPublishedSkill("Category1 Skill", "Skill in category 1", 1L);
        Long skillCat2Id = setupPublishedSkill("Category2 Skill", "Skill in category 2", 2L);

        MarketQueryRequest req = new MarketQueryRequest();
        req.setCategoryId(1L);
        PageResult<SkillVO> result = marketService.queryMarket(req);

        List<SkillVO> records = result.getRecords();
        boolean hasSkillCat1 = records.stream().anyMatch(vo -> vo.getId().equals(skillCat1Id));
        boolean hasSkillCat2 = records.stream().anyMatch(vo -> vo.getId().equals(skillCat2Id));

        assertTrue(hasSkillCat1, "queryMarket() with categoryId=1 must return skill in category 1");
        assertFalse(hasSkillCat2, "queryMarket() with categoryId=1 must not return skill in category 2");
    }

    @Test
    void testQueryByKeyword() {
        // Create two PUBLISHED skills with distinct keywords in their names
        Long matchingSkillId = setupPublishedSkill(
                "UniqueKeyword_XYZ Automation Tool",
                "A skill that automates tasks",
                3L);
        Long nonMatchingSkillId = setupPublishedSkill(
                "Completely Different Workflow",
                "An entirely separate description here",
                2L);

        MarketQueryRequest req = new MarketQueryRequest();
        req.setKeyword("UniqueKeyword_XYZ");
        PageResult<SkillVO> result = marketService.queryMarket(req);

        List<SkillVO> records = result.getRecords();
        boolean hasMatching = records.stream().anyMatch(vo -> vo.getId().equals(matchingSkillId));
        boolean hasNonMatching = records.stream().anyMatch(vo -> vo.getId().equals(nonMatchingSkillId));

        assertTrue(hasMatching, "queryMarket() with keyword should return matching skill");
        assertFalse(hasNonMatching, "queryMarket() with keyword should not return non-matching skill");
    }

    @Test
    void testInstall() {
        Long skillId = setupPublishedSkill("Installable Skill", "A skill ready to install", 1L);

        // Install should not throw
        assertDoesNotThrow(() -> marketService.install(skillId, userId));

        // installCount should have been incremented
        Long installCount = skillMapper.selectById(skillId).getInstallCount();
        assertTrue(installCount >= 1,
                "installCount should be >= 1 after a successful install, was: " + installCount);
    }

    @Test
    void testInstallNonPublishedFails() {
        // Create a DRAFT skill (no approval) — install must fail with NOT_FOUND
        Long draftSkillId = setupDraftSkill("Draft Only Skill", "This skill is never published", 1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(draftSkillId, userId));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getErrorCode().getCode(),
                "Installing a non-PUBLISHED skill must throw BusinessException with code NOT_FOUND (404)");
    }

    @Test
    void testInstallDuplicateFails() {
        Long skillId = setupPublishedSkill("Duplicate Install Skill", "Install this once only", 1L);

        // Register a second user to avoid conflicts with userId already set up
        RegisterRequest secondUserReq = new RegisterRequest();
        secondUserReq.setUsername("dupuser_" + System.nanoTime());
        secondUserReq.setPassword("password123");
        secondUserReq.setEmail("dupuser@test.com");
        userService.register(secondUserReq);
        User secondUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, secondUserReq.getUsername()));
        Long secondUserId = secondUser.getId();

        // First install should succeed
        assertDoesNotThrow(() -> marketService.install(skillId, secondUserId));

        // Second install by same user must throw ALREADY_INSTALLED
        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(skillId, secondUserId));

        assertEquals(ErrorCode.ALREADY_INSTALLED.getCode(), ex.getErrorCode().getCode(),
                "Re-installing the same skill must throw BusinessException with code ALREADY_INSTALLED (40004)");
    }
}
