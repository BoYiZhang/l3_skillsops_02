package com.boyi.skillops.service;

import com.boyi.skillops.dto.LoginRequest;
import com.boyi.skillops.dto.RegisterRequest;
import com.boyi.skillops.vo.LoginResponse;

public interface UserService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
