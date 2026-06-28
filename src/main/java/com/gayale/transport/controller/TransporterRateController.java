package com.gayale.transport.controller;

import com.gayale.transport.dto.payment.TransporterRateRequest;
import com.gayale.transport.dto.payment.TransporterRateResponse;
import com.gayale.transport.service.TransporterRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transporter-rates")
@SecurityRequirement(name = "JWT")
@Tag(name = "Transporter Rates", description = "Gestion des tarifs transporteur (XOF/tonne)")
public class TransporterRateController {

    private final TransporterRateService service;

    @Autowired
    public TransporterRateController(TransporterRateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister les tarifs transporteur")
    public ResponseEntity<List<TransporterRateResponse>> getAll(
            @RequestParam(required = false) String transporterId) {
        return ResponseEntity.ok(service.getAll(transporterId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'un tarif transporteur")
    public ResponseEntity<TransporterRateResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Creer un tarif transporteur")
    public ResponseEntity<TransporterRateResponse> create(@Valid @RequestBody TransporterRateRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un tarif transporteur")
    public ResponseEntity<TransporterRateResponse> update(@PathVariable String id,
                                                          @Valid @RequestBody TransporterRateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un tarif transporteur")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
