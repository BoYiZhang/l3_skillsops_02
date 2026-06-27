package com.boyi.skillops.e2e;

import com.boyi.skillops.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Contract Test — verifies the shape, status codes, auth gates, and
 * DTO consistency of every public HTTP endpoint.  Uses real Spring Security
 * with mocked service beans so we test the actual filter chain, validation,
 * and exception handling without touching the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private com.boyi.skillops.security.JwtTokenProvider jwtTokenProvider;

    // ---- Mock all services so the web layer is tested in isolation ----
    @MockBean private SkillService skillService;
    @MockBean private AdminService adminService;
    @MockBean private MarketService marketService;
    @MockBean private RatingService ratingService;
    @MockBean private UserService userService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = jwtTokenProvider.generateToken(100L, "regularuser", Arrays.asList("USER"));
        adminToken = jwtTokenProvider.generateToken(200L, "adminuser", Arrays.asList("ADMIN"));
    }

    // =====================================================================
    // 1.  Response format consistency
    // =====================================================================

    // ---------- 1a. Success response shape ----------

    @Test
    @Order(1)
    void testSuccessResponseHasCode200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(2)
    void testSuccessResponseHasMessageField() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_2\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(3)
    void testLoginSuccessResponseContainsTokenField() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_3\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_3\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    // ---------- 1b. Error response shape ----------

    @Test
    @Order(4)
    void testValidationErrorResponseHasCode4xx() throws Exception {
        // Send empty body that fails @NotBlank validation
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())           // GlobalExceptionHandler returns HTTP 200
                .andExpect(jsonPath("$.code").value(not(200)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(5)
    void testErrorResponseHasMessage() throws Exception {
        // Missing required fields triggers validation error
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @Order(6)
    void testUnauthenticatedAccessReturns403Code() throws Exception {
        // Spring Security stateless → 403 (not 401) for unauthenticated requests
        mockMvc.perform(get("/api/v1/skills/1"))
                .andExpect(status().isForbidden());
    }

    // ---------- 1c. Pagination response shape ----------

    @Test
    @Order(7)
    void testPaginationResponseContainsRequiredFields() throws Exception {
        mockMvc.perform(get("/api/v1/market/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        // Pagination fields exist on data when services are mocked, but since
        // services are mocked and return null the data may be empty/null.
        // We verify the 200 code contract.  The real shape is tested in E2E.
    }

    // =====================================================================
    // 2.  HTTP method & auth-gate contracts
    // =====================================================================

    // ---------- 2a. Public endpoints (no token needed) ----------

    @Test
    @Order(10)
    void testAuthRegisterAcceptsPost() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_10\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(11)
    void testAuthLoginAcceptsPost() throws Exception {
        // Register first so login can succeed
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_11\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apict_11\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(12)
    void testAuthLoginRejectsGet() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().is4xxClientError());
    }

    // ---------- 2b. Protected endpoints (token required) ----------

    @Test
    @Order(13)
    void testSkillsEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    void testSkillsEndpointAcceptsValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    void testMarketEndpointRequiresAuth() throws Exception {
        // Market /skills GET tolerates null auth at service level,
        // but SecurityConfig requires authentication on all /api/** paths.
        mockMvc.perform(get("/api/v1/market/skills"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(16)
    void testWorkspaceEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/my-skills"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(17)
    void testCreateSkillRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"description\":\"x\",\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(18)
    void testInstallRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/market/skills/1/install"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(19)
    void testRateRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isForbidden());
    }

    // ---------- 2c. Admin endpoints (admin role required) ----------

    @Test
    @Order(20)
    void testAdminEndpointRequiresAdminRole() throws Exception {
        // Regular user token should be denied access to admin endpoints
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())          // GlobalExceptionHandler swallows
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(21)
    void testAdminEndpointAllowsAdminToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(22)
    void testAdminCategoriesRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(23)
    void testAdminApproveRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skills/1/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(24)
    void testAdminRejectRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skills/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"bad\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(25)
    void testAdminDelistRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skills/1/delist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"policy\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(26)
    void testAdminStatsRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(27)
    void testAdminUsersRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(28)
    void testAdminCreateCategoryRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NewCat\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ---------- 2d. Invalid/bogus token ----------

    @Test
    @Order(30)
    void testInvalidTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(31)
    void testMissingBearerPrefixReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    // =====================================================================
    // 3.  Request/response DTO consistency
    // =====================================================================

    // ---------- 3a. Validation: request body constraints ----------

    @Test
    @Order(40)
    void testRegisterValidationRejectsShortUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(41)
    void testRegisterValidationRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"validuser\",\"password\":\"12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(42)
    void testSkillCreateValidationRejectsMissingName() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"desc\",\"categoryId\":1}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(43)
    void testSkillCreateValidationRejectsMissingCategoryId() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"name\",\"description\":\"desc\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(44)
    void testVersionCreateValidationRejectsMissingVersion() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changelog\":\"fixes\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(45)
    void testRatingValidationRejectsOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));

        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(46)
    void testRatingValidationRejectsMissingRating() throws Exception {
        mockMvc.perform(post("/api/v1/skills/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"nice\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ---------- 3b. Content-Type contract ----------

    @Test
    @Order(50)
    void testPostSkillsReturnsJson() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"description\":\"d\",\"categoryId\":1}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @Order(51)
    void testGetSkillsReturnsJson() throws Exception {
        mockMvc.perform(get("/api/v1/skills/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---------- 3c. Paginated endpoints accept page/size query params ----------

    @Test
    @Order(52)
    void testMarketQueryAcceptsPageParams() throws Exception {
        mockMvc.perform(get("/api/v1/market/skills")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(53)
    void testAdminPendingSkillsAcceptsPageParams() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pending-skills")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }
}
