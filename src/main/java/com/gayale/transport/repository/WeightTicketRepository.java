package com.gayale.transport.repository;

import com.gayale.transport.model.WeightTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightTicketRepository extends MongoRepository<WeightTicket, String> {
    Optional<WeightTicket> findByTicketNumber(String ticketNumber);
    List<WeightTicket> findByProjectId(String projectId);
    List<WeightTicket> findByPurchaseOrderId(String purchaseOrderId);
    List<WeightTicket> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<WeightTicket> findByDate(LocalDate date);
    List<WeightTicket> findByVehicle(String vehicle);
    List<WeightTicket> findByDriver(String driver);
    List<WeightTicket> findByTransporter(String transporter);
    List<WeightTicket> findBySupplier(String supplier);
    Page<WeightTicket> findAll(Pageable pageable);
    Optional<WeightTicket> findByChecksum(String checksum);
    boolean existsByChecksum(String checksum);
    List<WeightTicket> findByVehicleAndDateAndStatus(String vehicle, LocalDate date, WeightTicket.TicketStatus status);
}

