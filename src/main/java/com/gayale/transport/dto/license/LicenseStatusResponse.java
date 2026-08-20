package com.gayale.transport.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Etat de licence expose au frontend (GET /api/license/status). Ne contient jamais la cle brute. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStatusResponse {

    /** ACTIVE, TRIAL, GRACE, EXPIRED, MISSING, INVALID, MACHINE_MISMATCH, REVOKED. */
    private String status;

    private boolean valid;

    private String plan;
    private String planLabel;

    private String customer;
    private String licenseId;

    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private LocalDate graceUntil;
    private Integer daysRemaining;

    private boolean trial;

    /** Vrai si l'enforcement est coupe par configuration (environnement de developpement). */
    private boolean enforcementDisabled;

    private List<String> features;

    private int maxUsers;
    private int maxTrucks;
    private int maxTicketsPerMonth;

    private long usedUsers;
    private long usedTrucks;
    private long usedTicketsThisMonth;

    /** Empreinte de ce poste, a communiquer au revendeur pour une cle liee. */
    private String machineId;

    private LocalDateTime activatedAt;
    private String activatedBy;

    /** Message d'alerte ou d'explication a afficher (expiration proche, blocage, ...). */
    private String message;
}
