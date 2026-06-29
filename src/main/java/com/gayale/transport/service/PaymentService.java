package com.gayale.transport.service;

import com.gayale.transport.dto.payment.DriverPayoutLineDto;
import com.gayale.transport.dto.payment.DriverPayoutResponse;
import com.gayale.transport.dto.payment.PaymentGenerationRequest;
import com.gayale.transport.dto.payment.PaymentLineDto;
import com.gayale.transport.dto.payment.PaymentStatementResponse;
import com.gayale.transport.exception.MissingRateException;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.DriverPaymentStatement;
import com.gayale.transport.model.DriverPayoutLine;
import com.gayale.transport.model.DriverRate;
import com.gayale.transport.model.FuelConfig;
import com.gayale.transport.model.PaymentLine;
import com.gayale.transport.model.PaymentStatement;
import com.gayale.transport.model.Project;
import com.gayale.transport.model.PurchaseOrder;
import com.gayale.transport.model.TransporterEnterprise;
import com.gayale.transport.model.TransporterRate;
import com.gayale.transport.model.Truck;
import com.gayale.transport.model.WeightTicket;
import com.gayale.transport.repository.DriverPaymentStatementRepository;
import com.gayale.transport.repository.DriverRateRepository;
import com.gayale.transport.repository.FuelConfigRepository;
import com.gayale.transport.repository.PaymentStatementRepository;
import com.gayale.transport.repository.ProjectRepository;
import com.gayale.transport.repository.PurchaseOrderRepository;
import com.gayale.transport.repository.TransporterEnterpriseRepository;
import com.gayale.transport.repository.TransporterRateRepository;
import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.WeightTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final WeightTicketRepository weightTicketRepository;
    private final TransporterEnterpriseRepository transporterRepository;
    private final TruckRepository truckRepository;
    private final ProjectRepository projectRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TransporterRateRepository transporterRateRepository;
    private final DriverRateRepository driverRateRepository;
    private final DriverPaymentStatementRepository driverPaymentStatementRepository;
    private final FuelConfigRepository fuelConfigRepository;
    private final PaymentStatementRepository paymentStatementRepository;

    @Autowired
    public PaymentService(WeightTicketRepository weightTicketRepository,
                          TransporterEnterpriseRepository transporterRepository,
                          TruckRepository truckRepository,
                          ProjectRepository projectRepository,
                          PurchaseOrderRepository purchaseOrderRepository,
                          TransporterRateRepository transporterRateRepository,
                          DriverRateRepository driverRateRepository,
                          DriverPaymentStatementRepository driverPaymentStatementRepository,
                          FuelConfigRepository fuelConfigRepository,
                          PaymentStatementRepository paymentStatementRepository) {
        this.weightTicketRepository = weightTicketRepository;
        this.transporterRepository = transporterRepository;
        this.truckRepository = truckRepository;
        this.projectRepository = projectRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.transporterRateRepository = transporterRateRepository;
        this.driverRateRepository = driverRateRepository;
        this.driverPaymentStatementRepository = driverPaymentStatementRepository;
        this.fuelConfigRepository = fuelConfigRepository;
        this.paymentStatementRepository = paymentStatementRepository;
    }

    public PaymentStatementResponse preview(PaymentGenerationRequest request) {
        return toResponse(compute(request));
    }

    public PaymentStatementResponse generate(PaymentGenerationRequest request) {
        return toResponse(paymentStatementRepository.save(compute(request)));
    }

    public List<PaymentStatementResponse> list(String transporterId, PaymentStatement.PaymentStatus status) {
        List<PaymentStatement> result;
        if (transporterId != null && !transporterId.isBlank() && status != null) {
            result = paymentStatementRepository.findByTransporterIdAndStatus(transporterId, status);
        } else if (transporterId != null && !transporterId.isBlank()) {
            result = paymentStatementRepository.findByTransporterId(transporterId);
        } else if (status != null) {
            result = paymentStatementRepository.findByStatus(status);
        } else {
            result = paymentStatementRepository.findAll();
        }
        return result.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PaymentStatementResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    public PaymentStatementResponse validate(String id) {
        PaymentStatement s = findOrThrow(id);
        s.setStatus(PaymentStatement.PaymentStatus.VALIDATED);
        return toResponse(paymentStatementRepository.save(s));
    }

    public PaymentStatementResponse markPaid(String id) {
        PaymentStatement s = findOrThrow(id);
        s.setStatus(PaymentStatement.PaymentStatus.PAID);
        return toResponse(paymentStatementRepository.save(s));
    }

    // ----------------- Paiement chauffeurs par camion (process separe) -----------------

    public DriverPayoutResponse getDriverPayouts(String transporterId, String purchaseOrderId) {
        TransporterEnterprise transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporteur introuvable avec l'ID : " + transporterId));
        String transporterName = transporter.getName();

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande introuvable avec l'ID : " + purchaseOrderId));

        List<WeightTicket> tickets = weightTicketRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(t -> t.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                .filter(t -> transporterName != null && transporterName.equalsIgnoreCase(t.getTransporter()))
                .collect(Collectors.toList());

        FuelConfig fuelConfig = fuelConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc().orElse(null);
        double fuelPrice = fuelConfig != null ? fuelConfig.getFuelPricePerLitre() : 0.0;
        double roundTripFactor = (fuelConfig != null && fuelConfig.getRoundTripFactor() > 0)
                ? fuelConfig.getRoundTripFactor() : 2.0;

        // Regroupement par camion (vehicule)
        Map<String, DriverPayoutLineDto> byVehicle = new LinkedHashMap<>();
        Map<String, Double> distanceCache = new HashMap<>();
        Map<String, Double> consoCache = new HashMap<>();

        for (WeightTicket t : tickets) {
            String vehicle = t.getVehicle() != null ? t.getVehicle() : "(inconnu)";
            double tonnes = t.getNetWeight() / 1000.0;

            double litres = 0, fuelAmount = 0;
            double distanceKm = t.getProjectId() != null
                    ? distanceCache.computeIfAbsent(t.getProjectId(), pid -> projectRepository.findById(pid).map(Project::getDistanceKm).orElse(0.0))
                    : 0.0;
            double conso = consoCache.computeIfAbsent(vehicle, v -> truckRepository.findByVehicle(v).map(Truck::getFuelConsumptionLPerKm).orElse(0.0));
            if (distanceKm > 0 && conso > 0) {
                litres = distanceKm * roundTripFactor * conso;
                fuelAmount = litres * fuelPrice;
            }

            double driverRate = resolveDriverRate(transporterId, t.getDate());
            double gross = tonnes * driverRate;

            DriverPayoutLineDto line = byVehicle.computeIfAbsent(vehicle, v ->
                    DriverPayoutLineDto.builder().vehicle(v).driver(t.getDriver())
                            .tripCount(0).totalTonnes(0).grossAmount(0)
                            .fuelLitres(0).fuelAmount(0).netAmount(0).build());
            if (line.getDriver() == null) line.setDriver(t.getDriver());
            line.setTripCount(line.getTripCount() + 1);
            line.setTotalTonnes(line.getTotalTonnes() + tonnes);
            line.setGrossAmount(line.getGrossAmount() + gross);
            line.setFuelLitres(line.getFuelLitres() + litres);
            line.setFuelAmount(line.getFuelAmount() + fuelAmount);
        }

        List<DriverPayoutLineDto> lines = new ArrayList<>();
        double totalTonnes = 0, totalLitres = 0, totalFuel = 0, totalGross = 0, totalNet = 0;
        for (DriverPayoutLineDto line : byVehicle.values()) {
            double net = Math.max(0, line.getGrossAmount() - line.getFuelAmount());
            double pricePerTonne = line.getTotalTonnes() > 0 ? line.getGrossAmount() / line.getTotalTonnes() : 0;
            line.setTotalTonnes(round(line.getTotalTonnes()));
            line.setPricePerTonne(round(pricePerTonne));
            line.setGrossAmount(round(line.getGrossAmount()));
            line.setFuelLitres(round(line.getFuelLitres()));
            line.setFuelAmount(round(line.getFuelAmount()));
            line.setNetAmount(round(net));
            lines.add(line);
            totalTonnes += line.getTotalTonnes();
            totalLitres += line.getFuelLitres();
            totalFuel += line.getFuelAmount();
            totalGross += line.getGrossAmount();
            totalNet += line.getNetAmount();
        }

        return DriverPayoutResponse.builder()
                .transporterId(transporterId)
                .transporterName(transporterName)
                .purchaseOrderId(purchaseOrderId)
                .purchaseOrderNumber(purchaseOrder.getOrderNumber())
                .lines(lines)
                .totalTonnes(round(totalTonnes))
                .totalFuelLitres(round(totalLitres))
                .totalFuelAmount(round(totalFuel))
                .totalGross(round(totalGross))
                .totalNet(round(totalNet))
                .build();
    }

    private double resolveDriverRate(String transporterId, LocalDate date) {
        return driverRateRepository.findByActiveTrue().stream()
                .filter(r -> covers(r.getEffectiveFrom(), r.getEffectiveTo(), date))
                .filter(r -> isBlank(r.getTransporterId()) || r.getTransporterId().equals(transporterId))
                .max(Comparator.comparingInt(r -> !isBlank(r.getTransporterId()) ? 1 : 0))
                .map(DriverRate::getPricePerTonne)
                .orElse(0.0);
    }

    // ----------------- Facture transporteur (transport a la tonne + dotation carburant) -----------------

    private PaymentStatement compute(PaymentGenerationRequest request) {
        TransporterEnterprise transporter = transporterRepository.findById(request.getTransporterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transporteur introuvable avec l'ID : " + request.getTransporterId()));
        String transporterName = transporter.getName();

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bon de commande introuvable avec l'ID : " + request.getPurchaseOrderId()));
        String orderNumber = purchaseOrder.getOrderNumber();

        Project invoiceProject = (purchaseOrder.getProjectId() != null)
                ? projectRepository.findById(purchaseOrder.getProjectId()).orElse(null) : null;

        List<WeightTicket> tickets = weightTicketRepository
                .findByPurchaseOrderId(request.getPurchaseOrderId()).stream()
                .filter(t -> t.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                .filter(t -> transporterName != null && transporterName.equalsIgnoreCase(t.getTransporter()))
                .collect(Collectors.toList());

        FuelConfig fuelConfig = fuelConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc().orElse(null);
        double fuelPrice = fuelConfig != null ? fuelConfig.getFuelPricePerLitre() : 0.0;
        double roundTripFactor = (fuelConfig != null && fuelConfig.getRoundTripFactor() > 0)
                ? fuelConfig.getRoundTripFactor() : 2.0;

        List<PaymentLine> lines = new ArrayList<>();
        double totalTransport = 0, totalFuelLitres = 0, totalFuel = 0, totalTonnage = 0;
        Map<String, Double> distanceCache = new HashMap<>();
        Map<String, Double> consoCache = new HashMap<>();

        for (WeightTicket t : tickets) {
            double tonnes = t.getNetWeight() / 1000.0;

            double transportRate = resolveTransporterRate(request.getTransporterId(), t.getProjectId(), t.getProduct(), t.getDate());
            double transportAmount = round(tonnes * transportRate);

            double litres = 0, fuelAmount = 0;
            String note = null;
            double distanceKm = t.getProjectId() != null
                    ? distanceCache.computeIfAbsent(t.getProjectId(), pid -> projectRepository.findById(pid).map(Project::getDistanceKm).orElse(0.0))
                    : 0.0;
            double conso = t.getVehicle() != null
                    ? consoCache.computeIfAbsent(t.getVehicle(), v -> truckRepository.findByVehicle(v).map(Truck::getFuelConsumptionLPerKm).orElse(0.0))
                    : 0.0;
            if (distanceKm > 0 && conso > 0) {
                litres = distanceKm * roundTripFactor * conso;
                fuelAmount = round(litres * fuelPrice);
            } else {
                note = "Dotation carburant indisponible (distance projet ou consommation camion manquante)";
            }

            lines.add(PaymentLine.builder()
                    .ticketId(t.getId())
                    .ticketNumber(t.getTicketNumber())
                    .date(t.getDate())
                    .vehicle(t.getVehicle())
                    .driver(t.getDriver())
                    .tonnes(round(tonnes))
                    .transportAmount(transportAmount)
                    .distanceKm(distanceKm)
                    .fuelLitres(round(litres))
                    .fuelAmount(fuelAmount)
                    .note(note)
                    .build());

            totalTransport += transportAmount;
            totalFuelLitres += litres;
            totalFuel += fuelAmount;
            totalTonnage += tonnes;
        }

        double invoiceTotal = round(totalTransport + totalFuel);
        double unitPrice = totalTonnage > 0 ? round(totalTransport / totalTonnage) : 0;

        return PaymentStatement.builder()
                .transporterId(request.getTransporterId())
                .transporterName(transporterName)
                .purchaseOrderId(request.getPurchaseOrderId())
                .purchaseOrderNumber(orderNumber)
                .supplier(purchaseOrder.getSupplier())
                .projectName(invoiceProject != null ? invoiceProject.getName() : null)
                .client(invoiceProject != null ? invoiceProject.getClient() : null)
                .provenance(invoiceProject != null ? invoiceProject.getProvenance() : null)
                .destination(invoiceProject != null ? invoiceProject.getDestination() : null)
                .product(invoiceProject != null ? invoiceProject.getProduct() : null)
                .transporterAddress(transporter.getAddress())
                .transporterRegistrationNumber(transporter.getRegistrationNumber())
                .transporterPhone(transporter.getPhone())
                .transporterEmail(transporter.getEmail())
                .representativeName(transporter.getRepresentative() != null ? transporter.getRepresentative().getFullName() : null)
                .representativeEmail(transporter.getRepresentative() != null ? transporter.getRepresentative().getEmail() : null)
                .tonnageTotal(round(totalTonnage))
                .tripCount(tickets.size())
                .unitPricePerTonne(unitPrice)
                .transportAmount(round(totalTransport))
                .fuelLitres(round(totalFuelLitres))
                .fuelAmount(round(totalFuel))
                .invoiceTotal(invoiceTotal)
                .status(PaymentStatement.PaymentStatus.DRAFT)
                .lines(lines)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private double resolveTransporterRate(String transporterId, String projectId, String product, LocalDate date) {
        List<TransporterRate> candidates = transporterRateRepository
                .findByTransporterIdAndActiveTrue(transporterId).stream()
                .filter(r -> covers(r.getEffectiveFrom(), r.getEffectiveTo(), date))
                .filter(r -> (isBlank(r.getProjectId()) || r.getProjectId().equals(projectId))
                          && (isBlank(r.getProduct()) || r.getProduct().equalsIgnoreCase(product)))
                .collect(Collectors.toList());

        return candidates.stream()
                .max(Comparator.comparingInt(r ->
                        (!isBlank(r.getProjectId()) ? 2 : 0) + (!isBlank(r.getProduct()) ? 1 : 0)))
                .map(TransporterRate::getPricePerTonne)
                .orElseThrow(() -> new MissingRateException(
                        "Aucun tarif transporteur applicable pour le transporteur " + transporterId
                        + " a la date " + date));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean covers(LocalDate from, LocalDate to, LocalDate date) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private PaymentStatement findOrThrow(String id) {
        return paymentStatementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Releve de paiement introuvable avec l'ID : " + id));
    }

    // ----------------- Persistance paiement chauffeur -----------------

    public DriverPayoutResponse generateDriverPayment(String transporterId, String purchaseOrderId) {
        DriverPayoutResponse r = getDriverPayouts(transporterId, purchaseOrderId);
        DriverPaymentStatement st = DriverPaymentStatement.builder()
                .transporterId(r.getTransporterId())
                .transporterName(r.getTransporterName())
                .purchaseOrderId(r.getPurchaseOrderId())
                .purchaseOrderNumber(r.getPurchaseOrderNumber())
                .lines(r.getLines() == null ? new ArrayList<>()
                        : r.getLines().stream().map(this::toLineModel).collect(Collectors.toList()))
                .totalTonnes(r.getTotalTonnes())
                .totalFuelLitres(r.getTotalFuelLitres())
                .totalFuelAmount(r.getTotalFuelAmount())
                .totalGross(r.getTotalGross())
                .totalNet(r.getTotalNet())
                .status(DriverPaymentStatement.PaymentStatus.DRAFT)
                .generatedAt(LocalDateTime.now())
                .build();
        return toDriverResponse(driverPaymentStatementRepository.save(st));
    }

    public List<DriverPayoutResponse> listDriverPayments(String transporterId, DriverPaymentStatement.PaymentStatus status) {
        List<DriverPaymentStatement> result;
        if (status != null) {
            result = driverPaymentStatementRepository.findByStatus(status);
        } else if (transporterId != null && !transporterId.isBlank()) {
            result = driverPaymentStatementRepository.findByTransporterId(transporterId);
        } else {
            result = driverPaymentStatementRepository.findAll();
        }
        return result.stream().map(this::toDriverResponse).collect(Collectors.toList());
    }

    public DriverPayoutResponse getDriverPaymentById(String id) {
        return toDriverResponse(findDriverOrThrow(id));
    }

    public DriverPayoutResponse validateDriverPayment(String id) {
        DriverPaymentStatement s = findDriverOrThrow(id);
        s.setStatus(DriverPaymentStatement.PaymentStatus.VALIDATED);
        return toDriverResponse(driverPaymentStatementRepository.save(s));
    }

    public DriverPayoutResponse payDriverPayment(String id) {
        DriverPaymentStatement s = findDriverOrThrow(id);
        s.setStatus(DriverPaymentStatement.PaymentStatus.PAID);
        return toDriverResponse(driverPaymentStatementRepository.save(s));
    }

    private DriverPaymentStatement findDriverOrThrow(String id) {
        return driverPaymentStatementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement chauffeur introuvable avec l'ID : " + id));
    }

    private DriverPayoutLine toLineModel(DriverPayoutLineDto d) {
        return DriverPayoutLine.builder()
                .vehicle(d.getVehicle()).driver(d.getDriver()).tripCount(d.getTripCount())
                .totalTonnes(d.getTotalTonnes()).pricePerTonne(d.getPricePerTonne())
                .grossAmount(d.getGrossAmount()).fuelLitres(d.getFuelLitres())
                .fuelAmount(d.getFuelAmount()).netAmount(d.getNetAmount()).build();
    }

    private DriverPayoutResponse toDriverResponse(DriverPaymentStatement s) {
        List<DriverPayoutLineDto> lines = s.getLines() == null ? List.of() : s.getLines().stream()
                .map(l -> DriverPayoutLineDto.builder()
                        .vehicle(l.getVehicle()).driver(l.getDriver()).tripCount(l.getTripCount())
                        .totalTonnes(l.getTotalTonnes()).pricePerTonne(l.getPricePerTonne())
                        .grossAmount(l.getGrossAmount()).fuelLitres(l.getFuelLitres())
                        .fuelAmount(l.getFuelAmount()).netAmount(l.getNetAmount()).build())
                .collect(Collectors.toList());
        return DriverPayoutResponse.builder()
                .id(s.getId())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .transporterId(s.getTransporterId())
                .transporterName(s.getTransporterName())
                .purchaseOrderId(s.getPurchaseOrderId())
                .purchaseOrderNumber(s.getPurchaseOrderNumber())
                .lines(lines)
                .totalTonnes(s.getTotalTonnes())
                .totalFuelLitres(s.getTotalFuelLitres())
                .totalFuelAmount(s.getTotalFuelAmount())
                .totalGross(s.getTotalGross())
                .totalNet(s.getTotalNet())
                .build();
    }

    // ----------------- Mapping -----------------

    private PaymentStatementResponse toResponse(PaymentStatement s) {
        List<PaymentLineDto> lineDtos = s.getLines() == null ? List.of() : s.getLines().stream()
                .map(l -> PaymentLineDto.builder()
                        .ticketId(l.getTicketId())
                        .ticketNumber(l.getTicketNumber())
                        .date(l.getDate())
                        .vehicle(l.getVehicle())
                        .driver(l.getDriver())
                        .tonnes(l.getTonnes())
                        .transportAmount(l.getTransportAmount())
                        .distanceKm(l.getDistanceKm())
                        .fuelLitres(l.getFuelLitres())
                        .fuelAmount(l.getFuelAmount())
                        .note(l.getNote())
                        .build())
                .collect(Collectors.toList());

        return PaymentStatementResponse.builder()
                .id(s.getId())
                .transporterId(s.getTransporterId())
                .transporterName(s.getTransporterName())
                .purchaseOrderId(s.getPurchaseOrderId())
                .purchaseOrderNumber(s.getPurchaseOrderNumber())
                .supplier(s.getSupplier())
                .projectName(s.getProjectName())
                .client(s.getClient())
                .provenance(s.getProvenance())
                .destination(s.getDestination())
                .product(s.getProduct())
                .transporterAddress(s.getTransporterAddress())
                .transporterRegistrationNumber(s.getTransporterRegistrationNumber())
                .transporterPhone(s.getTransporterPhone())
                .transporterEmail(s.getTransporterEmail())
                .representativeName(s.getRepresentativeName())
                .representativeEmail(s.getRepresentativeEmail())
                .tonnageTotal(s.getTonnageTotal())
                .tripCount(s.getTripCount())
                .unitPricePerTonne(s.getUnitPricePerTonne())
                .transportAmount(s.getTransportAmount())
                .fuelLitres(s.getFuelLitres())
                .fuelAmount(s.getFuelAmount())
                .invoiceTotal(s.getInvoiceTotal())
                .status(s.getStatus())
                .lines(lineDtos)
                .generatedAt(s.getGeneratedAt())
                .build();
    }
}
