package com.gayale.transport.controller;

import com.gayale.transport.dto.payment.FuelConfigRequest;
import com.gayale.transport.dto.payment.FuelConfigResponse;
import com.gayale.transport.service.FuelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fuel-config")
@SecurityRequirement(name = "JWT")
@Tag(name = "Fuel Config", description = "Configuration du carburant (prix XOF/L, facteur, mode)")
public class FuelConfigController {

    private final FuelConfigService service;

    @Autowired
    public FuelConfigController(FuelConfigService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Recuperer la configuration carburant active")
    public ResponseEntity<FuelConfigResponse> get() {
        return ResponseEntity.ok(service.getActiveConfig());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre a jour la configuration carburant")
    public ResponseEntity<FuelConfigResponse> update(@Valid @RequestBody FuelConfigRequest request) {
        return ResponseEntity.ok(service.update(request));
    }
}
