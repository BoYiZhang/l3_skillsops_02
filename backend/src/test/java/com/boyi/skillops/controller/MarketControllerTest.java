package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.service.*;
import com.boyi.skillops.vo.SkillVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MarketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private com.boyi.skillops.security.JwtTokenProvider jwtTokenProvider;

    @MockBean private SkillService skillService;
    @MockBean private AdminService adminService;
    @MockBean private MarketService marketService;
    @MockBean private RatingService ratingService;
    @MockBean private UserService userService;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
    }

    // ========== testQueryMarket ==========
    @Test
    void testQueryMarket() throws Exception {
        SkillVO vo = new SkillVO();
        vo.setId(1L);
        vo.setName("Market Item");
        vo.setStatus("PUBLISHED");
        vo.setInstallCount(10L);
        vo.setAvgRating(4.5);

        PageResult<SkillVO> page = new PageResult<>();
        page.setRecords(Arrays.asList(vo));
        page.setTotal(1L);
        page.setSize(12L);
        page.setCurrent(1L);
        when(marketService.queryMarket(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/market/skills")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    // ========== testInstall ==========
    @Test
    void testInstall() throws Exception {
        // mock void method — default is do nothing
        mockMvc.perform(post("/api/v1/market/skills/1/install")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
