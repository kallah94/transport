package com.gayale.transport.service;

import com.gayale.transport.dto.UserDto;
import com.gayale.transport.dto.auth.LoginRequest;
import com.gayale.transport.dto.auth.LoginResponse;
import com.gayale.transport.dto.auth.RefreshTokenRequest;
import com.gayale.transport.dto.auth.RefreshTokenResponse;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.exception.UnauthorizedException;
import com.gayale.transport.model.RefreshToken;
import com.gayale.transport.model.User;
import com.gayale.transport.dto.auth.SignupRequest;
import com.gayale.transport.dto.auth.SignupResponse;
import com.gayale.transport.model.Tenant;
import com.gayale.transport.repository.RefreshTokenRepository;
import com.gayale.transport.repository.TenantRepository;
import com.gayale.transport.repository.UserRepository;
import com.gayale.transport.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.security.jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Auto-inscription d'un transporteur (mode shared) : cree le tenant + son administrateur.
     * Le tenantId de l'admin est pose explicitement (le tenant n'existe pas encore au moment
     * ou le contexte serait resolu).
     */
    public SignupResponse signup(SignupRequest request) {
        String key = request.getTenantKey().toLowerCase();
        if (tenantRepository.existsByKey(key)) {
            throw new IllegalArgumentException("Cette cle (sous-domaine) est deja utilisee : " + key);
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .key(key)
                .title(request.getTitle())
                .active(true)
                .theme(new Tenant.Theme("#3f51b5", "#1a237e", "#ff4081", "#121212", "#1e1e1e"))
                .build());

        User admin = new User();
        admin.setTenantId(tenant.getId());
        admin.setUsername(request.getAdminUsername());
        admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
        admin.setEmail(request.getAdminEmail());
        admin.setFullName(request.getAdminFullName());
        admin.setRole(User.UserRole.ADMIN);
        userRepository.save(admin);

        return SignupResponse.builder()
                .tenantKey(key)
                .tenantId(tenant.getId())
                .adminUsername(admin.getUsername())
                .message("Compte cree. Connectez-vous sur le sous-domaine '" + key + "'.")
                .build();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + loginRequest.getUsername()));

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user); // Uncomment this line to save the user with updated last login time
        //userRepository = userRepository.save(user);

        // Create refresh token
        RefreshToken refreshToken = createRefreshToken(user.getId());

        return LoginResponse.builder()
                            .token(jwt)
                            .refreshToken(refreshToken.getToken())
                            .expiresIn(jwtExpirationMs / 1000)
                            .user(mapUserToDto(user))
                            .build();
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                                     .map(this::verifyExpiration)
                                     .map(RefreshToken::getUserId)
                                     .map(userId -> {
                                         User user = userRepository.findById(userId)
                                                                   .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

                                         // Create new tokens with a proper UserDetails principal and authorities
                                         List<SimpleGrantedAuthority> authorities = List.of(
                                                 new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                                         UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                                                 user.getUsername(), user.getPassword(), authorities);
                                         Authentication authentication = new UsernamePasswordAuthenticationToken(
                                                 userDetails, null, authorities);
                                         String newToken = tokenProvider.generateToken(authentication);
                                         RefreshToken newRefreshToken = createRefreshToken(user.getId());

                                         return RefreshTokenResponse.builder()
                                                                    .token(newToken)
                                                                    .refreshToken(newRefreshToken.getToken())
                                                                    .expiresIn(jwtExpirationMs / 1000)
                                                                    .build();
                                     })
                                     .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
    }

    public void logout(String username) {
        userRepository.findByUsername(username)
                      .ifPresent(user -> refreshTokenRepository.findByUserId(user.getId())
                                                               .ifPresent(refreshTokenRepository::delete));
    }

    private RefreshToken createRefreshToken(String userId) {
        // Delete any existing refresh tokens for this user
        refreshTokenRepository.findByUserId(userId)
                              .ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token was expired. Please make a new login request");
        }
        return token;
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                      .id(user.getId())
                      .username(user.getUsername())
                      .fullName(user.getFullName())
                      .email(user.getEmail())
                      .role(user.getRole())
                      .lastLogin(user.getLastLogin())
                      .createdAt(user.getCreatedAt())
                      .build();
    }
}