package com.gayale.transport.dto.auth;

import com.gayale.transport.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String token;
    private UserDto user;
    private String refreshToken;
    private long expiresIn;
}