package com.gayale.transport.service;

import com.gayale.transport.dto.payment.TransporterRateRequest;
import com.gayale.transport.dto.payment.TransporterRateResponse;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.TransporterRate;
import com.gayale.transport.repository.TransporterRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransporterRateService {

    private final TransporterRateRepository repository;

    @Autowired
    public TransporterRateService(TransporterRateRepository repository) {
        this.repository = repository;
    }

    public List<TransporterRateResponse> getAll(String transporterId) {
        List<TransporterRate> rates = (transporterId != null && !transporterId.isBlank())
                ? repository.findByTransporterId(transporterId)
                : repository.findAll();
        return rates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TransporterRateResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    public TransporterRateResponse create(TransporterRateRequest request) {
        TransporterRate rate = new TransporterRate();
        apply(rate, request);
        return toResponse(repository.save(rate));
    }

    public TransporterRateResponse update(String id, TransporterRateRequest request) {
        TransporterRate rate = findOrThrow(id);
        apply(rate, request);
        return toResponse(repository.save(rate));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Tarif transporteur introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    private TransporterRate findOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif transporteur introuvable avec l'ID : " + id));
    }

    private void apply(TransporterRate rate, TransporterRateRequest request) {
        rate.setTransporterId(request.getTransporterId());
        rate.setPricePerTonne(request.getPricePerTonne());
        // Les champs optionnels vides sont normalises en null pour signifier "s'applique a tout"
        rate.setProjectId(blankToNull(request.getProjectId()));
        rate.setProduct(blankToNull(request.getProduct()));
        rate.setEffectiveFrom(request.getEffectiveFrom());
        rate.setEffectiveTo(request.getEffectiveTo());
        rate.setActive(request.getActive() == null || request.getActive());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private TransporterRateResponse toResponse(TransporterRate rate) {
        return TransporterRateResponse.builder()
                .id(rate.getId())
                .transporterId(rate.getTransporterId())
                .pricePerTonne(rate.getPricePerTonne())
                .projectId(rate.getProjectId())
                .product(rate.getProduct())
                .effectiveFrom(rate.getEffectiveFrom())
                .effectiveTo(rate.getEffectiveTo())
                .active(rate.isActive())
                .createdAt(rate.getCreatedAt())
                .updatedAt(rate.getUpdatedAt())
                .build();
    }
}
