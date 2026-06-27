package com.boyi.skillops.e2e;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boyi.skillops.dto.*;
import com.boyi.skillops.entity.Role;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.mapper.RoleMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.service.UserService;
import com.boyi.skillops.vo.LoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
public class ApiContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    private static String adminToken;
    private static String userToken;
    private static Long createdSkillId;
    private static Long createdCategoryId;
    private static Long adminUserId;
    private static Long normalUserId;

    @BeforeAll
    public static void setup(@Autowired TestRestTemplate rt, @Autowired UserMapper um,
                            @Autowired RoleMapper rm, @Autowired UserRoleMapper urm) {
        try {
            // Register normal user
            RegisterRequest regReq = new RegisterRequest();
            regReq.setUsername("testuser");
            regReq.setPassword("test123456");
            regReq.setEmail("testuser@test.com");
            rt.postForEntity("/api/v1/auth/register", regReq, String.class);

            // Register admin user
            RegisterRequest adminRegReq = new RegisterRequest();
            adminRegReq.setUsername("testadmin");
            adminRegReq.setPassword("admin123456");
            adminRegReq.setEmail("testadmin@test.com");
            rt.postForEntity("/api/v1/auth/register", adminRegReq, String.class);

            // Get user IDs from mapper
            User normalUser = um.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, "testuser"));
            normalUserId = normalUser.getId();
            User adminUser = um.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, "testadmin"));
            adminUserId = adminUser.getId();

            // Get or create ADMIN role
            LambdaQueryWrapper<Role> roleQuery = new LambdaQueryWrapper<>();
            roleQuery.eq(Role::getName, "ADMIN");
            Role adminRole = rm.selectOne(roleQuery);
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ADMIN");
                rm.insert(adminRole);
            }

            // Assign ADMIN role to test admin user
            UserRole ur = new UserRole();
            ur.setUserId(adminUserId);
            ur.setRoleId(adminRole.getId());
            urm.insert(ur);

            // Login as normal user
            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsername("testuser");
            loginReq.setPassword("test123456");
            ResponseEntity<String> loginResp = rt.postForEntity("/api/v1/auth/login", loginReq, String.class);
            if (loginResp.getStatusCode() == HttpStatus.OK) {
                JsonNode node = new ObjectMapper().readTree(loginResp.getBody());
                userToken = node.get("data").get("token").asText();
            }

            // Login as admin user
            LoginRequest adminLoginReq = new LoginRequest();
            adminLoginReq.setUsername("testadmin");
            adminLoginReq.setPassword("admin123456");
            ResponseEntity<String> adminLoginResp = rt.postForEntity("/api/v1/auth/login", adminLoginReq, String.class);
            if (adminLoginResp.getStatusCode() == HttpStatus.OK) {
                JsonNode node = new ObjectMapper().readTree(adminLoginResp.getBody());
                adminToken = node.get("data").get("token").asText();
            }
        } catch (Exception e) {
            throw new RuntimeException("Test setup failed", e);
        }
    }

    private HttpHeaders getHeadersWithToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }

    private void validateResultStructure(ResponseEntity<String> response, int expectedCode) throws Exception {
        assertNotNull(response.getBody(), "Response body should not be null");
        JsonNode root = objectMapper.readTree(response.getBody());
        assertTrue(root.has("code"), "Response should have 'code' field");
        assertTrue(root.has("message"), "Response should have 'message' field");
        assertEquals(expectedCode, root.get("code").asInt(), "Response code mismatch");
    }

    // ============ AUTH API TESTS ============

    @Test
    @Order(1)
    void testRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser" + System.currentTimeMillis());
        req.setPassword("password123456");
        req.setEmail("new@test.com");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/register", req, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(2)
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123456");
        req.setEmail("duplicate@test.com");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/register", req, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(40001, root.get("code").asInt(), "Should return 40001 for duplicate username");
    }

    @Test
    @Order(3)
    void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("test123456");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/login", req, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(200, root.get("code").asInt());
        assertTrue(root.get("data").has("token"), "Login response should contain token");
        assertNotNull(userToken, "User token should be set");
    }

    @Test
    @Order(4)
    void testLoginBadCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrongpassword");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/login", req, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(400, root.get("code").asInt(), "Invalid credentials should return 400");
    }

    // ============ CATEGORY API TESTS (Admin) ============

    @Test
    @Order(5)
    void testCreateCategory() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("name", "AI");
        body.put("description", "Artificial Intelligence");

        HttpEntity<Map> request = new HttpEntity<>(body, getHeadersWithToken(adminToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/admin/categories", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        createdCategoryId = root.get("data").get("id").asLong();
    }

    // ============ SKILL API TESTS ============

    @Test
    @Order(6)
    void testCreateSkill() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Test Skill " + System.currentTimeMillis());
        req.setDescription("A test skill");
        req.setCategoryId(createdCategoryId);
        req.setRepoUrl("https://github.com/test/repo");
        req.setDocUrl("https://docs.test.com");

        HttpEntity<SkillCreateRequest> request = new HttpEntity<>(req, getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/skills", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        createdSkillId = root.get("data").get("id").asLong();
        assertNotNull(createdSkillId);
    }

    @Test
    @Order(7)
    void testGetSkill() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId,
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(8)
    void testUpdateSkillByAuthor() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Updated Skill " + System.currentTimeMillis());
        req.setDescription("Updated description");
        req.setCategoryId(createdCategoryId);

        HttpEntity<SkillCreateRequest> request = new HttpEntity<>(req, getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId,
                org.springframework.http.HttpMethod.PUT, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(9)
    void testUpdateSkillByNonAuthor() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Should Fail");
        req.setDescription("Non-author update");
        req.setCategoryId(createdCategoryId);

        HttpEntity<SkillCreateRequest> request = new HttpEntity<>(req, getHeadersWithToken(adminToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId,
                org.springframework.http.HttpMethod.PUT, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(40301, root.get("code").asInt(), "Non-author should get 40301");
    }

    @Test
    @Order(10)
    void testSubmitForApproval() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/skills/" + createdSkillId + "/submit",
                request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(11)
    void testSubmitAlreadyPublished() throws Exception {
        // First approve and publish, then try to submit again
        HttpEntity<Void> approveRequest = new HttpEntity<>(getHeadersWithToken(adminToken));
        restTemplate.postForEntity("/api/v1/admin/skills/" + createdSkillId + "/approve",
                approveRequest, String.class);

        VersionCreateRequest versionReq = new VersionCreateRequest();
        versionReq.setVersion("1.0.0");
        HttpEntity<VersionCreateRequest> publishRequest = new HttpEntity<>(versionReq, getHeadersWithToken(userToken));
        restTemplate.postForEntity("/api/v1/skills/" + createdSkillId + "/versions",
                publishRequest, String.class);

        // Try to submit again - should fail
        HttpEntity<Void> submitRequest = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/skills/" + createdSkillId + "/submit",
                submitRequest, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(40002, root.get("code").asInt(), "Cannot submit already published skill");
    }

    @Test
    @Order(12)
    void testPublishVersion() throws Exception {
        VersionCreateRequest req = new VersionCreateRequest();
        req.setVersion("1.0.1");

        HttpEntity<VersionCreateRequest> request = new HttpEntity<>(req, getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/skills/" + createdSkillId + "/versions",
                request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(13)
    void testGetVersions() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId + "/versions?page=1&size=10",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertTrue(root.get("data").has("records"), "Version list should have records");
    }

    // ============ ADMIN API TESTS ============

    @Test
    @Order(14)
    void testGetPendingSkills() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(adminToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/admin/pending-skills?page=1&size=10",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(15)
    void testGetStats() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(adminToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/admin/stats",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(16)
    void testApproveSkill() throws Exception {
        // Create another skill to approve
        SkillCreateRequest skillReq = new SkillCreateRequest();
        skillReq.setName("Skill to Approve " + System.currentTimeMillis());
        skillReq.setDescription("For approval test");
        skillReq.setCategoryId(createdCategoryId);

        HttpEntity<SkillCreateRequest> createRequest = new HttpEntity<>(skillReq, getHeadersWithToken(userToken));
        ResponseEntity<String> createResp = restTemplate.postForEntity("/api/v1/skills", createRequest, String.class);
        JsonNode createNode = objectMapper.readTree(createResp.getBody());
        Long skillToApproveId = createNode.get("data").get("id").asLong();

        // Submit for approval
        HttpEntity<Void> submitRequest = new HttpEntity<>(getHeadersWithToken(userToken));
        restTemplate.postForEntity("/api/v1/skills/" + skillToApproveId + "/submit", submitRequest, String.class);

        // Approve
        HttpEntity<Void> approveRequest = new HttpEntity<>(getHeadersWithToken(adminToken));
        ResponseEntity<String> approveResp = restTemplate.postForEntity("/api/v1/admin/skills/" + skillToApproveId + "/approve",
                approveRequest, String.class);
        assertEquals(HttpStatus.OK, approveResp.getStatusCode());
        validateResultStructure(approveResp, 200);
    }

    @Test
    @Order(17)
    void testRejectSkill() throws Exception {
        // Create skill to reject
        SkillCreateRequest skillReq = new SkillCreateRequest();
        skillReq.setName("Skill to Reject " + System.currentTimeMillis());
        skillReq.setDescription("For rejection test");
        skillReq.setCategoryId(createdCategoryId);

        HttpEntity<SkillCreateRequest> createRequest = new HttpEntity<>(skillReq, getHeadersWithToken(userToken));
        ResponseEntity<String> createResp = restTemplate.postForEntity("/api/v1/skills", createRequest, String.class);
        JsonNode createNode = objectMapper.readTree(createResp.getBody());
        Long skillToRejectId = createNode.get("data").get("id").asLong();

        // Submit for approval
        HttpEntity<Void> submitRequest = new HttpEntity<>(getHeadersWithToken(userToken));
        restTemplate.postForEntity("/api/v1/skills/" + skillToRejectId + "/submit", submitRequest, String.class);

        // Reject
        Map<String, String> rejectBody = new HashMap<>();
        rejectBody.put("reason", "Does not meet requirements");
        HttpEntity<Map> rejectRequest = new HttpEntity<>(rejectBody, getHeadersWithToken(adminToken));
        ResponseEntity<String> rejectResp = restTemplate.postForEntity("/api/v1/admin/skills/" + skillToRejectId + "/reject",
                rejectRequest, String.class);
        assertEquals(HttpStatus.OK, rejectResp.getStatusCode());
        validateResultStructure(rejectResp, 200);
    }

    @Test
    @Order(18)
    void testDelistSkill() throws Exception {
        // Get an approved skill and delist it
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> getPendingResp = restTemplate.exchange("/api/v1/admin/pending-skills?page=1&size=100",
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(getHeadersWithToken(adminToken)), String.class);

        Map<String, String> delistBody = new HashMap<>();
        delistBody.put("reason", "Outdated");
        HttpEntity<Map> delistRequest = new HttpEntity<>(delistBody, getHeadersWithToken(adminToken));
        ResponseEntity<String> delistResp = restTemplate.postForEntity("/api/v1/admin/skills/" + createdSkillId + "/delist",
                delistRequest, String.class);
        assertEquals(HttpStatus.OK, delistResp.getStatusCode());
        validateResultStructure(delistResp, 200);
    }

    @Test
    @Order(19)
    void testUnauthorizedAdminAccess() throws Exception {
        // Try to approve as non-admin
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/admin/skills/" + createdSkillId + "/approve",
                request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(403, root.get("code").asInt(), "Non-admin should get 403");
    }

    // ============ MARKET API TESTS ============

    @Test
    @Order(20)
    void testMarketQueryEmpty() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/market/skills?page=1&size=10",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(21)
    void testInstallSkill() throws Exception {
        // Create and approve a skill for market
        SkillCreateRequest skillReq = new SkillCreateRequest();
        skillReq.setName("Market Skill " + System.currentTimeMillis());
        skillReq.setDescription("For market install test");
        skillReq.setCategoryId(createdCategoryId);

        HttpEntity<SkillCreateRequest> createRequest = new HttpEntity<>(skillReq, getHeadersWithToken(userToken));
        ResponseEntity<String> createResp = restTemplate.postForEntity("/api/v1/skills", createRequest, String.class);
        JsonNode createNode = objectMapper.readTree(createResp.getBody());
        Long skillToInstallId = createNode.get("data").get("id").asLong();

        // Submit and approve
        HttpEntity<Void> submitRequest = new HttpEntity<>(getHeadersWithToken(userToken));
        restTemplate.postForEntity("/api/v1/skills/" + skillToInstallId + "/submit", submitRequest, String.class);

        HttpEntity<Void> approveRequest = new HttpEntity<>(getHeadersWithToken(adminToken));
        restTemplate.postForEntity("/api/v1/admin/skills/" + skillToInstallId + "/approve", approveRequest, String.class);

        VersionCreateRequest versionReq = new VersionCreateRequest();
        versionReq.setVersion("1.0.0");
        HttpEntity<VersionCreateRequest> publishRequest = new HttpEntity<>(versionReq, getHeadersWithToken(userToken));
        restTemplate.postForEntity("/api/v1/skills/" + skillToInstallId + "/versions", publishRequest, String.class);

        // Install
        HttpEntity<Void> installRequest = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> installResp = restTemplate.postForEntity("/api/v1/market/skills/" + skillToInstallId + "/install",
                installRequest, String.class);
        assertEquals(HttpStatus.OK, installResp.getStatusCode());
        validateResultStructure(installResp, 200);
    }

    @Test
    @Order(22)
    void testInstallDuplicate() throws Exception {
        // Try to install same skill again - should fail
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/market/skills/" + createdSkillId + "/install",
                request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        // Should be 40004 (already installed) or 409 (conflict)
        assertTrue(root.get("code").asInt() == 40004 || root.get("code").asInt() == 409,
                "Duplicate install should return 40004 or 409");
    }

    @Test
    @Order(23)
    void testInstallStatus() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/market/skills/" + createdSkillId + "/install-status",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertTrue(root.get("data").has("installed") || root.get("data").has("status"),
                "Install status should contain installation info");
    }

    // ============ RATING API TESTS ============

    @Test
    @Order(24)
    void testRateInstalledSkill() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(5);
        req.setComment("Excellent skill!");

        HttpEntity<RatingRequest> request = new HttpEntity<>(req, getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/skills/" + createdSkillId + "/ratings",
                request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(25)
    void testRateNotInstalled() throws Exception {
        // Create a new skill
        SkillCreateRequest skillReq = new SkillCreateRequest();
        skillReq.setName("Uninstalled Skill " + System.currentTimeMillis());
        skillReq.setDescription("Not installed for rating test");
        skillReq.setCategoryId(createdCategoryId);

        HttpEntity<SkillCreateRequest> createRequest = new HttpEntity<>(skillReq, getHeadersWithToken(userToken));
        ResponseEntity<String> createResp = restTemplate.postForEntity("/api/v1/skills", createRequest, String.class);
        JsonNode createNode = objectMapper.readTree(createResp.getBody());
        Long uninstalledSkillId = createNode.get("data").get("id").asLong();

        // Try to rate without installing
        RatingRequest ratingReq = new RatingRequest();
        ratingReq.setRating(3);
        ratingReq.setComment("Not installed");

        HttpEntity<RatingRequest> ratingRequest = new HttpEntity<>(ratingReq, getHeadersWithToken(userToken));
        ResponseEntity<String> ratingResp = restTemplate.postForEntity("/api/v1/skills/" + uninstalledSkillId + "/ratings",
                ratingRequest, String.class);
        assertEquals(HttpStatus.OK, ratingResp.getStatusCode());
        JsonNode root = objectMapper.readTree(ratingResp.getBody());
        assertEquals(40003, root.get("code").asInt(), "Rating not installed should return 40003");
    }

    @Test
    @Order(26)
    void testUpdateRating() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(4);
        req.setComment("Updated rating");

        HttpEntity<RatingRequest> request = new HttpEntity<>(req, getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId + "/ratings",
                org.springframework.http.HttpMethod.PUT, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
    }

    @Test
    @Order(27)
    void testGetRatings() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId + "/ratings?page=1&size=10",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertTrue(root.get("data").has("records"), "Ratings list should have records");
    }

    // ============ WORKSPACE API TESTS ============

    @Test
    @Order(28)
    void testMySkills() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/workspace/my-skills?page=1&size=10",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertTrue(root.get("data").has("records"), "My skills list should have records");
    }

    @Test
    @Order(29)
    void testInstalledSkills() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/workspace/installed?page=1&size=10",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        validateResultStructure(response, 200);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertTrue(root.get("data").has("records"), "Installed skills list should have records");
    }

    // ============ SECURITY TESTS ============

    @Test
    @Order(30)
    void testNoTokenUnauthorized() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(null));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId,
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(401, root.get("code").asInt(), "No token should return 401");
    }

    @Test
    @Order(31)
    void testInvalidTokenUnauthorized() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer invalid-token-12345");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/" + createdSkillId,
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(401, root.get("code").asInt(), "Invalid token should return 401");
    }

    @Test
    @Order(32)
    void testNotFoundResource() throws Exception {
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithToken(userToken));
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/skills/99999999",
                org.springframework.http.HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(404, root.get("code").asInt(), "Non-existent resource should return 404");
    }
}
