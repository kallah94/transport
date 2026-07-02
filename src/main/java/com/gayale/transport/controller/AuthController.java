package com.gayale.transport.controller;

import com.gayale.transport.dto.auth.LoginRequest;
import com.gayale.transport.dto.auth.LoginResponse;
import com.gayale.transport.dto.auth.RefreshTokenRequest;
import com.gayale.transport.dto.auth.RefreshTokenResponse;
import com.gayale.transport.dto.auth.SignupRequest;
import com.gayale.transport.dto.auth.SignupResponse;
import com.gayale.transport.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "API for user authentication and token management")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final AuthService authService;

    /** En prod web (HTTPS, domaines distincts) : secure=true, same-site=None. En local : false / Lax. */
    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.mode:dedicated}")
    private String mode;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate JWT token",
            description = "Takes username and password, returns JWT token with user details")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                               HttpServletResponse servletResponse) {
        LoginResponse response = authService.login(loginRequest);
        addAccessTokenCookie(servletResponse, response.getToken(), response.getExpiresIn());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "Auto-inscription d'un transporteur (mode shared uniquement)",
            description = "Cree un nouveau tenant (sous-domaine) et son administrateur")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        if (!"shared".equalsIgnoreCase(mode)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token",
            description = "Takes a refresh token and returns a new JWT token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                             HttpServletResponse servletResponse) {
        RefreshTokenResponse response = authService.refreshToken(request);
        addAccessTokenCookie(servletResponse, response.getToken(), response.getExpiresIn());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user",
            description = "Invalidates the refresh token of the currently authenticated user")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse servletResponse) {
        if (authentication != null) {
            authService.logout(authentication.getName());
        }
        clearAccessTokenCookie(servletResponse);
        return ResponseEntity.ok().build();
    }

    /** Depose le jeton dans un cookie httpOnly (inaccessible au JS -> protege du XSS cote web). */
    private void addAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
