package com.gayale.transport.service;

import com.gayale.transport.dto.payment.DriverRateRequest;
import com.gayale.transport.dto.payment.DriverRateResponse;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.DriverRate;
import com.gayale.transport.repository.DriverRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverRateService {

    private final DriverRateRepository repository;

    @Autowired
    public DriverRateService(DriverRateRepository repository) {
        this.repository = repository;
    }

    public List<DriverRateResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DriverRateResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    public DriverRateResponse create(DriverRateRequest request) {
        DriverRate rate = new DriverRate();
        apply(rate, request);
        return toResponse(repository.save(rate));
    }

    public DriverRateResponse update(String id, DriverRateRequest request) {
        DriverRate rate = findOrThrow(id);
        apply(rate, request);
        return toResponse(repository.save(rate));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Tarif chauffeur introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    private DriverRate findOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif chauffeur introuvable avec l'ID : " + id));
    }

    private void apply(DriverRate rate, DriverRateRequest request) {
        rate.setPricePerTonne(request.getPricePerTonne());
        // transporterId vide => null (bareme global, s'applique a tous)
        rate.setTransporterId(blankToNull(request.getTransporterId()));
        rate.setEffectiveFrom(request.getEffectiveFrom());
        rate.setEffectiveTo(request.getEffectiveTo());
        rate.setActive(request.getActive() == null || request.getActive());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private DriverRateResponse toResponse(DriverRate rate) {
        return DriverRateResponse.builder()
                .id(rate.getId())
                .pricePerTonne(rate.getPricePerTonne())
                .transporterId(rate.getTransporterId())
                .effectiveFrom(rate.getEffectiveFrom())
                .effectiveTo(rate.getEffectiveTo())
                .active(rate.isActive())
                .createdAt(rate.getCreatedAt())
                .updatedAt(rate.getUpdatedAt())
                .build();
    }
}
