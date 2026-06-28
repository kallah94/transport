package com.gayale.transport.controller;

import com.gayale.transport.dto.payment.DriverRateRequest;
import com.gayale.transport.dto.payment.DriverRateResponse;
import com.gayale.transport.service.DriverRateService;
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
@RequestMapping("/driver-rates")
@SecurityRequirement(name = "JWT")
@Tag(name = "Driver Rates", description = "Gestion des tarifs chauffeur (XOF/tonne)")
public class DriverRateController {

    private final DriverRateService service;

    @Autowired
    public DriverRateController(DriverRateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister les tarifs chauffeur")
    public ResponseEntity<List<DriverRateResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'un tarif chauffeur")
    public ResponseEntity<DriverRateResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Creer un tarif chauffeur")
    public ResponseEntity<DriverRateResponse> create(@Valid @RequestBody DriverRateRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un tarif chauffeur")
    public ResponseEntity<DriverRateResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody DriverRateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un tarif chauffeur")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
