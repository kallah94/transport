package com.gayale.transport.service;

import com.gayale.transport.dto.PurchaseOrderDto;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.PurchaseOrder;
import com.gayale.transport.repository.PurchaseOrderRepository;
import com.gayale.transport.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                ProjectRepository projectRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.projectRepository = projectRepository;
    }

    public List<PurchaseOrderDto> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll().stream()
                                      .map(this::mapPurchaseOrderToDto)
                                      .collect(Collectors.toList());
    }

    public PurchaseOrderDto getPurchaseOrderById(String id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                                                             .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
        return mapPurchaseOrderToDto(purchaseOrder);
    }

    public List<PurchaseOrderDto> getPurchaseOrdersByProject(String projectId) {
        // Verify project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return purchaseOrderRepository.findByProjectId(projectId).stream()
                                      .map(this::mapPurchaseOrderToDto)
                                      .collect(Collectors.toList());
    }

    public PurchaseOrderDto getPurchaseOrderByOrderNumber(String orderNumber) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByOrderNumber(orderNumber)
                                                             .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with order number: " + orderNumber));
        return mapPurchaseOrderToDto(purchaseOrder);
    }

    public List<PurchaseOrderDto> getPurchaseOrdersByStatus(PurchaseOrder.OrderStatus status) {
        return purchaseOrderRepository.findByStatus(status).stream()
                                      .map(this::mapPurchaseOrderToDto)
                                      .collect(Collectors.toList());
    }

    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderDto purchaseOrderDto) {
        // Verify project exists
        if (!projectRepository.existsById(purchaseOrderDto.getProjectId())) {
            throw new ResourceNotFoundException("Project not found with id: " + purchaseOrderDto.getProjectId());
        }

        // Check if order number already exists
        if (purchaseOrderRepository.findByOrderNumber(purchaseOrderDto.getOrderNumber()).isPresent()) {
            throw new IllegalArgumentException("Purchase order number already exists: " + purchaseOrderDto.getOrderNumber());
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setOrderNumber(purchaseOrderDto.getOrderNumber());
        purchaseOrder.setSupplier(purchaseOrderDto.getSupplier());
        purchaseOrder.setTransporter(purchaseOrderDto.getTransporter());
        purchaseOrder.setOrderedQuantity(purchaseOrderDto.getOrderedQuantity());
        purchaseOrder.setDeliveredQuantity(0.0); // Initialize with zero
        purchaseOrder.setProjectId(purchaseOrderDto.getProjectId());
        purchaseOrder.setStatus(PurchaseOrder.OrderStatus.ACTIVE);

        // Calculate remaining quantity and delivery percentage
        purchaseOrder.recalculateDeliveryMetrics();

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        return mapPurchaseOrderToDto(savedPurchaseOrder);
    }

    public PurchaseOrderDto updatePurchaseOrder(String id, PurchaseOrderDto purchaseOrderDto) {
        PurchaseOrder existingPurchaseOrder = purchaseOrderRepository.findById(id)
                                                                     .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));

        // Verify project exists if it's changed
        if (!existingPurchaseOrder.getProjectId().equals(purchaseOrderDto.getProjectId()) &&
                !projectRepository.existsById(purchaseOrderDto.getProjectId())) {
            throw new ResourceNotFoundException("Project not found with id: " + purchaseOrderDto.getProjectId());
        }

        // Check if order number is being changed and if it already exists
        if (!existingPurchaseOrder.getOrderNumber().equals(purchaseOrderDto.getOrderNumber()) &&
                purchaseOrderRepository.findByOrderNumber(purchaseOrderDto.getOrderNumber()).isPresent()) {
            throw new IllegalArgumentException("Purchase order number already exists: " + purchaseOrderDto.getOrderNumber());
        }

        existingPurchaseOrder.setOrderNumber(purchaseOrderDto.getOrderNumber());
        existingPurchaseOrder.setSupplier(purchaseOrderDto.getSupplier());
        existingPurchaseOrder.setTransporter(purchaseOrderDto.getTransporter());
        existingPurchaseOrder.setOrderedQuantity(purchaseOrderDto.getOrderedQuantity());
        existingPurchaseOrder.setProjectId(purchaseOrderDto.getProjectId());
        existingPurchaseOrder.setStatus(purchaseOrderDto.getStatus());

        // Recalculate metrics based on updated values
        existingPurchaseOrder.recalculateDeliveryMetrics();

        PurchaseOrder updatedPurchaseOrder = purchaseOrderRepository.save(existingPurchaseOrder);
        return mapPurchaseOrderToDto(updatedPurchaseOrder);
    }

    public boolean deletePurchaseOrder(String id) {
        if (!purchaseOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Purchase order not found with id: " + id);
        }
        purchaseOrderRepository.deleteById(id);
        return true;
    }

    // Internal method to update delivered quantity
    public void updateDeliveredQuantity(String purchaseOrderId, double additionalQuantity) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                                                             .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + purchaseOrderId));

        purchaseOrder.setDeliveredQuantity(purchaseOrder.getDeliveredQuantity() + additionalQuantity);
        purchaseOrder.recalculateDeliveryMetrics();

        purchaseOrderRepository.save(purchaseOrder);
    }

    private PurchaseOrderDto mapPurchaseOrderToDto(PurchaseOrder purchaseOrder) {
        return PurchaseOrderDto.builder()
                               .id(purchaseOrder.getId())
                               .orderNumber(purchaseOrder.getOrderNumber())
                               .supplier(purchaseOrder.getSupplier())
                               .transporter(purchaseOrder.getTransporter())
                               .orderedQuantity(purchaseOrder.getOrderedQuantity())
                               .deliveredQuantity(purchaseOrder.getDeliveredQuantity())
                               .remainingQuantity(purchaseOrder.getRemainingQuantity())
                               .deliveryPercentage(purchaseOrder.getDeliveryPercentage())
                               .projectId(purchaseOrder.getProjectId())
                               .status(purchaseOrder.getStatus())
                               .createdAt(purchaseOrder.getCreatedAt())
                               .updatedAt(purchaseOrder.getUpdatedAt())
                               .build();
    }
}