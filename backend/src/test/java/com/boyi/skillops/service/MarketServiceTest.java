package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.MarketQueryRequest;
import com.boyi.skillops.dto.RegisterRequest;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MarketServiceTest {

    @Autowired private MarketService marketService;
    @Autowired private SkillService skillService;
    @Autowired private AdminService adminService;
    @Autowired private UserService userService;
    @Autowired private SkillMapper skillMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private SkillInstallMapper installMapper;
    @Autowired private SkillVersionMapper versionMapper;
    @Autowired private SkillRatingMapper ratingMapper;

    private Long userAId;
    private Long userBId;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());

        // 注册用户 A
        RegisterRequest reqA = new RegisterRequest();
        reqA.setUsername("mkt_a_" + suffix);
        reqA.setPassword("123456");
        userService.register(reqA);
        userAId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "mkt_a_" + suffix)).getId();

        // 注册用户 B
        RegisterRequest reqB = new RegisterRequest();
        reqB.setUsername("mkt_b_" + suffix);
        reqB.setPassword("123456");
        userService.register(reqB);
        userBId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "mkt_b_" + suffix)).getId();
    }

    /** 创建草稿 -> 提交审核 -> 审核通过 -> 发布版本 -> 返回已上架的 skillId */
    private Long createPublishedSkill(Long authorId, String nameSuffix) {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("MarketSkill_" + nameSuffix);
        req.setDescription("Desc for " + nameSuffix);
        req.setCategoryId(1L);
        req.setRepoUrl("https://github.com/test/" + nameSuffix);
        SkillVO vo = skillService.create(req, authorId);
        skillService.submitForApproval(vo.getId(), authorId);
        adminService.approve(vo.getId(), authorId);
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0.0");
        vReq.setChangelog("First release");
        skillService.publishVersion(vo.getId(), vReq, authorId);
        return vo.getId();
    }

    /** 创建草稿（未上架）*/
    private Long createDraftSkill(Long authorId, String nameSuffix) {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("DraftSkill_" + nameSuffix);
        req.setDescription("Draft desc");
        req.setCategoryId(1L);
        SkillVO vo = skillService.create(req, authorId);
        return vo.getId();
    }

    // ========== queryMarket ==========

    @Test
    void testQueryMarketEmpty() {
        MarketQueryRequest req = new MarketQueryRequest();
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getRecords()).isEmpty();
    }

    @Test
    void testQueryMarketWithPublishedSkills() {
        Long skillId = createPublishedSkill(userAId, "qm1_" + suffix);
        MarketQueryRequest req = new MarketQueryRequest();
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(skillId);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("PUBLISHED");
        assertThat(result.getRecords().get(0).getLatestVersion()).isEqualTo("1.0.0");
    }

    @Test
    void testQueryMarketDraftNotVisible() {
        // 创建草稿（不提交审核），不应在市场中出现
        Long draftId = createDraftSkill(userAId, "draftHidden_" + suffix);
        MarketQueryRequest req = new MarketQueryRequest();
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getRecords()).noneMatch(v -> v.getId().equals(draftId));
    }

    @Test
    void testQueryMarketFilterByCategory() {
        createPublishedSkill(userAId, "catA_" + suffix); // categoryId=1
        // 创建第二个技能在 categoryId=2
        SkillCreateRequest req2 = new SkillCreateRequest();
        req2.setName("MarketSkill_catB_" + suffix);
        req2.setDescription("Category B skill");
        req2.setCategoryId(2L);
        SkillVO vo2 = skillService.create(req2, userAId);
        skillService.submitForApproval(vo2.getId(), userAId);
        adminService.approve(vo2.getId(), userAId);
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0.0");
        skillService.publishVersion(vo2.getId(), vReq, userAId);

        MarketQueryRequest req = new MarketQueryRequest();
        req.setCategoryId(1L);
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getCategoryId()).isEqualTo(1L);
    }

    @Test
    void testQueryMarketFilterByKeyword() {
        createPublishedSkill(userAId, "PythonTool_" + suffix);
        createPublishedSkill(userAId, "JavaLib_" + suffix);

        MarketQueryRequest req = new MarketQueryRequest();
        req.setKeyword("Python");
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getName()).contains("Python");
    }

    @Test
    void testQueryMarketSortByHot() {
        Long skillId1 = createPublishedSkill(userAId, "hot1_" + suffix);
        Long skillId2 = createPublishedSkill(userAId, "hot2_" + suffix);

        // 直接修改 install_count 模拟热度差异（绕过 install 方法）
        Skill s1 = skillMapper.selectById(skillId1);
        s1.setInstallCount(100L);
        skillMapper.updateById(s1);
        Skill s2 = skillMapper.selectById(skillId2);
        s2.setInstallCount(10L);
        skillMapper.updateById(s2);

        MarketQueryRequest req = new MarketQueryRequest();
        req.setSortBy("HOT");
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords().get(0).getInstallCount()).isGreaterThanOrEqualTo(
                result.getRecords().get(1).getInstallCount());
    }

    @Test
    void testQueryMarketSortByRating() {
        Long skillId1 = createPublishedSkill(userAId, "rate1_" + suffix);
        Long skillId2 = createPublishedSkill(userAId, "rate2_" + suffix);

        Skill s1 = skillMapper.selectById(skillId1);
        s1.setAvgRating(4.5);
        skillMapper.updateById(s1);
        Skill s2 = skillMapper.selectById(skillId2);
        s2.setAvgRating(3.0);
        skillMapper.updateById(s2);

        MarketQueryRequest req = new MarketQueryRequest();
        req.setSortBy("RATING");
        PageResult<SkillVO> result = marketService.queryMarket(req, userAId);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords().get(0).getAvgRating()).isGreaterThanOrEqualTo(
                result.getRecords().get(1).getAvgRating());
    }

    @Test
    void testQueryMarketDelistedOnlyVisibleIfInstalled() {
        Long skillIdA = createPublishedSkill(userAId, "delA_" + suffix);
        Long skillIdB = createPublishedSkill(userAId, "delB_" + suffix);

        // 用户 A 安装 skill A
        marketService.install(skillIdA, userAId);

        // 下架两个 skill
        adminService.delist(skillIdA, "obsolete", userAId);
        adminService.delist(skillIdB, "obsolete", userAId);

        // 用户 A 查询：应看到已安装的 skill A（DELISTED 但已安装），不应看到 skill B
        MarketQueryRequest req = new MarketQueryRequest();
        PageResult<SkillVO> resultA = marketService.queryMarket(req, userAId);
        assertThat(resultA.getRecords()).anyMatch(v -> v.getId().equals(skillIdA));
        assertThat(resultA.getRecords()).noneMatch(v -> v.getId().equals(skillIdB));

        // 用户 B 查询：两个 DELISTED 技能都不可见
        PageResult<SkillVO> resultB = marketService.queryMarket(req, userBId);
        assertThat(resultB.getRecords()).noneMatch(v -> v.getId().equals(skillIdA));
        assertThat(resultB.getRecords()).noneMatch(v -> v.getId().equals(skillIdB));
    }

    @Test
    void testQueryMarketNullUserId() {
        createPublishedSkill(userAId, "noUser_" + suffix);
        MarketQueryRequest req = new MarketQueryRequest();
        PageResult<SkillVO> result = marketService.queryMarket(req, null);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void testQueryMarketPagination() {
        for (int i = 0; i < 5; i++) {
            createPublishedSkill(userAId, "page_" + i + "_" + suffix);
        }
        MarketQueryRequest req = new MarketQueryRequest();
        req.setPage(1);
        req.setSize(3);
        PageResult<SkillVO> page1 = marketService.queryMarket(req, userAId);
        assertThat(page1.getRecords()).hasSize(3);
        assertThat(page1.getTotal()).isEqualTo(5);

        req.setPage(2);
        PageResult<SkillVO> page2 = marketService.queryMarket(req, userAId);
        assertThat(page2.getRecords()).hasSize(2);
    }

    // ========== install ==========

    @Test
    void testInstallSuccess() {
        Long skillId = createPublishedSkill(userAId, "installOk_" + suffix);
        marketService.install(skillId, userAId);

        // 验证安装记录
        SkillInstall install = installMapper.selectOne(
                new LambdaQueryWrapper<SkillInstall>()
                        .eq(SkillInstall::getUserId, userAId)
                        .eq(SkillInstall::getSkillId, skillId));
        assertThat(install).isNotNull();
        assertThat(install.getVersionId()).isNotNull();

        // 验证安装计数自增
        Skill skill = skillMapper.selectById(skillId);
        assertThat(skill.getInstallCount()).isEqualTo(1);
    }

    @Test
    void testInstallNonPublishedSkillFails() {
        Long draftId = createDraftSkill(userAId, "installDraft_" + suffix);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(draftId, userAId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(404);
    }

    @Test
    void testInstallNonExistentSkillFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(99999L, userAId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(404);
    }

    @Test
    void testInstallAlreadyInstalledFails() {
        Long skillId = createPublishedSkill(userAId, "dupInstall_" + suffix);
        marketService.install(skillId, userAId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(skillId, userAId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40004);
    }

    @Test
    void testInstallNoVersionFails() {
        // 创建已上架但未发布版本的 skill
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("NoVersion_" + suffix);
        req.setDescription("No version");
        req.setCategoryId(1L);
        SkillVO vo = skillService.create(req, userAId);
        skillService.submitForApproval(vo.getId(), userAId);
        adminService.approve(vo.getId(), userAId);
        // 未发布版本

        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(vo.getId(), userAId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(404);
    }

    @Test
    void testInstallDelistedSkillFails() {
        Long skillId = createPublishedSkill(userAId, "delInstall_" + suffix);
        adminService.delist(skillId, "test", userAId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketService.install(skillId, userBId));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(404);
    }

    @Test
    void testInstallMultipleUsersInstallSameSkill() {
        Long skillId = createPublishedSkill(userAId, "multiUser_" + suffix);
        marketService.install(skillId, userAId);
        marketService.install(skillId, userBId);

        // 两个用户都有安装记录
        SkillInstall installA = installMapper.selectOne(
                new LambdaQueryWrapper<SkillInstall>()
                        .eq(SkillInstall::getUserId, userAId).eq(SkillInstall::getSkillId, skillId));
        SkillInstall installB = installMapper.selectOne(
                new LambdaQueryWrapper<SkillInstall>()
                        .eq(SkillInstall::getUserId, userBId).eq(SkillInstall::getSkillId, skillId));
        assertThat(installA).isNotNull();
        assertThat(installB).isNotNull();

        // 安装计数 = 2
        Skill skill = skillMapper.selectById(skillId);
        assertThat(skill.getInstallCount()).isEqualTo(2);
    }

    // ========== getInstallStatus ==========

    @Test
    void testGetInstallStatusNotInstalled() {
        Long skillId = createPublishedSkill(userAId, "statusNot_" + suffix);
        Map<String, Object> status = marketService.getInstallStatus(skillId, userAId);
        assertThat(status.get("installed")).isEqualTo(false);
        assertThat(status.get("rating")).isNull();
    }

    @Test
    void testGetInstallStatusInstalled() {
        Long skillId = createPublishedSkill(userAId, "statusYes_" + suffix);
        marketService.install(skillId, userAId);
        Map<String, Object> status = marketService.getInstallStatus(skillId, userAId);
        assertThat(status.get("installed")).isEqualTo(true);
        assertThat(status.get("rating")).isNull();
    }

    @Test
    void testGetInstallStatusWithRating() {
        Long skillId = createPublishedSkill(userAId, "statusRate_" + suffix);
        marketService.install(skillId, userAId);

        // 手动插入评分记录
        SkillRating rating = new SkillRating();
        rating.setUserId(userAId);
        rating.setSkillId(skillId);
        rating.setRating(5);
        rating.setComment("Great!");
        ratingMapper.insert(rating);

        Map<String, Object> status = marketService.getInstallStatus(skillId, userAId);
        assertThat(status.get("installed")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> ratingMap = (Map<String, Object>) status.get("rating");
        assertThat(ratingMap).isNotNull();
        assertThat(ratingMap.get("rating")).isEqualTo(5);
        assertThat(ratingMap.get("comment")).isEqualTo("Great!");
    }

    @Test
    void testGetInstallStatusSkillNotExist() {
        // 对不存在的 skill 查询状态
        Map<String, Object> status = marketService.getInstallStatus(99999L, userAId);
        assertThat(status.get("installed")).isEqualTo(false);
    }

    // ========== 数据一致性 ==========

    @Test
    void testInstallCountIncrementedCorrectly() {
        Long skillId = createPublishedSkill(userAId, "count_" + suffix);
        long before = skillMapper.selectById(skillId).getInstallCount();

        marketService.install(skillId, userAId);
        long after1 = skillMapper.selectById(skillId).getInstallCount();
        assertThat(after1).isEqualTo(before + 1);

        marketService.install(skillId, userBId);
        long after2 = skillMapper.selectById(skillId).getInstallCount();
        assertThat(after2).isEqualTo(before + 2);
    }

    @Test
    void testQueryMarketVOContainsCategoryAndAuthorName() {
        Long skillId = createPublishedSkill(userAId, "voname_" + suffix);
        MarketQueryRequest req = new MarketQueryRequest();
        SkillVO vo = marketService.queryMarket(req, userAId).getRecords().get(0);
        assertThat(vo.getCategoryName()).isNotNull();
        assertThat(vo.getAuthorName()).isNotNull();
        assertThat(vo.getLatestVersion()).isEqualTo("1.0.0");
    }
}
