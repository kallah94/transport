package com.gayale.transport.controller;

import com.gayale.transport.dto.license.LicenseActivationRequest;
import com.gayale.transport.dto.license.LicensePlanDto;
import com.gayale.transport.dto.license.LicenseStatusResponse;
import com.gayale.transport.license.LicenseFeature;
import com.gayale.transport.license.LicenseGuard;
import com.gayale.transport.license.LicensePlan;
import com.gayale.transport.license.LicenseState;
import com.gayale.transport.model.License;
import com.gayale.transport.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Administration de la licence.
 *
 * Ce controleur est volontairement HORS du perimetre de l'enforcement (cf.
 * LicenseEnforcementFilter) : un client dont la licence a expire doit pouvoir se connecter,
 * consulter son etat et saisir sa nouvelle cle. L'authentification reste requise.
 */
@RestController
@RequestMapping("/api/license")
@Tag(name = "Licence", description = "Activation et etat de la licence du deploiement")
public class LicenseController {

    private final LicenseService licenseService;
    private final LicenseGuard licenseGuard;

    public LicenseController(LicenseService licenseService, LicenseGuard licenseGuard) {
        this.licenseService = licenseService;
        this.licenseGuard = licenseGuard;
    }

    @GetMapping("/status")
    @Operation(summary = "Etat de la licence courante, quotas et consommation")
    public ResponseEntity<LicenseStatusResponse> status() {
        return ResponseEntity.ok(buildStatus());
    }

    @PostMapping("/activate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Active une cle de licence fournie par l'editeur")
    public ResponseEntity<LicenseStatusResponse> activate(@Valid @RequestBody LicenseActivationRequest request,
                                                          Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "inconnu";
        licenseService.activate(request.getLicenseKey(), actor);
        return ResponseEntity.ok(buildStatus());
    }

    @DeleteMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Retire la licence active de ce deploiement")
    public ResponseEntity<LicenseStatusResponse> deactivate() {
        licenseService.deactivate();
        return ResponseEntity.ok(buildStatus());
    }

    @GetMapping("/machine-id")
    @Operation(summary = "Empreinte de ce poste, a transmettre pour obtenir une cle liee")
    public ResponseEntity<String> machineId() {
        return ResponseEntity.ok(licenseService.machineId());
    }

    @GetMapping("/plans")
    @Operation(summary = "Catalogue des plans et de leurs limites")
    public ResponseEntity<List<LicensePlanDto>> plans() {
        List<LicensePlanDto> plans = Arrays.stream(LicensePlan.values())
                .map(p -> LicensePlanDto.builder()
                        .code(p.name())
                        .label(p.getLabel())
                        .maxUsers(p.getMaxUsers())
                        .maxTrucks(p.getMaxTrucks())
                        .maxTicketsPerMonth(p.getMaxTicketsPerMonth())
                        .features(p.getFeatures().stream().map(LicenseFeature::name).sorted().toList())
                        .purchasable(p != LicensePlan.TRIAL)
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(plans);
    }

    // ------------------------------------------------------------------ interne

    private LicenseStatusResponse buildStatus() {
        LicenseState state = licenseService.current();
        LicenseGuard.LicenseUsage usage = licenseGuard.usage();
        Optional<License> record = licenseService.currentRecord();

        return LicenseStatusResponse.builder()
                .status(state.getStatus() != null ? state.getStatus().name() : null)
                .valid(state.isValid())
                .plan(state.getPlan() != null ? state.getPlan().name() : null)
                .planLabel(state.getPlan() != null ? state.getPlan().getLabel() : null)
                .customer(state.getCustomer())
                .licenseId(state.getLicenseId())
                .issuedAt(state.getIssuedAt())
                .expiresAt(state.getExpiresAt())
                .graceUntil(state.getGraceUntil())
                .daysRemaining(state.getDaysRemaining())
                .trial(state.isTrial())
                .enforcementDisabled(state.isEnforcementDisabled())
                .features(state.getFeatures().stream().map(LicenseFeature::name).sorted().toList())
                .maxUsers(state.getMaxUsers())
                .maxTrucks(state.getMaxTrucks())
                .maxTicketsPerMonth(state.getMaxTicketsPerMonth())
                .usedUsers(usage.users())
                .usedTrucks(usage.trucks())
                .usedTicketsThisMonth(usage.ticketsThisMonth())
                .machineId(licenseService.machineId())
                .activatedAt(record.map(License::getActivatedAt).orElse(null))
                .activatedBy(record.map(License::getActivatedBy).orElse(null))
                .message(state.getMessage())
                .build();
    }
}
