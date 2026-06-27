package com.boyi.skillops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.RatingRequest;
import com.boyi.skillops.dto.RegisterRequest;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.entity.Role;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.SkillRatingMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.vo.RatingVO;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class RatingServiceTest {

    @Autowired
    private RatingService ratingService;

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
    private SkillRatingMapper skillRatingMapper;

    private Long adminId;
    private Long user1Id;
    private Long skillId;

    @BeforeEach
    void setUp() {
        // Use unique suffixes to avoid conflicts within H2 in-memory transactions
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Register admin user
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("ratingadmin_" + suffix);
        adminReq.setPassword("password123");
        userService.register(adminReq);

        User adminUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "ratingadmin_" + suffix));
        adminId = adminUser.getId();

        // Assign ADMIN role
        Role adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getName, "ADMIN"));
        UserRole ur = new UserRole();
        ur.setUserId(adminId);
        ur.setRoleId(adminRole.getId());
        userRoleMapper.insert(ur);

        // 2. Register regular user1
        RegisterRequest user1Req = new RegisterRequest();
        user1Req.setUsername("ratinguser1_" + suffix);
        user1Req.setPassword("password123");
        userService.register(user1Req);

        User user1 = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "ratinguser1_" + suffix));
        user1Id = user1.getId();

        // 3. Create skill → submit → approve → publishVersion → install for user1
        SkillCreateRequest skillReq = new SkillCreateRequest();
        skillReq.setName("Rating Test Skill " + suffix);
        skillReq.setDescription("A skill for rating tests");
        skillReq.setCategoryId(1L);

        SkillVO skillVO = skillService.create(skillReq, adminId);
        skillId = skillVO.getId();

        skillService.submitForApproval(skillId, adminId);
        adminService.approve(skillId, adminId);

        VersionCreateRequest versionReq = new VersionCreateRequest();
        versionReq.setVersion("1.0.0");
        versionReq.setChangelog("Initial release");
        skillService.publishVersion(skillId, versionReq, adminId);

        marketService.install(skillId, user1Id);
    }

    @Test
    void testRate() {
        RatingRequest req = new RatingRequest();
        req.setRating(4);
        req.setComment("good");

        assertDoesNotThrow(() -> ratingService.rate(skillId, req, user1Id));

        PageResult<RatingVO> result = ratingService.getRatings(skillId, 1, 10);
        assertNotNull(result);
        assertFalse(result.getRecords().isEmpty());

        RatingVO vo = result.getRecords().get(0);
        assertEquals(4, vo.getRating());
    }

    @Test
    void testRateNotInstalledFails() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        RegisterRequest user2Req = new RegisterRequest();
        user2Req.setUsername("ratinguser2_" + suffix);
        user2Req.setPassword("password123");
        userService.register(user2Req);

        User user2 = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "ratinguser2_" + suffix));
        Long user2Id = user2.getId();

        RatingRequest req = new RatingRequest();
        req.setRating(3);
        req.setComment("not installed");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ratingService.rate(skillId, req, user2Id));
        assertEquals(40003, ex.getErrorCode().getCode());
    }

    @Test
    void testUpdateRating() {
        RatingRequest req1 = new RatingRequest();
        req1.setRating(3);
        req1.setComment("average");
        ratingService.rate(skillId, req1, user1Id);

        RatingRequest req2 = new RatingRequest();
        req2.setRating(5);
        req2.setComment("excellent");
        ratingService.rate(skillId, req2, user1Id);

        PageResult<RatingVO> result = ratingService.getRatings(skillId, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(5, result.getRecords().get(0).getRating());
    }

    @Test
    void testGetRatings() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Register and install for user2
        RegisterRequest user2Req = new RegisterRequest();
        user2Req.setUsername("ratinguser2_" + suffix);
        user2Req.setPassword("password123");
        userService.register(user2Req);

        User user2 = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "ratinguser2_" + suffix));
        Long user2Id = user2.getId();

        marketService.install(skillId, user2Id);

        // Rate with user1
        RatingRequest req1 = new RatingRequest();
        req1.setRating(4);
        req1.setComment("user1 comment");
        ratingService.rate(skillId, req1, user1Id);

        // Rate with user2
        RatingRequest req2 = new RatingRequest();
        req2.setRating(5);
        req2.setComment("user2 comment");
        ratingService.rate(skillId, req2, user2Id);

        // Page 1, size 10 — should return both ratings
        PageResult<RatingVO> result = ratingService.getRatings(skillId, 1, 10);
        assertNotNull(result);
        assertEquals(2, result.getRecords().size());

        // Page 1, size 1 — pagination: only 1 record, total still 2
        PageResult<RatingVO> paged = ratingService.getRatings(skillId, 1, 1);
        assertNotNull(paged);
        assertEquals(1, paged.getRecords().size());
        assertEquals(2L, paged.getTotal());
    }
}
