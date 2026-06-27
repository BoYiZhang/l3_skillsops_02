package com.boyi.skillops.e2e;

import com.boyi.skillops.entity.*;
import com.boyi.skillops.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
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
    @Autowired private PasswordEncoder passwordEncoder;

    private static String userToken;
    private static String adminToken;
    private static Long skillId;

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    @Test @Order(1)
    void setupAdmin() {
        // Create admin user if not exists
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")) == null) {
            User u = new User(); u.setUsername("admin"); u.setPassword(passwordEncoder.encode("admin123"));
            u.setEmail("admin@test.com"); u.setStatus("ACTIVE"); userMapper.insert(u);
            Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getName, "ADMIN"));
            UserRole ur = new UserRole(); ur.setUserId(u.getId()); ur.setRoleId(adminRole.getId()); userRoleMapper.insert(ur);
        }
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> r = rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(map("username","admin","password","admin123"), h), Map.class);
        adminToken = ((Map<String,Object>)r.getBody().get("data")).get("token").toString();
        assertThat(adminToken).isNotEmpty();
    }

    @Test @Order(2)
    void registerAndLoginUser() {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(map("username","e2euser","password","123456"), h), Map.class);
        ResponseEntity<Map> r = rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(map("username","e2euser","password","123456"), h), Map.class);
        userToken = ((Map<String,Object>)r.getBody().get("data")).get("token").toString();
        assertThat(userToken).isNotEmpty();
    }

    @Test @Order(3)
    void createSkill() {
        HttpHeaders h = new HttpHeaders(); h.setBearerAuth(userToken); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> r = rest.exchange("/api/v1/skills", HttpMethod.POST,
                new HttpEntity<>(map("name","E2E Skill","description","test","categoryId",1), h), Map.class);
        assertThat((int)r.getBody().get("code")).isEqualTo(200);
        skillId = ((Number)((Map)r.getBody().get("data")).get("id")).longValue();
    }

    @Test @Order(4)
    void submitAndApprove() {
        HttpHeaders h = new HttpHeaders(); h.setBearerAuth(userToken);
        ResponseEntity<Map> r = rest.exchange("/api/v1/skills/"+skillId+"/submit",
                HttpMethod.POST, new HttpEntity<>(null, h), Map.class);
        assertThat((int)r.getBody().get("code")).isEqualTo(200);
        HttpHeaders ah = new HttpHeaders(); ah.setBearerAuth(adminToken);
        r = rest.exchange("/api/v1/admin/skills/"+skillId+"/approve",
                HttpMethod.POST, new HttpEntity<>(null, ah), Map.class);
        assertThat((int)r.getBody().get("code")).isEqualTo(200);
    }

    @Test @Order(5)
    void publishAndInstall() {
        HttpHeaders h = new HttpHeaders(); h.setBearerAuth(userToken); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> r = rest.exchange("/api/v1/skills/"+skillId+"/versions",
                HttpMethod.POST, new HttpEntity<>(map("version","1.0.0"), h), Map.class);
        assertThat((int)r.getBody().get("code")).isEqualTo(200);
        r = rest.exchange("/api/v1/market/skills/"+skillId+"/install",
                HttpMethod.POST, new HttpEntity<>(null, h), Map.class);
        assertThat((int)r.getBody().get("code")).isEqualTo(200);
    }

    @Test @Order(6)
    void rateAndDetail() {
        HttpHeaders h = new HttpHeaders(); h.setBearerAuth(userToken); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> r = rest.exchange("/api/v1/skills/"+skillId+"/ratings",
                HttpMethod.POST, new HttpEntity<>(map("rating",5,"comment","Good"), h), Map.class);
        assertThat((int)r.getBody().get("code")).isEqualTo(200);
        h.setContentType(null);
        r = rest.exchange("/api/v1/skills/"+skillId, HttpMethod.GET,
                new HttpEntity<>(null, h), Map.class);
        assertThat(((Map)r.getBody().get("data")).get("status")).isEqualTo("PUBLISHED");
    }
}
