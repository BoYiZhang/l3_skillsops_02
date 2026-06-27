package com.boyi.skillops.e2e;

import com.boyi.skillops.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API contract tests — verify response shape consistency for all endpoints.
 * These tests ensure the API response format (code, message, data) follows
 * the project convention across all controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApiContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        adminToken = jwtTokenProvider.generateToken(1L, "admin", Arrays.asList("ADMIN", "USER"));
    }

    // ==================== Auth Endpoints ====================

    @Test
    void testAuthContractLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testAuthContractRegister() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testcontract\",\"password\":\"123456\",\"email\":\"t@t.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    // ==================== Market Endpoints ====================

    @Test
    void testMarketContractQuery() throws Exception {
        // Market query is now publicly accessible
        mockMvc.perform(get("/api/v1/market/skills")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testMarketContractInstall() throws Exception {
        mockMvc.perform(post("/api/v1/market/skills/1/install")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void testMarketContractInstallStatus() throws Exception {
        mockMvc.perform(get("/api/v1/market/skills/1/install-status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== Skill Endpoints ====================

    @Test
    void testSkillContractCreate() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Contract Skill\",\"description\":\"test\",\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void testSkillContractGetById() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void testSkillContractSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/submit")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void testSkillContractGetVersions() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1/versions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== Rating Endpoints ====================

    @Test
    void testRatingContractRate() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void testRatingContractGetRatings() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== Workspace Endpoints ====================

    @Test
    void testWorkspaceContractMySkills() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/my-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testWorkspaceContractInstalled() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/installed")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== Admin Endpoints ====================

    @Test
    void testAdminContractCategories() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testAdminContractPendingSkills() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testAdminContractStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testAdminContractUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== Contract: Error Response Format ====================

    @Test
    void testErrorResponseFormat() throws Exception {
        // Unauthenticated access should return 403 Forbidden
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNotFoundResponseFormat() throws Exception {
        // Non-existent skill should return proper error response
        mockMvc.perform(get("/api/v1/skills/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }
}
