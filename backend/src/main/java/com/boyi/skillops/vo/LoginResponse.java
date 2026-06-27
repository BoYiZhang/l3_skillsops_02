package com.boyi.skillops.vo;

import lombok.Data;
import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private List<String> roles;
}
