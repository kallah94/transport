package com.gayale.transport.repository;

import com.gayale.transport.model.PaymentStatement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentStatementRepository extends MongoRepository<PaymentStatement, String> {

    List<PaymentStatement> findByTransporterId(String transporterId);

    List<PaymentStatement> findByPurchaseOrderId(String purchaseOrderId);

    List<PaymentStatement> findByStatus(PaymentStatement.PaymentStatus status);

    List<PaymentStatement> findByTransporterIdAndStatus(String transporterId, PaymentStatement.PaymentStatus status);
}
