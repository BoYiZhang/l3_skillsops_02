package com.boyi.skillops.service;

import com.boyi.skillops.dto.LoginRequest;
import com.boyi.skillops.dto.RegisterRequest;
import com.boyi.skillops.entity.User;
import com.boyi.skillops.entity.UserRole;
import com.boyi.skillops.enums.ErrorCode;
import com.boyi.skillops.exception.BusinessException;
import com.boyi.skillops.mapper.UserMapper;
import com.boyi.skillops.mapper.UserRoleMapper;
import com.boyi.skillops.vo.LoginResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
    }

    // ========== register ==========

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("reg_user_" + suffix);
        req.setPassword("pass123");
        req.setEmail("test@example.com");
        userService.register(req);

        // 验证用户已入库
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "reg_user_" + suffix));
        assertThat(user).isNotNull();
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getId()).isNotNull();
    }

    @Test
    void testRegisterPasswordIsEncoded() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("enc_user_" + suffix);
        req.setPassword("secret123");
        userService.register(req);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "enc_user_" + suffix));
        assertThat(user.getPassword()).isNotNull();
        // 密码应被 BCrypt 编码，不以明文存储
        assertThat(user.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", user.getPassword())).isTrue();
    }

    @Test
    void testRegisterAssignsUserRole() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("role_user_" + suffix);
        req.setPassword("pass123");
        userService.register(req);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "role_user_" + suffix));
        // 验证用户角色关联
        UserRole userRole = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        assertThat(userRole).isNotNull();
        assertThat(userRole.getRoleId()).isNotNull();
    }

    @Test
    void testRegisterDuplicateUsernameFails() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("dup_user_" + suffix);
        req1.setPassword("pass1");
        userService.register(req1);

        // 重复注册同名用户
        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("dup_user_" + suffix);
        req2.setPassword("pass2");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(req2));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(40001);
    }

    @Test
    void testRegisterNullEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("noemail_" + suffix);
        req.setPassword("pass123");
        // email is null
        userService.register(req);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "noemail_" + suffix));
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void testRegisterMultipleUsers() {
        for (int i = 0; i < 3; i++) {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("multi_" + i + "_" + suffix);
            req.setPassword("pass" + i);
            userService.register(req);
        }

        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .likeRight(User::getUsername, "multi_").eq(User::getStatus, "ACTIVE"));
        assertThat(count).isEqualTo(3);
    }

    // ========== login ==========

    @Test
    void testLoginSuccess() {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("login_ok_" + suffix);
        regReq.setPassword("correct123");
        userService.register(regReq);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("login_ok_" + suffix);
        loginReq.setPassword("correct123");

        LoginResponse resp = userService.login(loginReq);
        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isNotNull().isNotEmpty();
        assertThat(resp.getUsername()).isEqualTo("login_ok_" + suffix);
        assertThat(resp.getUserId()).isNotNull();
        assertThat(resp.getRoles()).contains("USER");
    }

    @Test
    void testLoginWrongPasswordFails() {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("badpw_" + suffix);
        regReq.setPassword("correct");
        userService.register(regReq);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("badpw_" + suffix);
        loginReq.setPassword("wrongpassword");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(loginReq));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(401);
    }

    @Test
    void testLoginNonExistentUserFails() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("nonexist_" + suffix);
        loginReq.setPassword("whatever");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(loginReq));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(401);
    }

    @Test
    void testLoginDisabledAccountFails() {
        // 注册用户后手动修改状态为 DISABLED
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("disabled_" + suffix);
        regReq.setPassword("pass123");
        userService.register(regReq);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "disabled_" + suffix));
        user.setStatus("DISABLED");
        userMapper.updateById(user);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("disabled_" + suffix);
        loginReq.setPassword("pass123");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(loginReq));
        assertThat(ex.getErrorCode().getCode()).isEqualTo(403);
    }

    @Test
    void testLoginTokenIsUniquePerUser() {
        RegisterRequest regReqA = new RegisterRequest();
        regReqA.setUsername("tokA_" + suffix);
        regReqA.setPassword("pass");
        userService.register(regReqA);

        RegisterRequest regReqB = new RegisterRequest();
        regReqB.setUsername("tokB_" + suffix);
        regReqB.setPassword("pass");
        userService.register(regReqB);

        LoginRequest loginA = new LoginRequest();
        loginA.setUsername("tokA_" + suffix);
        loginA.setPassword("pass");
        LoginResponse respA = userService.login(loginA);

        LoginRequest loginB = new LoginRequest();
        loginB.setUsername("tokB_" + suffix);
        loginB.setPassword("pass");
        LoginResponse respB = userService.login(loginB);

        assertThat(respA.getToken()).isNotEqualTo(respB.getToken());
        assertThat(respA.getUserId()).isNotEqualTo(respB.getUserId());
    }

    @Test
    void testLoginResponseContainsAllFields() {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("fullResp_" + suffix);
        regReq.setPassword("pass");
        userService.register(regReq);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("fullResp_" + suffix);
        loginReq.setPassword("pass");

        LoginResponse resp = userService.login(loginReq);
        assertThat(resp.getToken()).isNotNull().isNotEmpty();
        assertThat(resp.getUserId()).isNotNull().isPositive();
        assertThat(resp.getUsername()).isEqualTo("fullResp_" + suffix);
        assertThat(resp.getRoles()).isNotEmpty();
        assertThat(resp.getRoles()).contains("USER");
    }

    // ========== 边界与一致性 ==========

    @Test
    void testRegisterThenLoginConsistency() {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("consistent_" + suffix);
        regReq.setPassword("testpass");
        userService.register(regReq);

        // 验证 DB 中的状态
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "consistent_" + suffix));
        assertThat(user.getStatus()).isEqualTo("ACTIVE");

        // 登录
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("consistent_" + suffix);
        loginReq.setPassword("testpass");
        LoginResponse resp = userService.login(loginReq);

        // 所有 ID 一致
        assertThat(resp.getUserId()).isEqualTo(user.getId());
        assertThat(resp.getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    void testRegisterWithMinimalFields() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("minimal_" + suffix);
        req.setPassword("123456");
        // email not set
        userService.register(req);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "minimal_" + suffix));
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }
}
