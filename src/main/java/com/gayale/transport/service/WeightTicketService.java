package com.gayale.transport.service;

import com.gayale.transport.dto.WeightTicketDto;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.WeightTicket;
import com.gayale.transport.repository.PurchaseOrderRepository;
import com.gayale.transport.repository.ProjectRepository;
import com.gayale.transport.repository.WeightTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class WeightTicketService {

    private final WeightTicketRepository weightTicketRepository;
    private final ProjectRepository projectRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProjectService projectService;
    private final PurchaseOrderService purchaseOrderService;
    private final QRCodeService qrCodeService;

    @Autowired
    public WeightTicketService(WeightTicketRepository weightTicketRepository,
                               ProjectRepository projectRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               ProjectService projectService,
                               PurchaseOrderService purchaseOrderService,
                               QRCodeService qrCodeService) {
        this.weightTicketRepository = weightTicketRepository;
        this.projectRepository = projectRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.projectService = projectService;
        this.purchaseOrderService = purchaseOrderService;
        this.qrCodeService = qrCodeService;
    }

    public List<WeightTicketDto> getAllWeightTickets() {
        return weightTicketRepository.findAll().stream()
                                     .map(this::mapWeightTicketToDto)
                                     .collect(Collectors.toList());
    }

    public Page<WeightTicketDto> getAllWeightTicketsPaginated(Pageable pageable) {
        return weightTicketRepository.findAll(pageable)
                                     .map(this::mapWeightTicketToDto);
    }

    public WeightTicketDto getWeightTicketById(String id) {
        WeightTicket weightTicket = weightTicketRepository.findById(id)
                                                          .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with id: " + id));
        return mapWeightTicketToDto(weightTicket);
    }

    public WeightTicketDto getWeightTicketByTicketNumber(String ticketNumber) {
        WeightTicket weightTicket = weightTicketRepository.findByTicketNumber(ticketNumber)
                                                          .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with ticket number: " + ticketNumber));
        return mapWeightTicketToDto(weightTicket);
    }

    public List<WeightTicketDto> getWeightTicketsByProject(String projectId) {
        // Verify project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return weightTicketRepository.findByProjectId(projectId).stream()
                                     .map(this::mapWeightTicketToDto)
                                     .collect(Collectors.toList());
    }

    public List<WeightTicketDto> getWeightTicketsByPurchaseOrder(String purchaseOrderId) {
        // Verify purchase order exists
        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw new ResourceNotFoundException("Purchase order not found with id: " + purchaseOrderId);
        }

        return weightTicketRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                                     .map(this::mapWeightTicketToDto)
                                     .collect(Collectors.toList());
    }

    public List<WeightTicketDto> getWeightTicketsByDate(LocalDate date) {
        return weightTicketRepository.findByDate(date).stream()
                                     .map(this::mapWeightTicketToDto)
                                     .collect(Collectors.toList());
    }

    public List<WeightTicketDto> getWeightTicketsByDateRange(LocalDate startDate, LocalDate endDate) {
        return weightTicketRepository.findByDateBetween(startDate, endDate).stream()
                                     .map(this::mapWeightTicketToDto)
                                     .collect(Collectors.toList());
    }

    @Transactional
    public WeightTicketDto createWeightTicket(WeightTicketDto weightTicketDto) {
        // Verify project exists
        if (!projectRepository.existsById(weightTicketDto.getProjectId())) {
            throw new ResourceNotFoundException("Project not found with id: " + weightTicketDto.getProjectId());
        }

        // Verify purchase order exists
        if (!purchaseOrderRepository.existsById(weightTicketDto.getPurchaseOrderId())) {
            throw new ResourceNotFoundException("Purchase order not found with id: " + weightTicketDto.getPurchaseOrderId());
        }

        WeightTicket weightTicket = new WeightTicket();

        // Generate ticket number if not provided
        if (weightTicketDto.getTicketNumber() == null || weightTicketDto.getTicketNumber().isEmpty()) {
            weightTicket.setTicketNumber(generateTicketNumber());
        } else {
            // Verify ticket number doesn't already exist
            if (weightTicketRepository.findByTicketNumber(weightTicketDto.getTicketNumber()).isPresent()) {
                throw new IllegalArgumentException("Ticket number already exists: " + weightTicketDto.getTicketNumber());
            }
            weightTicket.setTicketNumber(weightTicketDto.getTicketNumber());
        }

        weightTicket.setDate(weightTicketDto.getDate() != null ? weightTicketDto.getDate() : LocalDate.now());
        weightTicket.setEmptyWeight(weightTicketDto.getEmptyWeight());
        weightTicket.setLoadedWeight(weightTicketDto.getLoadedWeight());
        weightTicket.setVehicle(weightTicketDto.getVehicle());
        weightTicket.setDriver(weightTicketDto.getDriver());
        weightTicket.setProduct(weightTicketDto.getProduct());
        weightTicket.setClient(weightTicketDto.getClient());
        weightTicket.setSupplier(weightTicketDto.getSupplier());
        weightTicket.setOrigin(weightTicketDto.getOrigin());
        weightTicket.setDestination(weightTicketDto.getDestination());
        weightTicket.setTransporter(weightTicketDto.getTransporter());
        weightTicket.setPurchaseOrderNumber(weightTicketDto.getPurchaseOrderNumber());
        weightTicket.setProjectId(weightTicketDto.getProjectId());
        weightTicket.setPurchaseOrderId(weightTicketDto.getPurchaseOrderId());
        weightTicket.setOperatorName(weightTicketDto.getOperatorName());
        weightTicket.setStatus(WeightTicket.TicketStatus.PENDING);

        // Calculate net weight and variance
        weightTicket.calculateWeights();

        // Save ticket first to get an ID
        WeightTicket savedTicket = weightTicketRepository.save(weightTicket);

        // Generate QR code
        String qrCode = qrCodeService.generateQRCodeBase64(savedTicket.getId());
        savedTicket.setQrCode(qrCode);

        // Save again with QR code
        savedTicket = weightTicketRepository.save(savedTicket);

        // Update the associated purchase order and project with the new tonnage
        updateDeliveryQuantities(savedTicket.getPurchaseOrderId(), savedTicket.getProjectId(), savedTicket.getNetWeight());

        return mapWeightTicketToDto(savedTicket);
    }

    @Transactional
    public WeightTicketDto updateWeightTicket(String id, WeightTicketDto weightTicketDto) {
        WeightTicket existingTicket = weightTicketRepository.findById(id)
                                                            .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with id: " + id));

        // Calculate the old net weight to adjust project and purchase order metrics
        double oldNetWeight = existingTicket.getNetWeight();

        // Update fields
        existingTicket.setDate(weightTicketDto.getDate());
        existingTicket.setEmptyWeight(weightTicketDto.getEmptyWeight());
        existingTicket.setLoadedWeight(weightTicketDto.getLoadedWeight());
        existingTicket.setVehicle(weightTicketDto.getVehicle());
        existingTicket.setDriver(weightTicketDto.getDriver());
        existingTicket.setProduct(weightTicketDto.getProduct());
        existingTicket.setClient(weightTicketDto.getClient());
        existingTicket.setSupplier(weightTicketDto.getSupplier());
        existingTicket.setOrigin(weightTicketDto.getOrigin());
        existingTicket.setDestination(weightTicketDto.getDestination());
        existingTicket.setTransporter(weightTicketDto.getTransporter());
        existingTicket.setOperatorName(weightTicketDto.getOperatorName());
        existingTicket.setStatus(weightTicketDto.getStatus());

        // Calculate net weight and variance
        existingTicket.calculateWeights();

        // Handle changes in Project or Purchase Order
        boolean projectChanged = !existingTicket.getProjectId().equals(weightTicketDto.getProjectId());
        boolean purchaseOrderChanged = !existingTicket.getPurchaseOrderId().equals(weightTicketDto.getPurchaseOrderId());

        if (projectChanged || purchaseOrderChanged) {
            // Remove tonnage from old associations
            updateDeliveryQuantities(existingTicket.getPurchaseOrderId(), existingTicket.getProjectId(), -oldNetWeight);

            // Update associations
            existingTicket.setProjectId(weightTicketDto.getProjectId());
            existingTicket.setPurchaseOrderId(weightTicketDto.getPurchaseOrderId());
            existingTicket.setPurchaseOrderNumber(weightTicketDto.getPurchaseOrderNumber());

            // Add tonnage to new associations
            updateDeliveryQuantities(existingTicket.getPurchaseOrderId(), existingTicket.getProjectId(), existingTicket.getNetWeight());
        } else {
            // Update tonnage difference
            double weightDifference = existingTicket.getNetWeight() - oldNetWeight;
            if (weightDifference != 0) {
                updateDeliveryQuantities(existingTicket.getPurchaseOrderId(), existingTicket.getProjectId(), weightDifference);
            }
        }

        WeightTicket updatedTicket = weightTicketRepository.save(existingTicket);
        return mapWeightTicketToDto(updatedTicket);
    }

    @Transactional
    public boolean deleteWeightTicket(String id) {
        WeightTicket ticket = weightTicketRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with id: " + id));

        // Remove tonnage from project and purchase order
        updateDeliveryQuantities(ticket.getPurchaseOrderId(), ticket.getProjectId(), -ticket.getNetWeight());

        weightTicketRepository.deleteById(id);
        return true;
    }

    public String generateQRCode(String ticketId) {
        WeightTicket ticket = weightTicketRepository.findById(ticketId)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with id: " + ticketId));

        String qrCode = qrCodeService.generateQRCodeBase64(ticketId);
        ticket.setQrCode(qrCode);
        weightTicketRepository.save(ticket);

        return qrCode;
    }

    @Transactional
    public WeightTicketDto validateWeightTicket(String id) {
        WeightTicket ticket = weightTicketRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with id: " + id));

        ticket.setStatus(WeightTicket.TicketStatus.VALIDATED);
        WeightTicket updatedTicket = weightTicketRepository.save(ticket);

        return mapWeightTicketToDto(updatedTicket);
    }

    @Transactional
    public WeightTicketDto cancelWeightTicket(String id) {
        WeightTicket ticket = weightTicketRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Weight ticket not found with id: " + id));

        // Only remove tonnage if the ticket was not already cancelled
        if (ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED) {
            updateDeliveryQuantities(ticket.getPurchaseOrderId(), ticket.getProjectId(), -ticket.getNetWeight());
        }

        ticket.setStatus(WeightTicket.TicketStatus.CANCELLED);
        WeightTicket updatedTicket = weightTicketRepository.save(ticket);

        return mapWeightTicketToDto(updatedTicket);
    }

    private String generateTicketNumber() {
        LocalDate now = LocalDate.now();
        String datePrefix = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        int randomSuffix = 10000 + random.nextInt(90000); // 5-digit random number

        String ticketNumber = "TK-" + datePrefix + "-" + randomSuffix;

        // Ensure uniqueness
        while (weightTicketRepository.findByTicketNumber(ticketNumber).isPresent()) {
            randomSuffix = 10000 + random.nextInt(90000);
            ticketNumber = "TK-" + datePrefix + "-" + randomSuffix;
        }

        return ticketNumber;
    }

    private void updateDeliveryQuantities(String purchaseOrderId, String projectId, double weightDifference) {
        // Update purchase order delivered quantity
        purchaseOrderService.updateDeliveredQuantity(purchaseOrderId, weightDifference/1000);

        // Update project total delivered tonnage
        projectService.updateTotalDeliveredTonnage(projectId, weightDifference /1000);
    }

    private WeightTicketDto mapWeightTicketToDto(WeightTicket weightTicket) {
        return WeightTicketDto.builder()
                              .id(weightTicket.getId())
                              .ticketNumber(weightTicket.getTicketNumber())
                              .date(weightTicket.getDate())
                              .emptyWeight(weightTicket.getEmptyWeight())
                              .loadedWeight(weightTicket.getLoadedWeight())
                              .netWeight(weightTicket.getNetWeight())
                              .variance(weightTicket.getVariance())
                              .vehicle(weightTicket.getVehicle())
                              .driver(weightTicket.getDriver())
                              .product(weightTicket.getProduct())
                              .client(weightTicket.getClient())
                              .supplier(weightTicket.getSupplier())
                              .origin(weightTicket.getOrigin())
                              .destination(weightTicket.getDestination())
                              .transporter(weightTicket.getTransporter())
                              .purchaseOrderNumber(weightTicket.getPurchaseOrderNumber())
                              .projectId(weightTicket.getProjectId())
                              .purchaseOrderId(weightTicket.getPurchaseOrderId())
                              .operatorName(weightTicket.getOperatorName())
                              .qrCode(weightTicket.getQrCode())
                              .status(weightTicket.getStatus())
                              .createdAt(weightTicket.getCreatedAt())
                              .updatedAt(weightTicket.getUpdatedAt())
                              .build();
    }
}