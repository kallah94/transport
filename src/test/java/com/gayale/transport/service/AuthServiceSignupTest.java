package com.gayale.transport.service;

import com.gayale.transport.dto.auth.SignupRequest;
import com.gayale.transport.dto.auth.SignupResponse;
import com.gayale.transport.model.Tenant;
import com.gayale.transport.model.User;
import com.gayale.transport.repository.RefreshTokenRepository;
import com.gayale.transport.repository.TenantRepository;
import com.gayale.transport.repository.UserRepository;
import com.gayale.transport.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider tokenProvider;
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock TenantRepository tenantRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    private SignupRequest request() {
        SignupRequest r = new SignupRequest();
        r.setTenantKey("ClientA");          // sera normalise en minuscules
        r.setTitle("Client A");
        r.setAdminUsername("admin");
        r.setAdminPassword("secret123");
        r.setAdminEmail("a@a.com");
        r.setAdminFullName("Admin A");
        return r;
    }

    @Test
    void signup_creates_tenant_and_admin() {
        when(tenantRepository.existsByKey("clienta")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(tenantRepository.save(any(Tenant.class)))
                .thenReturn(Tenant.builder().id("tid").key("clienta").build());

        SignupResponse resp = authService.signup(request());

        assertThat(resp.getTenantKey()).isEqualTo("clienta");
        assertThat(resp.getTenantId()).isEqualTo("tid");
        assertThat(resp.getAdminUsername()).isEqualTo("admin");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getTenantId()).isEqualTo("tid");
        assertThat(admin.getPassword()).isEqualTo("ENCODED");
        assertThat(admin.getRole()).isEqualTo(User.UserRole.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("a@a.com");
    }

    @Test
    void signup_rejects_duplicate_key() {
        when(tenantRepository.existsByKey("clienta")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
