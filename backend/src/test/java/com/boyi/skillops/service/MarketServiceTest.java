package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.MarketQueryRequest;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.vo.SkillVO;
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
public class MarketServiceTest {

    @Autowired private MarketService marketService;
    @Autowired private SkillService skillService;
    @Autowired private UserService userService;
    @Autowired private AdminService adminService;
    @Autowired private SkillMapper skillMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private SkillInstallMapper installMapper;

    private Long userId;
    private String classSuffix;

    @BeforeEach
    void setUp() {
        classSuffix = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("mktuser_" + classSuffix);
        req.setPassword("123456");
        userService.register(req);
        userId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "mktuser_" + classSuffix)).getId();
    }

    private Long setupPublishedSkill(String suffix) {
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("PubSkill_" + suffix);
        sReq.setDescription("Published skill " + suffix);
        sReq.setCategoryId(1L);
        sReq.setRepoUrl("https://github.com/pub/" + suffix);
        SkillVO vo = skillService.create(sReq, userId);
        skillService.submitForApproval(vo.getId(), userId);
        adminService.approve(vo.getId(), userId); // service layer has no role check
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0.0");
        vReq.setChangelog("First version");
        skillService.publishVersion(vo.getId(), vReq, userId);
        return vo.getId();
    }

    // ========== testQueryMarketOnlyPublished ==========
    @Test
    void testQueryMarketOnlyPublished() {
        setupPublishedSkill("onlypub1_" + classSuffix);
        // create a DRAFT skill that should not appear
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("DraftSkill_" + classSuffix);
        sReq.setDescription("Should not show");
        sReq.setCategoryId(1L);
        skillService.create(sReq, userId);

        MarketQueryRequest mReq = new MarketQueryRequest();
        PageResult<SkillVO> result = marketService.queryMarket(mReq);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("PUBLISHED");
    }

    // ========== testQueryByCategory ==========
    @Test
    void testQueryByCategory() {
        setupPublishedSkill("cat-" + classSuffix); // categoryId=1

        // create a second PUBLISHED skill with categoryId=2 (must use separate user for unique name)
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("Cat2Skill_" + classSuffix);
        sReq.setDescription("Category 2 skill");
        sReq.setCategoryId(2L);
        SkillVO vo2 = skillService.create(sReq, userId);
        skillService.submitForApproval(vo2.getId(), userId);
        adminService.approve(vo2.getId(), userId);
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0");
        skillService.publishVersion(vo2.getId(), vReq, userId);

        MarketQueryRequest mReq = new MarketQueryRequest();
        mReq.setCategoryId(2L);
        PageResult<SkillVO> result = marketService.queryMarket(mReq);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getCategoryId()).isEqualTo(2L);
    }

    // ========== testQueryByKeyword ==========
    @Test
    void testQueryByKeyword() {
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("UniqueAlphaName");
        sReq.setDescription("This has unique-beta in description");
        sReq.setCategoryId(1L);
        SkillVO vo = skillService.create(sReq, userId);
        skillService.submitForApproval(vo.getId(), userId);
        adminService.approve(vo.getId(), userId);
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0");
        skillService.publishVersion(vo.getId(), vReq, userId);

        MarketQueryRequest mReq = new MarketQueryRequest();
        mReq.setKeyword("UniqueAlphaName");
        PageResult<SkillVO> result = marketService.queryMarket(mReq);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getName()).contains("UniqueAlphaName");
    }

    // ========== testInstall ==========
    @Test
    void testInstall() {
        Long skillId = setupPublishedSkill("install_" + classSuffix);
        // register a second user to install
        String suffix2 = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest r2 = new com.boyi.skillops.dto.RegisterRequest();
        r2.setUsername("installer_" + suffix2);
        r2.setPassword("123456");
        userService.register(r2);
        Long installerId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "installer_" + suffix2)).getId();

        marketService.install(skillId, installerId);
        Skill skill = skillMapper.selectById(skillId);
        assertThat(skill.getInstallCount()).isGreaterThanOrEqualTo(1);

        SkillInstall inst = installMapper.selectOne(new LambdaQueryWrapper<SkillInstall>()
                .eq(SkillInstall::getUserId, installerId)
                .eq(SkillInstall::getSkillId, skillId));
        assertThat(inst).isNotNull();
    }

    // ========== testInstallNonPublishedFails ==========
    @Test
    void testInstallNonPublishedFails() {
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("DraftForInstall_" + classSuffix);
        sReq.setDescription("Draft skill");
        sReq.setCategoryId(1L);
        SkillVO vo = skillService.create(sReq, userId);
        // stay DRAFT, no submit

        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(vo.getId(), userId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(404);
    }

    // ========== testInstallDuplicateFails ==========
    @Test
    void testInstallDuplicateFails() {
        Long skillId = setupPublishedSkill("dupinstall_" + classSuffix);
        String suffix2 = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest r2 = new com.boyi.skillops.dto.RegisterRequest();
        r2.setUsername("dupinstaller_" + suffix2);
        r2.setPassword("123456");
        userService.register(r2);
        Long installerId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "dupinstaller_" + suffix2)).getId();

        marketService.install(skillId, installerId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(skillId, installerId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40004);
    }
}
