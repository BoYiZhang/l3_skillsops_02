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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    }

    // ========== testMySkillsSuccess ==========
    @Test
    void testMySkillsSuccess() throws Exception {
        SkillVO s1 = new SkillVO();
        s1.setId(1L);
        s1.setName("SkillOne");
        s1.setDescription("First skill");
        s1.setStatus("PUBLISHED");
        s1.setCategoryName("Automation");

        SkillVO s2 = new SkillVO();
        s2.setId(2L);
        s2.setName("SkillTwo");
        s2.setDescription("Second skill");
        s2.setStatus("DRAFT");

        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(s1, s2));
        pageResult.setTotal(2L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getMySkills(eq(100L), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("SkillOne"))
                .andExpect(jsonPath("$.data.records[0].description").value("First skill"))
                .andExpect(jsonPath("$.data.records[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.records[1].name").value("SkillTwo"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    // ========== testMySkillsWithPagination ==========
    @Test
    void testMySkillsWithPagination() throws Exception {
        SkillVO s = new SkillVO();
        s.setId(10L);
        s.setName("PagedSkill");

        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(s));
        pageResult.setTotal(15L);
        pageResult.setSize(5L);
        pageResult.setCurrent(2L);

        when(skillService.getMySkills(eq(100L), eq(2), eq(5))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                .param("page", "2")
                .param("size", "5")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records[0].name").value("PagedSkill"));
    }

    // ========== testMySkillsUnauthenticated ==========
    @Test
    void testMySkillsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/my-skills"))
                .andExpect(status().isForbidden());
    }

    // ========== testMySkillsEmpty ==========
    @Test
    void testMySkillsEmpty() throws Exception {
        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(0L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getMySkills(eq(100L), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ========== testInstalledSuccess ==========
    @Test
    void testInstalledSuccess() throws Exception {
        SkillInstall si = new SkillInstall();
        si.setId(1L);
        si.setSkillId(10L);
        si.setVersionId(100L);

        Skill skill = new Skill();
        skill.setName("TestSkill");
        skill.setDescription("A test skill description");
        skill.setStatus("PUBLISHED");

        SkillVersion sv = new SkillVersion();
        sv.setVersion("1.0.0");

        Page<SkillInstall> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(si));
        mockPage.setTotal(1L);
        mockPage.setSize(20L);
        mockPage.setCurrent(1L);

        when(installMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(skillMapper.selectById(eq(10L))).thenReturn(skill);
        when(versionMapper.selectById(eq(100L))).thenReturn(sv);

        mockMvc.perform(get("/api/v1/workspace/installed")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].skillId").value(10))
                .andExpect(jsonPath("$.data.records[0].skillName").value("TestSkill"))
                .andExpect(jsonPath("$.data.records[0].skillDescription").value("A test skill description"))
                .andExpect(jsonPath("$.data.records[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.data.records[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    // ========== testInstalledUnauthenticated ==========
    @Test
    void testInstalledUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/installed"))
                .andExpect(status().isForbidden());
    }

    // ========== testInstalledEmpty ==========
    @Test
    void testInstalledEmpty() throws Exception {
        Page<SkillInstall> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(0L);
        emptyPage.setSize(20L);
        emptyPage.setCurrent(1L);

        when(installMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/workspace/installed")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ========== testInstalledWithMissingSkill ==========
    @Test
    void testInstalledWithMissingSkill() throws Exception {
        SkillInstall si = new SkillInstall();
        si.setId(2L);
        si.setSkillId(999L);
        si.setVersionId(200L);

        SkillVersion sv = new SkillVersion();
        sv.setVersion("2.0.0");

        Page<SkillInstall> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(si));
        mockPage.setTotal(1L);
        mockPage.setSize(20L);
        mockPage.setCurrent(1L);

        when(installMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(skillMapper.selectById(eq(999L))).thenReturn(null);
        when(versionMapper.selectById(eq(200L))).thenReturn(sv);

        mockMvc.perform(get("/api/v1/workspace/installed")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.records[0].skillId").value(999))
                .andExpect(jsonPath("$.data.records[0].skillName").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].skillDescription").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].version").value("2.0.0"));
    }

    // ========== testInstalledWithMissingVersion ==========
    @Test
    void testInstalledWithMissingVersion() throws Exception {
        SkillInstall si = new SkillInstall();
        si.setId(3L);
        si.setSkillId(30L);
        si.setVersionId(888L);

        Skill skill = new Skill();
        skill.setName("VersionlessSkill");
        skill.setDescription("Skill with no matching version");
        skill.setStatus("PUBLISHED");

        Page<SkillInstall> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(si));
        mockPage.setTotal(1L);
        mockPage.setSize(20L);
        mockPage.setCurrent(1L);

        when(installMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(skillMapper.selectById(eq(30L))).thenReturn(skill);
        when(versionMapper.selectById(eq(888L))).thenReturn(null);

        mockMvc.perform(get("/api/v1/workspace/installed")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(3))
                .andExpect(jsonPath("$.data.records[0].skillId").value(30))
                .andExpect(jsonPath("$.data.records[0].skillName").value("VersionlessSkill"))
                .andExpect(jsonPath("$.data.records[0].skillDescription").value("Skill with no matching version"))
                .andExpect(jsonPath("$.data.records[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.records[0].version").doesNotExist());
    }

    // ========== testMySkillsDefaultPagination ==========
    @Test
    void testMySkillsDefaultPagination() throws Exception {
        PageResult<SkillVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(0L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getMySkills(eq(100L), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/workspace/my-skills")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.current").value(1));
    }
}
