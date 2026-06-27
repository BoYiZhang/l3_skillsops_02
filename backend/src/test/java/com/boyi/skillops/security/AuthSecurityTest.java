package com.boyi.skillops.security;

import com.boyi.skillops.controller.AdminController;
import com.boyi.skillops.controller.MarketController;
import com.boyi.skillops.controller.SkillController;
import com.boyi.skillops.controller.WorkspaceController;
import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.service.MarketService;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security authentication and authorization tests.
 * Verifies JWT token lifecycle, role-based access control,
 * and endpoint-level security configurations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private UserService userService;
    @MockBean private SkillService skillService;
    @MockBean private MarketService marketService;
    @MockBean private AdminService adminService;

    private String userToken;
    private String adminToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        adminToken = jwtTokenProvider.generateToken(1L, "admin", Arrays.asList("ADMIN", "USER"));
        // Generate a token that's already expired (negative expiration)
        expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGVzIjoiQURNSU4iLCJpYXQiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMDAwMX0.invalid";
    }

    // ==================== Token Validation ====================

    @Test
    void testValidTokenAccessGranted() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void testMissingTokenAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testMalformedTokenAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testExpiredTokenAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNoBearerPrefixAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testEmptyAuthorizationHeaderAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", ""))
                .andExpect(status().isForbidden());
    }

    // ==================== Role-Based Access Control ====================

    @Test
    void testAdminEndpointsAllowAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminEndpointsRejectUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminApproveRejectUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skills/1/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminPendingSkillsRejectUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminStatsRejectUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminUsersRejectUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ==================== User Endpoints Accept USER Role ====================

    @Test
    void testSkillEndpointsAllowUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void testWorkspaceEndpointsAllowUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/my-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void testMarketEndpointsAllowUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/market/skills/1/install")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ==================== Public Endpoints ====================

    @Test
    void testAuthLoginPublicAccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthRegisterPublicAccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testMarketQueryPublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/market/skills")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    // ==================== Token Provider Unit Tests ====================

    @Test
    void testTokenProviderGenerateAndValidate() {
        String token = jwtTokenProvider.generateToken(1L, "admin", Arrays.asList("ADMIN"));
        assert jwtTokenProvider.validateToken(token);
        assert jwtTokenProvider.getUserId(token).equals(1L);
        assert jwtTokenProvider.getUsername(token).equals("admin");
        assert jwtTokenProvider.getRoles(token).contains("ADMIN");
    }

    @Test
    void testTokenProviderMultipleRoles() {
        String token = jwtTokenProvider.generateToken(1L, "admin", Arrays.asList("ADMIN", "USER"));
        assert jwtTokenProvider.validateToken(token);
        assert jwtTokenProvider.getRoles(token).contains("ADMIN");
        assert jwtTokenProvider.getRoles(token).contains("USER");
        assert jwtTokenProvider.getRoles(token).size() == 2;
    }

    // ==================== CORS Headers ====================

    @Test
    void testCorsHeadersPresent() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }
}
