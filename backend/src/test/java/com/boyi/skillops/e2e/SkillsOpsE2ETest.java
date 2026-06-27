package com.boyi.skillops.e2e;

import com.boyi.skillops.common.Result;
import com.boyi.skillops.dto.*;
import com.boyi.skillops.entity.Role;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SkillsOpsE2ETest {

    @Autowired private TestRestTemplate rest;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private UserMapper userMapper;

    private static String userToken;
    private static String adminToken;
    private static Long skillId;

    @Test @Order(1)
    void registerAndLoginUser() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("e2euser"); req.setPassword("123456");
        ResponseEntity<Result> rr = rest.postForEntity("/api/v1/auth/register", req, Result.class);
        assertThat(rr.getBody().getCode()).isEqualTo(200);

        LoginRequest lr = new LoginRequest();
        lr.setUsername("e2euser"); lr.setPassword("123456");
        ResponseEntity<Result> lResp = rest.postForEntity("/api/v1/auth/login", lr, Result.class);
        userToken = ((Map<String, Object>) lResp.getBody().getData()).get("token").toString();
        assertThat(userToken).isNotEmpty();
    }

    @Test @Order(2)
    void registerAndLoginAdmin() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("admin"); req.setPassword("admin123");
        rest.postForEntity("/api/v1/auth/register", req, Result.class);

        // Assign ADMIN role
        User adminUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getName, "ADMIN"));
        UserRole ur = new UserRole();
        ur.setUserId(adminUser.getId());
        ur.setRoleId(adminRole.getId());
        userRoleMapper.insert(ur);

        LoginRequest lr = new LoginRequest();
        lr.setUsername("admin"); lr.setPassword("admin123");
        ResponseEntity<Result> lResp = rest.postForEntity("/api/v1/auth/login", lr, Result.class);
        adminToken = ((Map<String, Object>) lResp.getBody().getData()).get("token").toString();
        assertThat(adminToken).isNotEmpty();
    }

    @Test @Order(3)
    void createSkill() {
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(userToken);
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("E2E Test Skill"); req.setDescription("Test"); req.setCategoryId(1L);
        ResponseEntity<Result> resp = rest.exchange("/api/v1/skills", HttpMethod.POST,
                new HttpEntity<>(req, headers), Result.class);
        assertThat(resp.getBody().getCode()).isEqualTo(200);
        skillId = ((Number) ((Map) resp.getBody().getData()).get("id")).longValue();
    }

    @Test @Order(4)
    void submitAndApprove() {
        HttpHeaders uHeaders = new HttpHeaders(); uHeaders.setBearerAuth(userToken);
        ResponseEntity<Result> sr = rest.exchange("/api/v1/skills/" + skillId + "/submit",
                HttpMethod.POST, new HttpEntity<>(null, uHeaders), Result.class);
        assertThat(sr.getBody().getCode()).isEqualTo(200);

        HttpHeaders aHeaders = new HttpHeaders(); aHeaders.setBearerAuth(adminToken);
        ResponseEntity<Result> ar = rest.exchange("/api/v1/admin/skills/" + skillId + "/approve",
                HttpMethod.POST, new HttpEntity<>(null, aHeaders), Result.class);
        assertThat(ar.getBody().getCode()).isEqualTo(200);
    }

    @Test @Order(5)
    void publishVersionAndInstall() {
        HttpHeaders uHeaders = new HttpHeaders(); uHeaders.setBearerAuth(userToken);
        VersionCreateRequest vReq = new VersionCreateRequest();
        vReq.setVersion("1.0.0"); vReq.setChangelog("First release");
        ResponseEntity<Result> vr = rest.exchange("/api/v1/skills/" + skillId + "/versions",
                HttpMethod.POST, new HttpEntity<>(vReq, uHeaders), Result.class);
        assertThat(vr.getBody().getCode()).isEqualTo(200);

        ResponseEntity<Result> ir = rest.exchange("/api/v1/market/skills/" + skillId + "/install",
                HttpMethod.POST, new HttpEntity<>(null, uHeaders), Result.class);
        assertThat(ir.getBody().getCode()).isEqualTo(200);
    }

    @Test @Order(6)
    void rateAndVerify() {
        HttpHeaders uHeaders = new HttpHeaders(); uHeaders.setBearerAuth(userToken);
        RatingRequest rateReq = new RatingRequest();
        rateReq.setRating(5); rateReq.setComment("Great!");
        ResponseEntity<Result> rr = rest.exchange("/api/v1/skills/" + skillId + "/ratings",
                HttpMethod.POST, new HttpEntity<>(rateReq, uHeaders), Result.class);
        assertThat(rr.getBody().getCode()).isEqualTo(200);

        ResponseEntity<Result> detail = rest.exchange("/api/v1/skills/" + skillId,
                HttpMethod.GET, new HttpEntity<>(null, uHeaders), Result.class);
        Map data = (Map) detail.getBody().getData();
        assertThat(data.get("status")).isEqualTo("PUBLISHED");
        assertThat((Integer) data.get("installCount")).isEqualTo(1);
    }
}
