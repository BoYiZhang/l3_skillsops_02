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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    }

    private RatingRequest validRequest() {
        RatingRequest req = new RatingRequest();
        req.setRating(4);
        req.setComment("Great skill!");
        return req;
    }

    // ========== POST /{id}/ratings (Create Rating) ==========

    @Test
    @DisplayName("POST /ratings - success")
    void testRateSuccess() throws Exception {
        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }

    @Test
    @DisplayName("POST /ratings - unauthenticated returns 403")
    void testRateUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(ratingService, never()).rate(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("POST /ratings - validation fails when rating < 1")
    void testRateValidationFailRatingTooLow() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(0);
        req.setComment("Too low");

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("POST /ratings - validation fails when rating > 5")
    void testRateValidationFailRatingTooHigh() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(6);
        req.setComment("Too high");

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("POST /ratings - validation fails when rating is null")
    void testRateValidationFailMissingRating() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setComment("No rating");

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(ratingService, never()).rate(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("POST /ratings - skill not found returns 404")
    void testRateSkillNotFound() throws Exception {
        doThrow(new BusinessException(ErrorCode.NOT_FOUND, "Skill not found"))
                .when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }

    @Test
    @DisplayName("POST /ratings - not installed returns 40003")
    void testRateNotInstalled() throws Exception {
        doThrow(new BusinessException(ErrorCode.NOT_INSTALLED))
                .when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }

    // ========== PUT /{id}/ratings (Update Rating) ==========

    @Test
    @DisplayName("PUT /ratings - success")
    void testUpdateRatingSuccess() throws Exception {
        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(put("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }

    @Test
    @DisplayName("PUT /ratings - unauthenticated returns 403")
    void testUpdateRatingUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(ratingService, never()).rate(anyLong(), any(), anyLong());
    }

    // ========== GET /{id}/ratings (List Ratings - PUBLIC) ==========

    @Test
    @DisplayName("GET /ratings - success with records")
    void testGetRatingsSuccess() throws Exception {
        RatingVO vo1 = new RatingVO();
        vo1.setId(1L);
        vo1.setUserId(100L);
        vo1.setUsername("testuser");
        vo1.setRating(5);
        vo1.setComment("Excellent!");
        vo1.setCreateTime(LocalDateTime.of(2026, 6, 28, 10, 0));

        RatingVO vo2 = new RatingVO();
        vo2.setId(2L);
        vo2.setUserId(200L);
        vo2.setUsername("otheruser");
        vo2.setRating(3);
        vo2.setComment("Good");
        vo2.setCreateTime(LocalDateTime.of(2026, 6, 28, 11, 0));

        List<RatingVO> records = new ArrayList<>();
        records.add(vo1);
        records.add(vo2);

        PageResult<RatingVO> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(2L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(ratingService.getRatings(eq(1L), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].userId").value(100))
                .andExpect(jsonPath("$.data.records[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.records[0].rating").value(5))
                .andExpect(jsonPath("$.data.records[0].comment").value("Excellent!"))
                .andExpect(jsonPath("$.data.records[1].username").value("otheruser"))
                .andExpect(jsonPath("$.data.records[1].rating").value(3))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.current").value(1));

        verify(ratingService).getRatings(eq(1L), eq(1), eq(20));
    }

    @Test
    @DisplayName("GET /ratings - empty result")
    void testGetRatingsEmpty() throws Exception {
        PageResult<RatingVO> pageResult = new PageResult<>();
        pageResult.setRecords(new ArrayList<>());
        pageResult.setTotal(0L);
        pageResult.setSize(20L);
        pageResult.setCurrent(1L);

        when(ratingService.getRatings(eq(1L), eq(1), eq(20))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(ratingService).getRatings(eq(1L), eq(1), eq(20));
    }

    @Test
    @DisplayName("GET /ratings - pagination params respected")
    void testGetRatingsPagination() throws Exception {
        PageResult<RatingVO> pageResult = new PageResult<>();
        pageResult.setRecords(new ArrayList<>());
        pageResult.setTotal(10L);
        pageResult.setSize(5L);
        pageResult.setCurrent(2L);

        when(ratingService.getRatings(eq(1L), eq(2), eq(5))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/skills/1/ratings")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(5));

        verify(ratingService).getRatings(eq(1L), eq(2), eq(5));
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("POST /ratings - comment is optional, succeeds with null comment")
    void testRateWithCommentOnly() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(3);
        req.setComment(null);

        doNothing().when(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ratingService).rate(eq(1L), any(RatingRequest.class), eq(100L));
    }
}
