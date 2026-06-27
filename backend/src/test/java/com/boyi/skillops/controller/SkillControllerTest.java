package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.AdminService;
import com.boyi.skillops.service.MarketService;
import com.boyi.skillops.service.RatingService;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.service.UserService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

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

    @Test
    void testCreateSkillUnauthorized() throws Exception {
        SkillCreateRequest request = new SkillCreateRequest();
        request.setName("TestSkill");
        request.setDescription("A test skill");
        request.setCategoryId(1L);

        mockMvc.perform(post("/api/v1/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetSkill() throws Exception {
        SkillVO vo = new SkillVO();
        vo.setId(1L);
        vo.setName("TestSkill");
        vo.setStatus("PUBLISHED");
        when(skillService.getById(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/skills/1")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testGetVersions() throws Exception {
        VersionVO versionVO = new VersionVO();
        versionVO.setId(10L);
        versionVO.setSkillId(1L);
        versionVO.setVersion("1.0.0");
        versionVO.setChangelog("Initial release");

        PageResult<VersionVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(versionVO));
        pageResult.setTotal(1L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getVersions(eq(1L), anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/versions")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
