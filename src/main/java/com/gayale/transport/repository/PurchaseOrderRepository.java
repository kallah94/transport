package com.gayale.transport.repository;

import com.gayale.transport.model.PurchaseOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends MongoRepository<PurchaseOrder, String> {
    List<PurchaseOrder> findByProjectId(String projectId);
    List<PurchaseOrder> findByStatus(PurchaseOrder.OrderStatus status);
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
    List<PurchaseOrder> findBySupplier(String supplier);
    List<PurchaseOrder> findByTransporter(String transporter);
}
