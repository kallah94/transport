package com.gayale.transport.controller;

import com.gayale.transport.dto.PurchaseOrderDto;
import com.gayale.transport.model.PurchaseOrder;
import com.gayale.transport.service.PurchaseOrderService;
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
@RequestMapping("/purchase-orders")
@SecurityRequirement(name = "JWT")
@Tag(name = "Purchase Orders", description = "API for purchase order management")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    @Operation(summary = "Get all purchase orders", description = "Returns a list of all purchase orders")
    public ResponseEntity<List<PurchaseOrderDto>> getAllPurchaseOrders() {
        List<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getAllPurchaseOrders();
        return ResponseEntity.ok(purchaseOrders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID", description = "Returns a purchase order by its ID")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrderById(@PathVariable String id) {
        PurchaseOrderDto purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(purchaseOrder);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get purchase orders by project", description = "Returns a list of purchase orders for a specific project")
    public ResponseEntity<List<PurchaseOrderDto>> getPurchaseOrdersByProject(@PathVariable String projectId) {
        List<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getPurchaseOrdersByProject(projectId);
        return ResponseEntity.ok(purchaseOrders);
    }

    @GetMapping("/order-number/{orderNumber}")
    @Operation(summary = "Get purchase order by order number", description = "Returns a purchase order by its order number")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrderByOrderNumber(@PathVariable String orderNumber) {
        PurchaseOrderDto purchaseOrder = purchaseOrderService.getPurchaseOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(purchaseOrder);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get purchase orders by status", description = "Returns a list of purchase orders with a specific status")
    public ResponseEntity<List<PurchaseOrderDto>> getPurchaseOrdersByStatus(@PathVariable PurchaseOrder.OrderStatus status) {
        List<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(purchaseOrders);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create a new purchase order", description = "Creates a new purchase order and returns the created purchase order")
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(@Valid @RequestBody PurchaseOrderDto purchaseOrderDto) {
        PurchaseOrderDto createdPurchaseOrder = purchaseOrderService.createPurchaseOrder(purchaseOrderDto);
        return new ResponseEntity<>(createdPurchaseOrder, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update a purchase order", description = "Updates a purchase order by its ID and returns the updated purchase order")
    public ResponseEntity<PurchaseOrderDto> updatePurchaseOrder(@PathVariable String id, @Valid @RequestBody PurchaseOrderDto purchaseOrderDto) {
        PurchaseOrderDto updatedPurchaseOrder = purchaseOrderService.updatePurchaseOrder(id, purchaseOrderDto);
        return ResponseEntity.ok(updatedPurchaseOrder);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a purchase order", description = "Deletes a purchase order by its ID")
    public ResponseEntity<Boolean> deletePurchaseOrder(@PathVariable String id) {
        boolean deleted = purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(deleted);
    }
}