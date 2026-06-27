package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.SkillCreateRequest;
import com.boyi.skillops.dto.VersionCreateRequest;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.SkillService;
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
public class SkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private SkillService skillService;

    private String userToken;
    private String userToken2;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        userToken2 = jwtTokenProvider.generateToken(200L, "otheruser", Arrays.asList("USER"));
        reset(skillService);
    }

    // ==================== Create Skill ====================

    @Test
    void testCreateSuccess() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Test Skill");
        req.setDescription("A test skill");
        req.setCategoryId(1L);
        req.setRepoUrl("https://github.com/test/skill");
        req.setDocUrl("https://docs.test.com/skill");

        SkillVO vo = buildSkillVO(1L, "Test Skill", "A test skill", 1L, "Category1", "DRAFT");
        when(skillService.create(any(SkillCreateRequest.class), eq(100L))).thenReturn(vo);

        mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Skill"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(skillService).create(any(SkillCreateRequest.class), eq(100L));
    }

    @Test
    void testCreateUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateValidationFailure() throws Exception {
        // Missing required fields should still reach controller (validation is @Valid)
        // but we test that the service layer is NOT called when validation fails
        SkillCreateRequest req = new SkillCreateRequest();

        mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(skillService, never()).create(any(), any());
    }

    // ==================== Update Skill ====================

    @Test
    void testUpdateSuccess() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Updated Skill");
        req.setDescription("Updated description");
        req.setCategoryId(2L);

        SkillVO vo = buildSkillVO(1L, "Updated Skill", "Updated description", 2L, "Category2", "DRAFT");
        when(skillService.update(eq(1L), any(SkillCreateRequest.class), eq(100L))).thenReturn(vo);

        mockMvc.perform(put("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Skill"));

        verify(skillService).update(eq(1L), any(SkillCreateRequest.class), eq(100L));
    }

    @Test
    void testUpdateNotFound() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Test"); req.setDescription("Test"); req.setCategoryId(1L);

        when(skillService.update(eq(999L), any(), eq(100L)))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(put("/api/v1/skills/999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void testUpdateNotAuthor() throws Exception {
        SkillCreateRequest req = new SkillCreateRequest();
        req.setName("Test"); req.setDescription("Test"); req.setCategoryId(1L);

        when(skillService.update(eq(1L), any(), eq(100L)))
                .thenThrow(new BusinessException(ErrorCode.NOT_AUTHOR));

        mockMvc.perform(put("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("非作者无权操作"));
    }

    // ==================== Get Skill By Id ====================

    @Test
    void testGetByIdSuccess() throws Exception {
        SkillVO vo = buildSkillVO(1L, "Test Skill", "Description", 1L, "Category", "PUBLISHED");
        when(skillService.getById(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Skill"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        when(skillService.getById(999L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/skills/999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    // ==================== Submit for Approval ====================

    @Test
    void testSubmitSuccess() throws Exception {
        doNothing().when(skillService).submitForApproval(1L, 100L);

        mockMvc.perform(post("/api/v1/skills/1/submit")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(skillService).submitForApproval(1L, 100L);
    }

    @Test
    void testSubmitInvalidStatus() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_STATUS))
                .when(skillService).submitForApproval(1L, 100L);

        mockMvc.perform(post("/api/v1/skills/1/submit")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.message").value("当前状态不允许此操作"));
    }

    @Test
    void testSubmitUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/submit"))
                .andExpect(status().isForbidden());
    }

    // ==================== Publish Version ====================

    @Test
    void testPublishVersionSuccess() throws Exception {
        VersionCreateRequest req = new VersionCreateRequest();
        req.setVersion("1.0.0");
        req.setChangelog("Initial release");

        VersionVO vo = new VersionVO();
        vo.setId(1L);
        vo.setSkillId(1L);
        vo.setVersion("1.0.0");
        vo.setChangelog("Initial release");
        vo.setCreateTime(LocalDateTime.now());

        when(skillService.publishVersion(eq(1L), any(VersionCreateRequest.class), eq(100L))).thenReturn(vo);

        mockMvc.perform(post("/api/v1/skills/1/versions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.version").value("1.0.0"));

        verify(skillService).publishVersion(eq(1L), any(VersionCreateRequest.class), eq(100L));
    }

    @Test
    void testPublishVersionValidationFailure() throws Exception {
        VersionCreateRequest req = new VersionCreateRequest();

        mockMvc.perform(post("/api/v1/skills/1/versions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(skillService, never()).publishVersion(any(), any(), any());
    }

    // ==================== Get Versions ====================

    @Test
    void testGetVersionsSuccess() throws Exception {
        VersionVO v1 = new VersionVO();
        v1.setId(1L); v1.setSkillId(1L); v1.setVersion("2.0.0"); v1.setChangelog("Major update");
        VersionVO v2 = new VersionVO();
        v2.setId(2L); v2.setSkillId(1L); v2.setVersion("1.0.0"); v2.setChangelog("Initial release");

        PageResult<VersionVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(v1, v2));
        pageResult.setTotal(2L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getVersions(1L, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/versions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].version").value("2.0.0"))
                .andExpect(jsonPath("$.data.records[1].version").value("1.0.0"));
    }

    @Test
    void testGetVersionsEmpty() throws Exception {
        PageResult<VersionVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(0L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(skillService.getVersions(1L, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/versions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void testGetVersionsPagination() throws Exception {
        PageResult<VersionVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(10L);
        pageResult.setSize(5L);
        pageResult.setCurrent(2L);

        when(skillService.getVersions(1L, 2, 5)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/versions")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    // ==================== Helpers ====================

    private SkillVO buildSkillVO(Long id, String name, String description,
                                  Long categoryId, String categoryName, String status) {
        SkillVO vo = new SkillVO();
        vo.setId(id);
        vo.setName(name);
        vo.setDescription(description);
        vo.setCategoryId(categoryId);
        vo.setCategoryName(categoryName);
        vo.setAuthorId(100L);
        vo.setAuthorName("testuser");
        vo.setRepoUrl("https://github.com/test/" + name.toLowerCase().replace(" ", "-"));
        vo.setDocUrl("https://docs.test.com/" + name.toLowerCase().replace(" ", "-"));
        vo.setStatus(status);
        vo.setInstallCount(0L);
        vo.setAvgRating(0.0);
        vo.setLatestVersion("1.0.0");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
