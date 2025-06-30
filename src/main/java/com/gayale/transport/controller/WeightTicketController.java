package com.gayale.transport.controller;

import com.gayale.transport.dto.WeightTicketDto;
import com.gayale.transport.exception.DuplicateTicketException;
import com.gayale.transport.service.WeightTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
@SecurityRequirement(name = "JWT")
@Tag(name = "Weight Tickets", description = "API for weight ticket management")
public class WeightTicketController {

    private final WeightTicketService weightTicketService;

    @Autowired
    public WeightTicketController(WeightTicketService weightTicketService) {
        this.weightTicketService = weightTicketService;
    }

    @GetMapping
    @Operation(summary = "Get all weight tickets (paginated)", description = "Returns a paginated list of all weight tickets")
    public ResponseEntity<Page<WeightTicketDto>> getAllWeightTickets(@PageableDefault(size = 20) Pageable pageable) {
        Page<WeightTicketDto> tickets = weightTicketService.getAllWeightTicketsPaginated(pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all weight tickets", description = "Returns a list of all weight tickets (use with caution for large datasets)")
    public ResponseEntity<List<WeightTicketDto>> getAllWeightTicketsUnpaginated() {
        List<WeightTicketDto> tickets = weightTicketService.getAllWeightTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get weight ticket by ID", description = "Returns a weight ticket by its ID")
    public ResponseEntity<WeightTicketDto> getWeightTicketById(@PathVariable String id) {
        WeightTicketDto ticket = weightTicketService.getWeightTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/ticket-number/{ticketNumber}")
    @Operation(summary = "Get weight ticket by ticket number", description = "Returns a weight ticket by its ticket number")
    public ResponseEntity<WeightTicketDto> getWeightTicketByTicketNumber(@PathVariable String ticketNumber) {
        WeightTicketDto ticket = weightTicketService.getWeightTicketByTicketNumber(ticketNumber);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get weight tickets by project", description = "Returns a list of weight tickets for a specific project")
    public ResponseEntity<List<WeightTicketDto>> getWeightTicketsByProject(@PathVariable String projectId) {
        List<WeightTicketDto> tickets = weightTicketService.getWeightTicketsByProject(projectId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @Operation(summary = "Get weight tickets by purchase order", description = "Returns a list of weight tickets for a specific purchase order")
    public ResponseEntity<List<WeightTicketDto>> getWeightTicketsByPurchaseOrder(@PathVariable String purchaseOrderId) {
        List<WeightTicketDto> tickets = weightTicketService.getWeightTicketsByPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "Get weight tickets by date", description = "Returns a list of weight tickets for a specific date")
    public ResponseEntity<List<WeightTicketDto>> getWeightTicketsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<WeightTicketDto> tickets = weightTicketService.getWeightTicketsByDate(date);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get weight tickets by date range", description = "Returns a list of weight tickets between start date and end date")
    public ResponseEntity<List<WeightTicketDto>> getWeightTicketsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<WeightTicketDto> tickets = weightTicketService.getWeightTicketsByDateRange(startDate, endDate);
        return ResponseEntity.ok(tickets);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create a new weight ticket", description = "Creates a new weight ticket and returns the created weight ticket")
    public ResponseEntity<?> createWeightTicket(@Valid @RequestBody WeightTicketDto weightTicketDto) {
        try {
            WeightTicketDto createdTicket = weightTicketService.createWeightTicket(weightTicketDto);
            return new ResponseEntity<>(createdTicket, HttpStatus.CREATED);
        } catch (DuplicateTicketException e) {
            // Retourner info sur le ticket existant
            Map<String, Object> error = Map.of(
                    "error", "DUPLICATE_TICKET",
                    "message", e.getMessage(),
                    "existingTicketId", e.getExistingTicketId(),
                    "checksum", e.getChecksum(),
                    "originalTicket", weightTicketService.getOriginalTicket(e.getChecksum())
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update a weight ticket", description = "Updates a weight ticket by its ID and returns the updated weight ticket")
    public ResponseEntity<WeightTicketDto> updateWeightTicket(@PathVariable String id, @Valid @RequestBody WeightTicketDto weightTicketDto) {
        WeightTicketDto updatedTicket = weightTicketService.updateWeightTicket(id, weightTicketDto);
        return ResponseEntity.ok(updatedTicket);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a weight ticket", description = "Deletes a weight ticket by its ID")
    public ResponseEntity<Boolean> deleteWeightTicket(@PathVariable String id) {
        boolean deleted = weightTicketService.deleteWeightTicket(id);
        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/{id}/qr-code")
    @Operation(summary = "Generate QR code for a weight ticket", description = "Generates a QR code for a weight ticket by its ID")
    public ResponseEntity<Map<String, String>> generateQRCode(@PathVariable String id) {
        String qrCode = weightTicketService.generateQRCode(id);
        Map<String, String> response = new HashMap<>();
        response.put("qrCodeBase64", qrCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Validate a weight ticket", description = "Validates a weight ticket by its ID")
    public ResponseEntity<WeightTicketDto> validateWeightTicket(@PathVariable String id) {
        WeightTicketDto validatedTicket = weightTicketService.validateWeightTicket(id);
        return ResponseEntity.ok(validatedTicket);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Cancel a weight ticket", description = "Cancels a weight ticket by its ID")
    public ResponseEntity<WeightTicketDto> cancelWeightTicket(@PathVariable String id) {
        WeightTicketDto cancelledTicket = weightTicketService.cancelWeightTicket(id);
        return ResponseEntity.ok(cancelledTicket);
    }
}