package com.boyi.skillops.service;

import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
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
public class SkillServiceTest {

    @Autowired private SkillService skillService;
    @Autowired private UserService userService;
    @Autowired private AdminService adminService;
    @Autowired private SkillMapper skillMapper;
    @Autowired private UserMapper userMapper;

    private Long userAId;
    private String userASuffix;

    @BeforeEach
    void setUp() {
        userASuffix = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest req;
        req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("skilla_" + userASuffix);
        req.setPassword("123456");
        userService.register(req);
        User userA = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "skilla_" + userASuffix));
        userAId = userA.getId();
    }

    private SkillVO createDraftSkill(Long authorId, String suffix) {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("TestSkill_" + suffix);
        req.setDescription("Test description");
        req.setCategoryId(1L);
        req.setRepoUrl("https://github.com/test/repo");
        req.setDocUrl("https://docs.test.com");
        return skillService.create(req, authorId);
    }

    // ========== testCreateSkill ==========
    @Test
    void testCreateSkill() {
        SkillVO vo = createDraftSkill(userAId, "create_" + userASuffix);
        assertThat(vo.getStatus()).isEqualTo("DRAFT");
        assertThat(vo.getAuthorId()).isEqualTo(userAId);
        assertThat(vo.getName()).contains("TestSkill_create");
    }

    // ========== testSubmitForApproval ==========
    @Test
    void testSubmitForApproval() {
        SkillVO vo = createDraftSkill(userAId, "submit_" + userASuffix);
        skillService.submitForApproval(vo.getId(), userAId);
        Skill skill = skillMapper.selectById(vo.getId());
        assertThat(skill.getStatus()).isEqualTo("PENDING_APPROVAL");
    }

    // ========== testSubmitNonDraftFails ==========
    @Test
    void testSubmitNonDraftFails() {
        SkillVO vo = createDraftSkill(userAId, "doublesub_" + userASuffix);
        skillService.submitForApproval(vo.getId(), userAId);
        // now PENDING_APPROVAL, submit again should fail
        BusinessException ex = assertThrows(BusinessException.class,
                () -> skillService.submitForApproval(vo.getId(), userAId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40002);
    }

    // ========== testUpdateByNonAuthorFails ==========
    @Test
    void testUpdateByNonAuthorFails() {
        SkillVO vo = createDraftSkill(userAId, "updateauth_" + userASuffix);

        // register userB
        String userBSuffix = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("skillb_" + userBSuffix);
        req.setPassword("123456");
        userService.register(req);
        User userB = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "skillb_" + userBSuffix));
        Long userBId = userB.getId();

        SkillCreateRequest updReq = new SkillCreateRequest();
        updReq.setName("Hacked"); updReq.setDescription("X"); updReq.setCategoryId(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> skillService.update(vo.getId(), updReq, userBId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40301);
    }

    // ========== testPublishVersion ==========
    @Test
    void testPublishVersion() {
        SkillVO vo = createDraftSkill(userAId, "pubver_" + userASuffix);
        skillService.submitForApproval(vo.getId(), userAId);

        // AdminService.approve() has no role check at service layer; any userId works as auditor
        adminService.approve(vo.getId(), userAId);

        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0.0");
        vReq.setChangelog("Initial release");
        VersionVO vvo = skillService.publishVersion(vo.getId(), vReq, userAId);
        assertThat(vvo.getVersion()).isEqualTo("1.0.0");
        assertThat(vvo.getSkillId()).isEqualTo(vo.getId());

        // verify version count
        com.boyi.skillops.common.PageResult<VersionVO> versions = skillService.getVersions(vo.getId(), 1, 10);
        assertThat(versions.getTotal()).isEqualTo(1);
    }

    // ========== testGetMySkills ==========
    @Test
    void testGetMySkills() {
        SkillVO v1 = createDraftSkill(userAId, "myskills1_" + userASuffix);
        SkillVO v2 = createDraftSkill(userAId, "myskills2_" + userASuffix);

        // register userB and create a skill
        String userBSuffix = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("skillc_" + userBSuffix);
        req.setPassword("123456");
        userService.register(req);
        User userB = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "skillc_" + userBSuffix));
        createDraftSkill(userB.getId(), "mine_" + userBSuffix);

        com.boyi.skillops.common.PageResult<SkillVO> mySkills = skillService.getMySkills(userAId, 1, 10);
        assertThat(mySkills.getTotal()).isEqualTo(2);
        for (SkillVO sv : mySkills.getRecords()) {
            assertThat(sv.getAuthorId()).isEqualTo(userAId);
        }
    }
}
