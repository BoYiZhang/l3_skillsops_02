package com.boyi.skillops.security;

import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.service.MarketService;
import com.boyi.skillops.service.RatingService;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    @MockBean
    private SkillService skillService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private MarketService marketService;

    @MockBean
    private RatingService ratingService;

    @Test
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessAdminEndpointAsUser() throws Exception {
        String userToken = jwtTokenProvider.generateToken(100L, "regularuser", Arrays.asList("USER"));

        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
