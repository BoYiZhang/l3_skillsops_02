package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.service.*;
import com.boyi.skillops.vo.SkillVO;
import com.boyi.skillops.vo.VersionVO;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private com.boyi.skillops.security.JwtTokenProvider jwtTokenProvider;

    @MockBean private SkillService skillService;
    @MockBean private AdminService adminService;
    @MockBean private MarketService marketService;
    @MockBean private RatingService ratingService;
    @MockBean private UserService userService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        adminToken = jwtTokenProvider.generateToken(1L, "admin", Arrays.asList("ADMIN", "USER"));
    }

    // ========== testCreateSkillUnauthorized ==========
    @Test
    void testCreateSkillUnauthorized() throws Exception {
        // No token — Spring Security stateless returns 403 (not 401)
        mockMvc.perform(post("/api/v1/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"description\":\"Y\",\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    // ========== testGetSkill ==========
    @Test
    void testGetSkill() throws Exception {
        SkillVO vo = new SkillVO();
        vo.setId(1L);
        vo.setName("TestSkill");
        vo.setStatus("PUBLISHED");
        vo.setCategoryName("Automation");
        when(skillService.getById(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/skills/1")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("TestSkill"));
    }

    // ========== testGetVersions ==========
    @Test
    void testGetVersions() throws Exception {
        VersionVO v = new VersionVO();
        v.setId(10L);
        v.setSkillId(1L);
        v.setVersion("1.0.0");
        v.setChangelog("Initial");

        PageResult<VersionVO> page = new PageResult<>();
        page.setRecords(Arrays.asList(v));
        page.setTotal(1L);
        page.setSize(20L);
        page.setCurrent(1L);
        when(skillService.getVersions(eq(1L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/skills/1/versions")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].version").value("1.0.0"));
    }
}
