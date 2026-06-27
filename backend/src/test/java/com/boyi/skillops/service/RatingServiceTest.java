package com.boyi.skillops.service;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.RatingRequest;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.*;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.*;
import com.boyi.skillops.vo.RatingVO;
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
public class RatingServiceTest {

    @Autowired private RatingService ratingService;
    @Autowired private MarketService marketService;
    @Autowired private SkillService skillService;
    @Autowired private UserService userService;
    @Autowired private AdminService adminService;
    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private SkillMapper skillMapper;

    private Long user1Id;
    private Long skillId;
    private String classSuffix;

    @BeforeEach
    void setUp() {
        classSuffix = String.valueOf(System.nanoTime());

        // Register user1
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("rateuser1_" + classSuffix);
        req.setPassword("123456");
        userService.register(req);
        user1Id = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "rateuser1_" + classSuffix)).getId();

        // Create, submit, approve, publish, and install a skill for user1
        SkillCreateRequest sReq = new SkillCreateRequest();
        sReq.setName("RatedSkill_" + classSuffix);
        sReq.setDescription("For rating tests");
        sReq.setCategoryId(1L);
        com.boyi.skillops.vo.SkillVO vo = skillService.create(sReq, user1Id);
        skillService.submitForApproval(vo.getId(), user1Id);
        adminService.approve(vo.getId(), user1Id);
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0.0");
        skillService.publishVersion(vo.getId(), vReq, user1Id);
        marketService.install(vo.getId(), user1Id);
        skillId = vo.getId();
    }

    // ========== testRate ==========
    @Test
    void testRate() {
        RatingRequest rReq = new RatingRequest();
        rReq.setRating(4);
        rReq.setComment("good");
        ratingService.rate(skillId, rReq, user1Id);

        PageResult<RatingVO> ratings = ratingService.getRatings(skillId, 1, 10);
        assertThat(ratings.getTotal()).isEqualTo(1);
        assertThat(ratings.getRecords().get(0).getRating()).isEqualTo(4);
    }

    // ========== testRateNotInstalledFails ==========
    @Test
    void testRateNotInstalledFails() {
        String suffix2 = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("norater_" + suffix2);
        req.setPassword("123456");
        userService.register(req);
        Long user2Id = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "norater_" + suffix2)).getId();

        RatingRequest rReq = new RatingRequest();
        rReq.setRating(3);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ratingService.rate(skillId, rReq, user2Id));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40003);
    }

    // ========== testUpdateRating ==========
    @Test
    void testUpdateRating() {
        RatingRequest rReq1 = new RatingRequest();
        rReq1.setRating(3);
        rReq1.setComment("first");
        ratingService.rate(skillId, rReq1, user1Id);

        RatingRequest rReq2 = new RatingRequest();
        rReq2.setRating(5);
        rReq2.setComment("updated");
        ratingService.rate(skillId, rReq2, user1Id);

        PageResult<RatingVO> ratings = ratingService.getRatings(skillId, 1, 10);
        assertThat(ratings.getTotal()).isEqualTo(1);
        assertThat(ratings.getRecords().get(0).getRating()).isEqualTo(5);
        assertThat(ratings.getRecords().get(0).getComment()).isEqualTo("updated");
    }

    // ========== testGetRatings ==========
    @Test
    void testGetRatings() {
        // Install and rate from user1 (already installed in setUp)
        RatingRequest rReq1 = new RatingRequest();
        rReq1.setRating(4);
        rReq1.setComment("user1 rating");
        ratingService.rate(skillId, rReq1, user1Id);

        // Register, install, and rate user2
        String suffix2 = String.valueOf(System.nanoTime());
        com.boyi.skillops.dto.RegisterRequest req = new com.boyi.skillops.dto.RegisterRequest();
        req.setUsername("rater2_" + suffix2);
        req.setPassword("123456");
        userService.register(req);
        Long user2Id = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "rater2_" + suffix2)).getId();
        marketService.install(skillId, user2Id);
        RatingRequest rReq2 = new RatingRequest();
        rReq2.setRating(5);
        rReq2.setComment("user2 rating");
        ratingService.rate(skillId, rReq2, user2Id);

        PageResult<RatingVO> ratings = ratingService.getRatings(skillId, 1, 10);
        assertThat(ratings.getTotal()).isEqualTo(2);
    }
}
