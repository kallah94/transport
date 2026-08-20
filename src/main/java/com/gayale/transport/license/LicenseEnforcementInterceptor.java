package com.gayale.transport.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gayale.transport.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Barriere commerciale centrale. Enregistree dans {@code WebConfig}, donc executee APRES la
 * chaine Spring Security : une requete non authentifiee recoit bien 401, et non 402.
 *
 * Deux verdicts possibles :
 *  1. licence absente / expiree / invalide  -> 402 sur TOUTE l'API metier ;
 *  2. licence valide mais plan insuffisant -> 402 sur les seuls prefixes du module vendu.
 *
 * Les routes d'echappement ({@code /auth}, {@code /api/license}, {@code /api/branding}, Swagger)
 * restent toujours ouvertes : sans elles, un client dont la licence expire ne pourrait plus
 * se connecter pour saisir sa nouvelle cle.
 *
 * Le gating par prefixe est la defense de premiere ligne, lisible et exhaustive. Les quotas
 * fins (utilisateurs, camions, tickets/mois) sont controles dans les services via
 * {@link LicenseGuard}, car ils demandent un comptage en base.
 */
@Slf4j
@Component
public class LicenseEnforcementInterceptor implements HandlerInterceptor {

    /** Chemins toujours accessibles, licence ou non. */
    private static final List<String> ALWAYS_ALLOWED = List.of(
            "/auth",
            "/api/license",
            "/api/branding",
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator/health",
            "/error");

    /** Prefixe d'API -> fonctionnalite requise. Le prefixe le plus long l'emporte. */
    private static final Map<String, LicenseFeature> FEATURE_BY_PREFIX = new LinkedHashMap<>();

    static {
        FEATURE_BY_PREFIX.put("/payments", LicenseFeature.PAYMENTS);
        FEATURE_BY_PREFIX.put("/driver-payments", LicenseFeature.DRIVER_PAYOUTS);
        FEATURE_BY_PREFIX.put("/driver-rates", LicenseFeature.PRICING_FUEL);
        FEATURE_BY_PREFIX.put("/transporter-rates", LicenseFeature.PRICING_FUEL);
        FEATURE_BY_PREFIX.put("/fuel-config", LicenseFeature.PRICING_FUEL);
        FEATURE_BY_PREFIX.put("/statistics", LicenseFeature.ADVANCED_STATISTICS);
        FEATURE_BY_PREFIX.put("/api/driver", LicenseFeature.DRIVER_MOBILE_APP);
        FEATURE_BY_PREFIX.put("/api/notifications", LicenseFeature.NOTIFICATIONS);
        FEATURE_BY_PREFIX.put("/tickets", LicenseFeature.WEIGHT_TICKETS);
        FEATURE_BY_PREFIX.put("/projects", LicenseFeature.PROJECTS);
        FEATURE_BY_PREFIX.put("/purchase-orders", LicenseFeature.PROJECTS);
        FEATURE_BY_PREFIX.put("/trucks", LicenseFeature.FLEET);
        FEATURE_BY_PREFIX.put("/transporter-enterprises", LicenseFeature.FLEET);
    }

    private final LicenseService licenseService;
    private final ObjectMapper objectMapper;

    public LicenseEnforcementInterceptor(LicenseService licenseService, ObjectMapper objectMapper) {
        this.licenseService = licenseService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isAlwaysAllowed(path)) {
            return true;
        }

        LicenseState state = licenseService.current();

        if (!state.isValid()) {
            deny(response, state, null,
                    state.getMessage() != null ? state.getMessage()
                            : "Licence absente ou invalide pour cette installation.");
            return false;
        }

        LicenseFeature required = requiredFeature(path);
        if (required != null && !state.has(required)) {
            deny(response, state, required,
                    "Le module « " + required.name() + " » n'est pas inclus dans votre plan "
                            + (state.getPlan() != null ? state.getPlan().getLabel() : "") + ".");
            return false;
        }

        return true;
    }

    private boolean isAlwaysAllowed(String path) {
        for (String allowed : ALWAYS_ALLOWED) {
            if (path.startsWith(allowed)) {
                return true;
            }
        }
        return false;
    }

    /** Prefixe le plus specifique (le plus long) qui correspond au chemin. */
    private LicenseFeature requiredFeature(String path) {
        LicenseFeature best = null;
        int bestLength = -1;
        for (Map.Entry<String, LicenseFeature> e : FEATURE_BY_PREFIX.entrySet()) {
            String prefix = e.getKey();
            if ((path.equals(prefix) || path.startsWith(prefix + "/")) && prefix.length() > bestLength) {
                best = e.getValue();
                bestLength = prefix.length();
            }
        }
        return best;
    }

    private void deny(HttpServletResponse response, LicenseState state, LicenseFeature feature, String message)
            throws IOException {
        response.setStatus(402); // Payment Required
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "LICENSE_REQUIRED");
        body.put("licenseStatus", state.getStatus() != null ? state.getStatus().name() : LicenseStatus.MISSING.name());
        body.put("plan", state.getPlan() != null ? state.getPlan().name() : null);
        body.put("feature", feature != null ? feature.name() : null);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
