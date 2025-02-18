package com.gym.dto;

import com.gym.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private User.Role role;
}
