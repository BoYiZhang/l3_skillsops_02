package com.boyi.skillops.security;

import com.boyi.skillops.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private com.boyi.skillops.security.JwtTokenProvider jwtTokenProvider;

    @MockBean private SkillService skillService;
    @MockBean private AdminService adminService;
    @MockBean private MarketService marketService;
    @MockBean private RatingService ratingService;
    @MockBean private UserService userService;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "regularuser", Arrays.asList("USER"));
    }

    // ========== testAccessProtectedEndpointWithoutToken ==========
    @Test
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        // Spring Security with stateless sessions defaults to 403 for unauthenticated
        mockMvc.perform(get("/api/v1/skills/1"))
                .andExpect(status().isForbidden());
    }

    // ========== testAccessProtectedEndpointWithInvalidToken ==========
    @Test
    void testAccessProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    // ========== testAccessAdminEndpointAsUser ==========
    @Test
    void testAccessAdminEndpointAsUser() throws Exception {
        // GlobalExceptionHandler catches AccessDeniedException and returns HTTP 200
        // with body code=403
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
