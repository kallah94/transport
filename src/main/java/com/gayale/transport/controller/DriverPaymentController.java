package com.gayale.transport.controller;

import com.gayale.transport.dto.payment.DriverPayoutResponse;
import com.gayale.transport.model.DriverPaymentStatement;
import com.gayale.transport.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/driver-payments")
@SecurityRequirement(name = "JWT")
@Tag(name = "Driver Payments", description = "Bons de paiement chauffeur (par bon de commande)")
public class DriverPaymentController {

    private final PaymentService service;

    @Autowired
    public DriverPaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Apercu du paiement chauffeur (sans persistance)")
    public ResponseEntity<DriverPayoutResponse> preview(
            @RequestParam String transporterId,
            @RequestParam String purchaseOrderId) {
        return ResponseEntity.ok(service.getDriverPayouts(transporterId, purchaseOrderId));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generer et enregistrer un bon de paiement chauffeur")
    public ResponseEntity<DriverPayoutResponse> generate(
            @RequestParam String transporterId,
            @RequestParam String purchaseOrderId) {
        return new ResponseEntity<>(service.generateDriverPayment(transporterId, purchaseOrderId), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lister les bons de paiement chauffeur")
    public ResponseEntity<List<DriverPayoutResponse>> list(
            @RequestParam(required = false) String transporterId,
            @RequestParam(required = false) DriverPaymentStatement.PaymentStatus status) {
        return ResponseEntity.ok(service.listDriverPayments(transporterId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'un bon de paiement chauffeur")
    public ResponseEntity<DriverPayoutResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getDriverPaymentById(id));
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider (DRAFT -> VALIDATED)")
    public ResponseEntity<DriverPayoutResponse> validate(@PathVariable String id) {
        return ResponseEntity.ok(service.validateDriverPayment(id));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marquer paye (VALIDATED -> PAID)")
    public ResponseEntity<DriverPayoutResponse> pay(@PathVariable String id) {
        return ResponseEntity.ok(service.payDriverPayment(id));
    }
}
