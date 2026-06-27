package com.boyi.skillops.controller;

import com.boyi.skillops.common.PageResult;
import com.boyi.skillops.dto.RatingRequest;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.security.JwtTokenProvider;
import com.boyi.skillops.service.RatingService;
import com.boyi.skillops.vo.RatingVO;
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
public class RatingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private RatingService ratingService;

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "testuser", Arrays.asList("USER"));
        reset(ratingService);
    }

    // ==================== Rate Skill ====================

    @Test
    void testRateSuccess() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(4);
        req.setComment("Good skill");

        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }

    @Test
    void testRateMinimumScore() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(1);

        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRateMaximumScore() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(5);
        req.setComment("Excellent!");

        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRateInvalidScoreTooLow() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(0);

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(any(), any(), any());
    }

    @Test
    void testRateInvalidScoreTooHigh() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(6);

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(any(), any(), any());
    }

    @Test
    void testRateNotInstalled() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(3);

        doThrow(new BusinessException(ErrorCode.NOT_INSTALLED))
                .when(ratingService).rate(eq(999L), any(), eq(100L));

        mockMvc.perform(post("/api/v1/skills/999/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003))
                .andExpect(jsonPath("$.message").value("请先安装再评分"));
    }

    @Test
    void testRateUnauthenticated() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(3);

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRateNullRating() throws Exception {
        RatingRequest req = new RatingRequest();

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(any(), any(), any());
    }

    // ==================== Update Rating ====================

    @Test
    void testUpdateRatingSuccess() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(5);
        req.setComment("Updated review");

        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(put("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }

    @Test
    void testUpdateRatingValidationFailure() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(10); // Out of range

        mockMvc.perform(put("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(any(), any(), any());
    }

    @Test
    void testUpdateRatingUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== Get Ratings ====================

    @Test
    void testGetRatingsSuccess() throws Exception {
        RatingVO r1 = buildRatingVO(1L, 100L, "user1", 5, "Excellent");
        RatingVO r2 = buildRatingVO(2L, 200L, "user2", 3, "Average");

        PageResult<RatingVO> pageResult = new PageResult<>();
        pageResult.setRecords(Arrays.asList(r1, r2));
        pageResult.setTotal(2L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(ratingService.getRatings(1L, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].username").value("user1"))
                .andExpect(jsonPath("$.data.records[0].rating").value(5))
                .andExpect(jsonPath("$.data.records[0].comment").value("Excellent"))
                .andExpect(jsonPath("$.data.records[1].username").value("user2"))
                .andExpect(jsonPath("$.data.records[1].rating").value(3));
    }

    @Test
    void testGetRatingsEmpty() throws Exception {
        PageResult<RatingVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(0L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(ratingService.getRatings(1L, 1, 20)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void testGetRatingsPagination() throws Exception {
        PageResult<RatingVO> pageResult = new PageResult<>();
        pageResult.setRecords(Collections.emptyList());
        pageResult.setTotal(50L);
        pageResult.setSize(10L);
        pageResult.setCurrent(3L);

        when(ratingService.getRatings(1L, 3, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "3")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(3))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    // ==================== Helpers ====================

    private RatingVO buildRatingVO(Long id, Long userId, String username, int rating, String comment) {
        RatingVO vo = new RatingVO();
        vo.setId(id);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setRating(rating);
        vo.setComment(comment);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }
}
