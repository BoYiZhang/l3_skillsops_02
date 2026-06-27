package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.MarketService;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MarketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private MarketService marketService;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        reset(marketService);
    }

    // ========== query skills ==========

    @Test
    void testQuerySkillsSuccess() throws Exception {
        SkillVO vo1 = buildSkillVO(1L, "Auto Deploy", "CI/CD automation", 1L, "DevOps");
        SkillVO vo2 = buildSkillVO(2L, "Log Analyzer", "Analyze logs with AI", 2L, "Monitoring");

        PageResult<SkillVO> page = new PageResult<>();
        page.setRecords(Arrays.asList(vo1, vo2));
        page.setTotal(2L);
        page.setSize(12L);
        page.setCurrent(1L);

        when(marketService.queryMarket(any(), eq(100L))).thenReturn(page);

        mockMvc.perform(get("/api/v1/market/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "1")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("Auto Deploy"))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("DevOps"))
                .andExpect(jsonPath("$.data.records[1].name").value("Log Analyzer"))
                .andExpect(jsonPath("$.data.records[1].categoryName").value("Monitoring"));
    }

    @Test
    void testQuerySkillsUnauthenticated() throws Exception {
        // Market query supports optional auth — controller passes null userId when no auth
        when(marketService.queryMarket(any(), isNull())).thenReturn(new PageResult<>());

        mockMvc.perform(get("/api/v1/market/skills")
                        .param("page", "1")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(marketService).queryMarket(any(), isNull());
    }

    @Test
    void testQuerySkillsWithFilters() throws Exception {
        when(marketService.queryMarket(any(), eq(100L))).thenReturn(new PageResult<>());

        mockMvc.perform(get("/api/v1/market/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .param("keyword", "python")
                        .param("categoryId", "1")
                        .param("sortBy", "rating")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(marketService).queryMarket(argThat(req ->
                "python".equals(req.getKeyword())
                        && Long.valueOf(1L).equals(req.getCategoryId())
                        && "rating".equals(req.getSortBy())
                        && req.getPage() == 1L
                        && req.getSize() == 20L), eq(100L));
    }

    @Test
    void testQuerySkillsEmptyResult() throws Exception {
        PageResult<SkillVO> emptyPage = new PageResult<>();
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(0L);
        emptyPage.setSize(12L);
        emptyPage.setCurrent(1L);

        when(marketService.queryMarket(any(), eq(100L))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/market/skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    // ========== install skill ==========

    @Test
    void testInstallSuccess() throws Exception {
        doNothing().when(marketService).install(eq(1L), eq(100L));

        mockMvc.perform(post("/api/v1/market/skills/1/install")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInstallUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/market/skills/1/install"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInstallNotFound() throws Exception {
        doThrow(new BusinessException(ErrorCode.NOT_FOUND))
                .when(marketService).install(eq(999L), eq(100L));

        mockMvc.perform(post("/api/v1/market/skills/999/install")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void testInstallAlreadyInstalled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ALREADY_INSTALLED))
                .when(marketService).install(eq(1L), eq(100L));

        mockMvc.perform(post("/api/v1/market/skills/1/install")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40004))
                .andExpect(jsonPath("$.message").value("已安装过此Skill"));
    }

    @Test
    void testInstallInternalError() throws Exception {
        doThrow(new BusinessException(ErrorCode.INTERNAL_ERROR))
                .when(marketService).install(eq(1L), eq(100L));

        mockMvc.perform(post("/api/v1/market/skills/1/install")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务端异常"));
    }

    // ========== install status ==========

    @Test
    void testInstallStatusSuccess() throws Exception {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("installed", true);
        status.put("version", "1.0.0");
        status.put("installTime", "2025-06-15T10:30:00");

        when(marketService.getInstallStatus(eq(1L), eq(100L))).thenReturn(status);

        mockMvc.perform(get("/api/v1/market/skills/1/install-status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.installed").value(true))
                .andExpect(jsonPath("$.data.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.installTime").value("2025-06-15T10:30:00"));
    }

    @Test
    void testInstallStatusUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/market/skills/1/install-status"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInstallStatusNeverInstalled() throws Exception {
        when(marketService.getInstallStatus(eq(1L), eq(100L)))
                .thenThrow(new BusinessException(ErrorCode.NOT_INSTALLED));

        mockMvc.perform(get("/api/v1/market/skills/1/install-status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003))
                .andExpect(jsonPath("$.message").value("请先安装再评分"));
    }

    // ========== helpers ==========

    private SkillVO buildSkillVO(Long id, String name, String description,
                                  Long categoryId, String categoryName) {
        SkillVO vo = new SkillVO();
        vo.setId(id);
        vo.setName(name);
        vo.setDescription(description);
        vo.setCategoryId(categoryId);
        vo.setCategoryName(categoryName);
        vo.setAuthorId(10L);
        vo.setAuthorName("author1");
        vo.setRepoUrl("https://github.com/example/" + name.toLowerCase().replace(" ", "-"));
        vo.setDocUrl("https://docs.example.com/" + name.toLowerCase().replace(" ", "-"));
        vo.setStatus("PUBLISHED");
        vo.setInstallCount(1000L);
        vo.setAvgRating(4.5);
        vo.setLatestVersion("1.2.0");
        return vo;
    }
}
