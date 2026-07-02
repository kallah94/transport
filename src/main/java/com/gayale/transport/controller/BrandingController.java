package com.gayale.transport.controller;

import com.gayale.transport.dto.BrandingDto;
import com.gayale.transport.model.Tenant;
import com.gayale.transport.repository.TenantRepository;
import com.gayale.transport.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Branding public : le frontend appelle GET /api/branding?host=clientA.app.com AVANT login
 * pour thematiser l'application (logo/titre/couleurs) selon le tenant. Repli sur le branding
 * par defaut si le tenant est inconnu.
 */
@RestController
@RequestMapping("/api/branding")
@Tag(name = "Branding", description = "Identite visuelle publique par tenant")
public class BrandingController {

    private final TenantRepository tenantRepository;

    public BrandingController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    @Operation(summary = "Branding d'un tenant resolu par sous-domaine")
    public ResponseEntity<BrandingDto> getBranding(@RequestParam(value = "host", required = false) String host) {
        String key = subdomainOf(host);
        BrandingDto dto = (key == null
                ? java.util.Optional.<Tenant>empty()
                : tenantRepository.findByKeyAndActiveTrue(key))
                .map(this::toDto)
                .orElseGet(this::defaultBranding);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Met a jour le branding du tenant courant (admin)")
    public ResponseEntity<BrandingDto> updateBranding(@RequestBody BrandingDto dto) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }
        return tenantRepository.findById(tenantId)
                .map(t -> {
                    if (dto.getTitle() != null) {
                        t.setTitle(dto.getTitle());
                    }
                    if (dto.getLogoUrl() != null) {
                        t.setLogoUrl(dto.getLogoUrl());
                    }
                    if (dto.getTheme() != null) {
                        BrandingDto.ThemeDto th = dto.getTheme();
                        t.setTheme(new Tenant.Theme(th.getPrimary(), th.getPrimaryDark(),
                                th.getAccent(), th.getBackground(), th.getSurface()));
                    }
                    tenantRepository.save(t);
                    return ResponseEntity.ok(toDto(t));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private BrandingDto toDto(Tenant t) {
        BrandingDto def = defaultBranding();
        Tenant.Theme th = t.getTheme();
        BrandingDto.ThemeDto theme = (th == null) ? def.getTheme() : BrandingDto.ThemeDto.builder()
                .primary(coalesce(th.getPrimary(), def.getTheme().getPrimary()))
                .primaryDark(coalesce(th.getPrimaryDark(), def.getTheme().getPrimaryDark()))
                .accent(coalesce(th.getAccent(), def.getTheme().getAccent()))
                .background(coalesce(th.getBackground(), def.getTheme().getBackground()))
                .surface(coalesce(th.getSurface(), def.getTheme().getSurface()))
                .build();
        return BrandingDto.builder()
                .key(t.getKey())
                .title(coalesce(t.getTitle(), def.getTitle()))
                .subtitle(def.getSubtitle())
                .logoUrl(coalesce(t.getLogoUrl(), def.getLogoUrl()))
                .faviconUrl(def.getFaviconUrl())
                .theme(theme)
                .build();
    }

    private BrandingDto defaultBranding() {
        return BrandingDto.builder()
                .key("gayale")
                .title("Gayale Transport")
                .subtitle("Système de Gestion de Transport de Gravier")
                .logoUrl("assets/images/logo.png")
                .faviconUrl("favicon.ico")
                .theme(BrandingDto.ThemeDto.builder()
                        .primary("#3f51b5")
                        .primaryDark("#1a237e")
                        .accent("#ff4081")
                        .background("#121212")
                        .surface("#1e1e1e")
                        .build())
                .build();
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    /** clientA.app.com -> "clientA" ; "localhost" / IP -> null. */
    private String subdomainOf(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        int dot = host.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        return host.substring(0, dot);
    }
}
