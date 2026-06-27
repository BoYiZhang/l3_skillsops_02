package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.RegisterRequest;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.SkillMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boyi.skillops.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SkillServiceTest {

    @Autowired
    private SkillService skillService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Register a plain user and return their DB id. */
    private Long registerUser(String username) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword("Password123!");
        req.setEmail(username + "@test.com");
        userService.register(req);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        assertNotNull(user, "User should exist after registration: " + username);
        return user.getId();
    }

    /**
     * Register a user with username="admin" (or a given username) and manually
     * assign ADMIN role (role_id=1 per seed data) so isAdmin() returns true.
     */
    private Long registerAdmin(String username) {
        Long adminId = registerUser(username);
        UserRole userRole = new UserRole();
        userRole.setUserId(adminId);
        userRole.setRoleId(1L); // ADMIN role from V2__seed_data.sql
        userRoleMapper.insert(userRole);
        return adminId;
    }

    /** Build a minimal valid SkillCreateRequest. */
    private SkillCreateRequest buildSkillRequest(String name) {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName(name);
        req.setDescription("Test description for " + name);
        req.setCategoryId(1L); // categories 1..5 from seed data
        return req;
    }

    /** Build a minimal valid VersionCreateRequest. */
    private VersionCreateRequest buildVersionRequest(String version) {
        VersionCreateRequest req = new VersionCreateRequest();
        req.setVersion(version);
        req.setChangelog("Initial release " + version);
        return req;
    }

    // -----------------------------------------------------------------------
    // Test 1: create skill → status=DRAFT, authorId matches
    // -----------------------------------------------------------------------

    @Test
    public void testCreateSkill() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long userId = registerUser("creator_" + suffix);

        SkillVO skill = skillService.create(buildSkillRequest("MySkill_" + suffix), userId);

        assertNotNull(skill);
        assertNotNull(skill.getId());
        assertEquals("DRAFT", skill.getStatus(), "New skill should have status DRAFT");
        assertEquals(userId, skill.getAuthorId(), "authorId should match the registering user");
        assertEquals("MySkill_" + suffix, skill.getName());
    }

    // -----------------------------------------------------------------------
    // Test 2: create skill, submit → status becomes PENDING_APPROVAL
    // -----------------------------------------------------------------------

    @Test
    public void testSubmitForApproval() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long userId = registerUser("submitter_" + suffix);

        SkillVO skill = skillService.create(buildSkillRequest("SubmitSkill_" + suffix), userId);
        assertEquals("DRAFT", skill.getStatus());

        skillService.submitForApproval(skill.getId(), userId);

        SkillVO updated = skillService.getById(skill.getId());
        assertEquals("PENDING_APPROVAL", updated.getStatus(),
                "Status should be PENDING_APPROVAL after submitForApproval");
    }

    // -----------------------------------------------------------------------
    // Test 3: submit a non-DRAFT skill → BusinessException(40002)
    // -----------------------------------------------------------------------

    @Test
    public void testSubmitNonDraftFails() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long userId = registerUser("submitter2_" + suffix);

        SkillVO skill = skillService.create(buildSkillRequest("NonDraftSkill_" + suffix), userId);
        // First submit: DRAFT → PENDING_APPROVAL
        skillService.submitForApproval(skill.getId(), userId);

        // Second submit: PENDING_APPROVAL → should throw
        BusinessException ex = assertThrows(BusinessException.class,
                () -> skillService.submitForApproval(skill.getId(), userId));
        assertEquals(40002, ex.getErrorCode().getCode(),
                "Expected INVALID_STATUS error code 40002");
    }

    // -----------------------------------------------------------------------
    // Test 4: non-author update → BusinessException(40301)
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateByNonAuthorFails() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long userAId = registerUser("authorA_" + suffix);
        Long userBId = registerUser("authorB_" + suffix);

        SkillVO skill = skillService.create(buildSkillRequest("AuthorSkill_" + suffix), userAId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> skillService.update(skill.getId(),
                        buildSkillRequest("AuthorSkill_modified_" + suffix), userBId));
        assertEquals(40301, ex.getErrorCode().getCode(),
                "Expected NOT_AUTHOR error code 40301");
    }

    // -----------------------------------------------------------------------
    // Test 5: publish version on a PUBLISHED skill succeeds
    //         (create → submit → admin approves → publishVersion)
    // -----------------------------------------------------------------------

    @Test
    public void testPublishVersion() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        // Register a regular user as author
        Long authorId = registerUser("pvauthor_" + suffix);

        // Register admin: username must be "admin" for isAdmin() check in
        // SkillServiceImpl, but within this transaction we use a unique prefix
        // to avoid conflicts across parallel tests. We give the user the ADMIN
        // role explicitly via UserRoleMapper, which is what AdminServiceImpl
        // checks when calling approve().
        Long adminId = registerAdmin("admin_" + suffix);

        // Create and submit the skill
        SkillVO skill = skillService.create(buildSkillRequest("PVSkill_" + suffix), authorId);
        skillService.submitForApproval(skill.getId(), authorId);

        // Admin approves → status becomes PUBLISHED
        adminService.approve(skill.getId(), adminId);

        SkillVO published = skillService.getById(skill.getId());
        assertEquals("PUBLISHED", published.getStatus(),
                "Skill should be PUBLISHED after admin approval");

        // Now publish a version
        VersionVO version = skillService.publishVersion(
                skill.getId(), buildVersionRequest("1.0.0"), authorId);

        assertNotNull(version);
        assertNotNull(version.getId());
        assertEquals("1.0.0", version.getVersion());
        assertEquals(skill.getId(), version.getSkillId());
    }

    // -----------------------------------------------------------------------
    // Test 6: getMySkills returns only the requesting user's skills
    // -----------------------------------------------------------------------

    @Test
    public void testGetMySkills() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long userAId = registerUser("myskillsA_" + suffix);
        Long userBId = registerUser("myskillsB_" + suffix);

        // userA creates 2 skills
        skillService.create(buildSkillRequest("SkillA1_" + suffix), userAId);
        skillService.create(buildSkillRequest("SkillA2_" + suffix), userAId);

        // userB creates 1 skill
        skillService.create(buildSkillRequest("SkillB1_" + suffix), userBId);

        PageResult<SkillVO> result = skillService.getMySkills(userAId, 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getRecords().size(),
                "getMySkills should return exactly 2 skills for userA");
        result.getRecords().forEach(s ->
                assertEquals(userAId, s.getAuthorId(),
                        "All returned skills must belong to userA"));
    }
}
