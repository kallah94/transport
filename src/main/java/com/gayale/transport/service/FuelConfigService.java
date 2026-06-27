package com.gayale.transport.service;

import com.gayale.transport.dto.payment.FuelConfigRequest;
import com.gayale.transport.dto.payment.FuelConfigResponse;
import com.gayale.transport.model.FuelConfig;
import com.gayale.transport.repository.FuelConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FuelConfigService {

    private final FuelConfigRepository repository;

    @Autowired
    public FuelConfigService(FuelConfigRepository repository) {
        this.repository = repository;
    }

    // Retourne la configuration active, ou une configuration par defaut (non persistee).
    public FuelConfigResponse getActiveConfig() {
        return repository.findFirstByActiveTrueOrderByEffectiveFromDesc()
                .map(this::toResponse)
                .orElseGet(() -> FuelConfigResponse.builder()
                        .fuelPricePerLitre(0)
                        .roundTripFactor(2.0)
                        .dotationMode(FuelConfig.DotationMode.INCLUS)
                        .active(true)
                        .build());
    }

    // Met a jour (ou cree) la configuration active unique.
    public FuelConfigResponse update(FuelConfigRequest request) {
        FuelConfig config = repository.findFirstByActiveTrueOrderByEffectiveFromDesc()
                .orElseGet(FuelConfig::new);
        config.setFuelPricePerLitre(request.getFuelPricePerLitre());
        config.setRoundTripFactor(request.getRoundTripFactor() > 0 ? request.getRoundTripFactor() : 2.0);
        config.setDotationMode(request.getDotationMode());
        config.setEffectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDate.now());
        config.setActive(true);
        return toResponse(repository.save(config));
    }

    private FuelConfigResponse toResponse(FuelConfig config) {
        return FuelConfigResponse.builder()
                .id(config.getId())
                .fuelPricePerLitre(config.getFuelPricePerLitre())
                .roundTripFactor(config.getRoundTripFactor())
                .dotationMode(config.getDotationMode())
                .effectiveFrom(config.getEffectiveFrom())
                .active(config.isActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
