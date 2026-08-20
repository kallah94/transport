package com.gayale.transport.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Description d'un plan du catalogue, pour la page tarifaire du client. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicensePlanDto {

    private String code;
    private String label;

    /** -1 = illimite. */
    private int maxUsers;
    private int maxTrucks;
    private int maxTicketsPerMonth;

    private List<String> features;

    /** Faux pour TRIAL : genere automatiquement, jamais vendu. */
    private boolean purchasable;
}
