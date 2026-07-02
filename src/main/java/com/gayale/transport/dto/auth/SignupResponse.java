package com.gayale.transport.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupResponse {
    private String tenantKey;
    private String tenantId;
    private String adminUsername;
    private String message;
}
