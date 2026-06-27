package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.entity.Skill;
import com.boyi.skillops.entity.SkillInstall;
import com.boyi.skillops.entity.SkillVersion;
import com.boyi.skillops.mapper.SkillInstallMapper;
import com.boyi.skillops.mapper.SkillMapper;
import com.boyi.skillops.mapper.SkillVersionMapper;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.SkillService;
import com.boyi.skillops.vo.InstallVO;
import com.boyi.skillops.vo.SkillVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private SkillService skillService;
    @MockBean private SkillInstallMapper installMapper;
    @MockBean private SkillMapper skillMapper;
    @MockBean private SkillVersionMapper versionMapper;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        reset(skillService, installMapper, skillMapper, versionMapper);
    }

    // ==================== My Skills ====================

    @Test
    void testMySkillsSuccess() throws Exception {
        SkillVO vo1 = new SkillVO();
        vo1.setId(1L); vo1.setName("My Skill 1"); vo1.setDescription("First skill");
        vo1.setStatus("DRAFT"); vo1.setAuthorId(100L); vo1.setAuthorName("testuser");
        SkillVO vo2 = new SkillVO();
        vo2.setId(2L); vo2.setName("My Skill 2"); vo2.setDescription("Second skill");
        vo2.setStatus("PUBLISHED"); vo2.setAuthorId(100L); vo2.setAuthorName("testuser");

        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(vo1, vo2));
        pageResult.setTotal(2L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getMySkills(100L, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].name").value("My Skill 1"))
                .andExpect(jsonPath("$.data.records[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.records[1].name").value("My Skill 2"))
                .andExpect(jsonPath("$.data.records[1].status").value("PUBLISHED"));
    }

    @Test
    void testMySkillsEmpty() throws Exception {
        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(0L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getMySkills(100L, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void testMySkillsPagination() throws Exception {
        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(15L);
        pageResult.setSize(5L);
        pageResult.setCurrent(3L);

        when(skillService.getMySkills(100L, 3, 5)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "3")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(3))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void testMySkillsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/my-skills"))
                .andExpect(status().isForbidden());
    }

    // ==================== Installed Skills ====================

    @Test
    void testInstalledSuccess() throws Exception {
        SkillInstall inst1 = new SkillInstall();
        inst1.setId(1L); inst1.setUserId(100L); inst1.setSkillId(10L); inst1.setVersionId(101L);

        SkillInstall inst2 = new SkillInstall();
        inst2.setId(2L); inst2.setUserId(100L); inst2.setSkillId(20L); inst2.setVersionId(201L);

        Page<SkillInstall> page = new Page<>(1, 20);
        page.setRecords(Arrays.asList(inst1, inst2));
        page.setTotal(2L); page.setSize(20L); page.setCurrent(1L);

        Skill skill1 = new Skill();
        skill1.setId(10L); skill1.setName("Installed Skill 1");
        skill1.setDescription("Desc 1"); skill1.setStatus("PUBLISHED");

        Skill skill2 = new Skill();
        skill2.setId(20L); skill2.setName("Installed Skill 2");
        skill2.setDescription("Desc 2"); skill2.setStatus("PUBLISHED");

        SkillVersion ver1 = new SkillVersion();
        ver1.setId(101L); ver1.setVersion("1.0.0");
        SkillVersion ver2 = new SkillVersion();
        ver2.setId(201L); ver2.setVersion("2.0.0");

        when(installMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(skillMapper.selectById(10L)).thenReturn(skill1);
        when(skillMapper.selectById(20L)).thenReturn(skill2);
        when(versionMapper.selectById(101L)).thenReturn(ver1);
        when(versionMapper.selectById(201L)).thenReturn(ver2);

        mockMvc.perform(get("/api/v1/workspace/installed")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].skillName").value("Installed Skill 1"))
                .andExpect(jsonPath("$.data.records[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.data.records[1].skillName").value("Installed Skill 2"))
                .andExpect(jsonPath("$.data.records[1].version").value("2.0.0"));
    }

    @Test
    void testInstalledEmpty() throws Exception {
        Page<SkillInstall> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0L); page.setSize(20L); page.setCurrent(1L);

        when(installMapper.selectPage(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/workspace/installed")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void testInstalledWithDeletedSkill() throws Exception {
        // When a skill is deleted but install record still exists
        SkillInstall inst = new SkillInstall();
        inst.setId(1L); inst.setUserId(100L); inst.setSkillId(10L); inst.setVersionId(101L);

        Page<SkillInstall> page = new Page<>(1, 20);
        page.setRecords(Arrays.asList(inst));
        page.setTotal(1L); page.setSize(20L); page.setCurrent(1L);

        when(installMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(skillMapper.selectById(10L)).thenReturn(null);
        when(versionMapper.selectById(101L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/workspace/installed")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].skillName").doesNotExist());
    }

    @Test
    void testInstalledUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/installed"))
                .andExpect(status().isForbidden());
    }
}
