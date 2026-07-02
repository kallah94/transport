package com.gayale.transport.security;

import com.gayale.transport.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTenantTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret",
                "test-secret-test-secret-test-secret-1234567890");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3_600_000L);
        provider.init();
    }

    @AfterEach
    void clean() {
        TenantContext.clear();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        User principal = new User("alice", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return new UsernamePasswordAuthenticationToken(principal, "x", principal.getAuthorities());
    }

    @Test
    void token_carries_tenantId_when_context_is_set() {
        TenantContext.setTenantId("tenant-A");

        String token = provider.generateToken(authentication());

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(provider.getTenantIdFromToken(token)).isEqualTo("tenant-A");
    }

    @Test
    void token_has_no_tenantId_when_context_is_absent() {
        String token = provider.generateToken(authentication());

        assertThat(provider.getTenantIdFromToken(token)).isNull();
    }
}
