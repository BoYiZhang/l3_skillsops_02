package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.service.MarketService;
import com.boyi.skillops.service.RatingService;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.service.UserService;
import com.boyi.skillops.vo.SkillVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MarketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private MarketService marketService;
    @MockBean private SkillService skillService;
    @MockBean private AdminService adminService;
    @MockBean private RatingService ratingService;
    @MockBean private UserService userService;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
    }

    @Test
    void testQueryMarket() throws Exception {
        SkillVO vo = new SkillVO();
        vo.setId(1L);
        vo.setName("TestSkill");
        vo.setStatus("PUBLISHED");

        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(vo));
        pageResult.setTotal(1L);
        pageResult.setSize(12L);
        pageResult.setCurrent(1L);

        when(marketService.queryMarket(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/market/skills")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testInstall() throws Exception {
        mockMvc.perform(post("/api/v1/market/skills/1/install")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
