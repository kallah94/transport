package com.gayale.transport.controller;

import com.gayale.transport.dto.payment.DriverPayoutResponse;
import com.gayale.transport.dto.payment.PaymentGenerationRequest;
import com.gayale.transport.dto.payment.PaymentStatementResponse;
import com.gayale.transport.model.PaymentStatement;
import com.gayale.transport.service.PaymentService;
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
@RequestMapping("/payments")
@SecurityRequirement(name = "JWT")
@Tag(name = "Payments", description = "Generation et suivi des paiements transporteurs")
public class PaymentController {

    private final PaymentService service;

    @Autowired
    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Apercu du paiement (sans persistance)")
    public ResponseEntity<PaymentStatementResponse> preview(@Valid @RequestBody PaymentGenerationRequest request) {
        return ResponseEntity.ok(service.preview(request));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generer et enregistrer un releve de paiement")
    public ResponseEntity<PaymentStatementResponse> generate(@Valid @RequestBody PaymentGenerationRequest request) {
        return new ResponseEntity<>(service.generate(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lister les releves de paiement")
    public ResponseEntity<List<PaymentStatementResponse>> list(
            @RequestParam(required = false) String transporterId,
            @RequestParam(required = false) PaymentStatement.PaymentStatus status) {
        return ResponseEntity.ok(service.list(transporterId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'un releve de paiement")
    public ResponseEntity<PaymentStatementResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/driver-payouts")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Tableau de bord paiement chauffeurs par camion (par bon de commande)")
    public ResponseEntity<DriverPayoutResponse> driverPayouts(
            @RequestParam String transporterId,
            @RequestParam String purchaseOrderId) {
        return ResponseEntity.ok(service.getDriverPayouts(transporterId, purchaseOrderId));
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider un releve (DRAFT -> VALIDATED)")
    public ResponseEntity<PaymentStatementResponse> validate(@PathVariable String id) {
        return ResponseEntity.ok(service.validate(id));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marquer un releve comme paye (VALIDATED -> PAID)")
    public ResponseEntity<PaymentStatementResponse> pay(@PathVariable String id) {
        return ResponseEntity.ok(service.markPaid(id));
    }
}
