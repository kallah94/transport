package com.gayale.transport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.gayale.transport.dto.truck.TruckRequest;
import com.gayale.transport.dto.truck.TruckResponse;
import com.gayale.transport.dto.truck.TruckStatistics;
import com.gayale.transport.model.DriverRate;
import com.gayale.transport.model.FuelConfig;
import com.gayale.transport.model.Project;
import com.gayale.transport.model.WeightTicket;
import com.gayale.transport.repository.DriverRateRepository;
import com.gayale.transport.repository.FuelConfigRepository;
import com.gayale.transport.repository.ProjectRepository;
import com.gayale.transport.repository.WeightTicketRepository;
import com.gayale.transport.exception.DuplicateResourceException;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.iservices.ITruckService;
import com.gayale.transport.license.LicenseGuard;
import com.gayale.transport.model.Truck;
import com.gayale.transport.model.TransporterEnterprise;
import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.TransporterEnterpriseRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TruckService implements ITruckService {
    private final TruckRepository truckRepository;
    private final TransporterEnterpriseRepository transporterEnterpriseRepository;
    private final WeightTicketRepository weightTicketRepository;
    private final ProjectRepository projectRepository;
    private final DriverRateRepository driverRateRepository;
    private final FuelConfigRepository fuelConfigRepository;
    private final ModelMapper modelMapper;
    private final LicenseGuard licenseGuard;

    @Autowired
    public TruckService(TruckRepository truckRepository,
                        TransporterEnterpriseRepository transporterEnterpriseRepository,
                        WeightTicketRepository weightTicketRepository,
                        ProjectRepository projectRepository,
                        DriverRateRepository driverRateRepository,
                        FuelConfigRepository fuelConfigRepository,
                        ModelMapper modelMapper,
                        LicenseGuard licenseGuard) {
        this.truckRepository = truckRepository;
        this.transporterEnterpriseRepository = transporterEnterpriseRepository;
        this.weightTicketRepository = weightTicketRepository;
        this.projectRepository = projectRepository;
        this.driverRateRepository = driverRateRepository;
        this.fuelConfigRepository = fuelConfigRepository;
        this.modelMapper = modelMapper;
        this.licenseGuard = licenseGuard;
    }

    public TruckStatistics getTruckStatistics(String id) {
        Truck truck = truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));

        List<WeightTicket> tickets = weightTicketRepository.findByVehicle(truck.getVehicle()).stream()
                .filter(t -> t.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                .collect(Collectors.toList());

        int totalTrips = tickets.size();
        double totalTonnage = tickets.stream().mapToDouble(t -> t.getNetWeight() / 1000.0).sum();
        double avg = totalTrips > 0 ? totalTonnage / totalTrips : 0;
        double capacity = truck.getCapacityTonnes();
        double utilization = capacity > 0 ? (avg / capacity) * 100 : 0;

        LocalDate first = tickets.stream().map(WeightTicket::getDate).filter(d -> d != null).min(Comparator.naturalOrder()).orElse(null);
        LocalDate last = tickets.stream().map(WeightTicket::getDate).filter(d -> d != null).max(Comparator.naturalOrder()).orElse(null);

        // Repartition par projet
        Map<String, double[]> projAgg = new java.util.LinkedHashMap<>();
        for (WeightTicket t : tickets) {
            String pid = t.getProjectId() != null ? t.getProjectId() : "(sans projet)";
            double[] a = projAgg.computeIfAbsent(pid, k -> new double[]{0, 0});
            a[0] += 1;
            a[1] += t.getNetWeight() / 1000.0;
        }
        List<TruckStatistics.Breakdown> byProject = new ArrayList<>();
        for (Map.Entry<String, double[]> e : projAgg.entrySet()) {
            String name = e.getKey();
            if (!"(sans projet)".equals(name)) {
                name = projectRepository.findById(e.getKey()).map(Project::getName).orElse(e.getKey());
            }
            byProject.add(TruckStatistics.Breakdown.builder()
                    .label(name).trips((int) e.getValue()[0]).tonnage(round(e.getValue()[1])).build());
        }

        // Repartition par mois (yyyy-MM, ordonne)
        Map<String, double[]> monthAgg = new TreeMap<>();
        for (WeightTicket t : tickets) {
            if (t.getDate() == null) continue;
            String ym = YearMonth.from(t.getDate()).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            double[] a = monthAgg.computeIfAbsent(ym, k -> new double[]{0, 0});
            a[0] += 1;
            a[1] += t.getNetWeight() / 1000.0;
        }
        List<TruckStatistics.Breakdown> byMonth = new ArrayList<>();
        for (Map.Entry<String, double[]> e : monthAgg.entrySet()) {
            byMonth.add(TruckStatistics.Breakdown.builder()
                    .label(e.getKey()).trips((int) e.getValue()[0]).tonnage(round(e.getValue()[1])).build());
        }

        // Remuneration : tonnage x tarif chauffeur, dotation carburant
        String transporterId = truck.getTransporter() != null ? truck.getTransporter().getId() : null;
        FuelConfig fc = fuelConfigRepository.findFirstByActiveTrueOrderByEffectiveFromDesc().orElse(null);
        double fuelPrice = fc != null ? fc.getFuelPricePerLitre() : 0.0;
        double rtf = (fc != null && fc.getRoundTripFactor() > 0) ? fc.getRoundTripFactor() : 2.0;
        double conso = truck.getFuelConsumptionLPerKm();
        double grossReceive = 0, fuelLitres = 0, fuelAmount = 0;
        Map<String, Double> distCache = new HashMap<>();
        for (WeightTicket t : tickets) {
            double tonnes = t.getNetWeight() / 1000.0;
            grossReceive += tonnes * resolveDriverRate(transporterId, t.getDate());
            double dist = (t.getProjectId() != null)
                    ? distCache.computeIfAbsent(t.getProjectId(), pid -> projectRepository.findById(pid).map(Project::getDistanceKm).orElse(0.0)) : 0.0;
            if (dist > 0 && conso > 0) {
                double litres = dist * rtf * conso;
                fuelLitres += litres;
                fuelAmount += litres * fuelPrice;
            }
        }
        double netReceive = Math.max(0, grossReceive - fuelAmount);

        return TruckStatistics.builder()
                .id(truck.getId())
                .vehicle(truck.getVehicle())
                .driverName(truck.getDriverName())
                .transporterName(truck.getTransporter() != null ? truck.getTransporter().getName() : null)
                .capacityTonnes(capacity)
                .fuelConsumptionLPerKm(truck.getFuelConsumptionLPerKm())
                .totalTrips(totalTrips)
                .totalTonnage(round(totalTonnage))
                .averageTonnagePerTrip(round(avg))
                .capacityUtilizationPct(round(utilization))
                .firstTripDate(first)
                .lastTripDate(last)
                .grossToReceive(round(grossReceive))
                .totalFuelLitres(round(fuelLitres))
                .totalFuelAmount(round(fuelAmount))
                .netToReceive(round(netReceive))
                .byProject(byProject)
                .byMonth(byMonth)
                .build();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double resolveDriverRate(String transporterId, LocalDate date) {
        return driverRateRepository.findByActiveTrue().stream()
                .filter(r -> coversDate(r.getEffectiveFrom(), r.getEffectiveTo(), date))
                .filter(r -> isBlankStr(r.getTransporterId()) || r.getTransporterId().equals(transporterId))
                .max(Comparator.comparingInt(r -> !isBlankStr(r.getTransporterId()) ? 1 : 0))
                .map(DriverRate::getPricePerTonne)
                .orElse(0.0);
    }

    private boolean isBlankStr(String v) {
        return v == null || v.isBlank();
    }

    private boolean coversDate(LocalDate from, LocalDate to, LocalDate date) {
        if (date == null) return false;
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    @Override
    public List<TruckResponse> getAllTrucks() {
        return truckRepository.findAll().stream()
                              .map(truck -> modelMapper.map(truck, TruckResponse.class))
                              .collect(Collectors.toList());
    }

    @Override
    public TruckResponse getTruckById(String id) {
        Truck truck = truckRepository.findById(id)
                                     .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));
        return modelMapper.map(truck, TruckResponse.class);
    }

    @Override
    public TruckResponse getTruckByVehicle(String vehicle) {
        Truck truck = truckRepository.findByVehicle(vehicle)
                                     .orElseThrow(() -> new ResourceNotFoundException("Truck not found with vehicle: " + vehicle));
        return modelMapper.map(truck, TruckResponse.class);
    }

    @Override
    public List<TruckResponse> getTrucksByTransporterId(String transporterId) {
        return truckRepository.findByTransporterId(transporterId).stream()
                              .map(truck -> modelMapper.map(truck, TruckResponse.class))
                              .collect(Collectors.toList());
    }

    @Override
    public List<TruckResponse> getTrucksByTransporterName(String transporterName) {
        return truckRepository.findByTransporterName(transporterName).stream()
                              .map(truck -> modelMapper.map(truck, TruckResponse.class))
                              .collect(Collectors.toList());
    }


    public void updateTransporterTruckCount(TransporterEnterprise transporter) {
        try {
            long count = truckRepository.countByTransporter(transporter);
            transporter.setNumberOfTrucks((int) count);
            transporterEnterpriseRepository.save(transporter);
        } catch (Exception e) {
            System.err.println("Error details: " + e.getMessage());
        }
    }

    @Override
    public TruckResponse createTruck(TruckRequest truckRequest) {
        // Quota contractuel : refuse l'ajout au-dela du nombre de camions vendu (HTTP 402).
        licenseGuard.checkTruckQuota();

        if (truckRepository.existsByVehicle(truckRequest.getVehicle())) {
            System.err.println("ERROR: Vehicle already exists: " + truckRequest.getVehicle());
            throw new DuplicateResourceException("Truck with this vehicle number " + truckRequest.getVehicle() + " already exists");
        }
        TransporterEnterprise transporter = transporterEnterpriseRepository.findById(truckRequest.getTransporterId())
               .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + truckRequest.getTransporterId()));
        Truck truck = modelMapper.map(truckRequest, Truck.class);
        System.err.println("truck id " + truck.getId());
        truck.setId(null);
        truck.setTransporter(transporter);
        truck.setCreatedAt(LocalDateTime.now());
        truck.setUpdatedAt(LocalDateTime.now());
        Truck savedTruck = truckRepository.save(truck);
        this.updateTransporterTruckCount(transporter);
        return modelMapper.map(savedTruck, TruckResponse.class);
    }

    @Override
    public TruckResponse updateTruck(String id, TruckRequest truckRequest) {
        Truck existingTruck = truckRepository.findById(id)
                                             .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));

        // Check for duplicate vehicle number (only if vehicle is being changed)
        if (!existingTruck.getVehicle().equals(truckRequest.getVehicle()) &&
                truckRepository.existsByVehicle(truckRequest.getVehicle())) {
            throw new DuplicateResourceException("Truck with this vehicle number " + truckRequest.getVehicle() + " already exists");
        }

        // Validate that the transporter exists (if being changed)
        if (!existingTruck.getTransporter().getId().equals(truckRequest.getTransporterId())) {
            TransporterEnterprise transporter = transporterEnterpriseRepository.findById(truckRequest.getTransporterId())
                                                                               .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + truckRequest.getTransporterId()));
            existingTruck.setTransporter(transporter);
        }

        existingTruck.setVehicle(truckRequest.getVehicle());
        existingTruck.setDriverName(truckRequest.getDriverName());
        existingTruck.setPhone(truckRequest.getPhone());
        existingTruck.setCapacityTonnes(truckRequest.getCapacityTonnes());
        existingTruck.setFuelConsumptionLPerKm(truckRequest.getFuelConsumptionLPerKm());
        existingTruck.setUpdatedAt(LocalDateTime.now());

        Truck updatedTruck = truckRepository.save(existingTruck);
        return modelMapper.map(updatedTruck, TruckResponse.class);
    }

    @Override
    public void deleteTruck(String id) {
        Truck truck = truckRepository.findById(id)
         .orElseThrow(() -> new ResourceNotFoundException("Truck not found with id: " + id));
        TransporterEnterprise transporter = truck.getTransporter();
        truckRepository.deleteById(id);
        updateTransporterTruckCount(transporter);
    }

    @Override
    public boolean vehicleExists(String vehicle) {
        return truckRepository.existsByVehicle(vehicle);
    }
}