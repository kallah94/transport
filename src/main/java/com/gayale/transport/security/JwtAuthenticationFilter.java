package com.gayale.transport.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final boolean shared;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService, boolean shared) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.shared = shared;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Check if the request is for a public path (like Swagger)
            String requestPath = request.getServletPath();
            if (isPublicPath(requestPath)) {
                // If it's a public path, let it pass without checking the token
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && tenantMatches(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);

                if (userDetailsService != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            tokenProvider.getAuthentication(jwt, userDetails);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * En mode "shared", n'authentifie que si le tenant du jeton correspond au tenant du
     * sous-domaine courant (anti-usurpation inter-tenant). En "dedicated" : toujours vrai.
     */
    private boolean tenantMatches(String jwt) {
        if (!shared) {
            return true;
        }
        String tokenTenant = tokenProvider.getTenantIdFromToken(jwt);
        String currentTenant = com.gayale.transport.tenant.TenantContext.getTenantId();
        return currentTenant != null && currentTenant.equals(tokenTenant);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-ui.html") ||
                path.startsWith("/auth/");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader(TOKEN_HEADER);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
                return bearerToken.substring(TOKEN_PREFIX.length());
            }
            // Repli : jeton porte par un cookie httpOnly (auth web securisee, non lisible par JS)
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting JWT from request", e);
            return null;
        }
    }
}