package com.gayale.transport.repository;

import com.gayale.transport.model.DriverPaymentStatement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DriverPaymentStatementRepository extends MongoRepository<DriverPaymentStatement, String> {
    List<DriverPaymentStatement> findByTransporterId(String transporterId);
    List<DriverPaymentStatement> findByStatus(DriverPaymentStatement.PaymentStatus status);
    List<DriverPaymentStatement> findByPurchaseOrderId(String purchaseOrderId);
}
