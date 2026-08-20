package com.gayale.transport.service;

import com.gayale.transport.dto.driver.DriverDto;
import com.gayale.transport.model.Project;
import com.gayale.transport.model.Truck;
import com.gayale.transport.model.WeightTicket;
import com.gayale.transport.repository.ProjectRepository;
import com.gayale.transport.repository.TruckRepository;
import com.gayale.transport.repository.WeightTicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Données de l'app chauffeur, toujours restreintes à UN camion (`vehicle`). Réutilise les
 * tickets de pesée : tonnage = netWeight/1000, hors tickets annulés.
 */
@Service
public class DriverService {

    private final WeightTicketRepository weightTicketRepository;
    private final ProjectRepository projectRepository;
    private final TruckRepository truckRepository;

    public DriverService(WeightTicketRepository weightTicketRepository,
                         ProjectRepository projectRepository,
                         TruckRepository truckRepository) {
        this.weightTicketRepository = weightTicketRepository;
        this.projectRepository = projectRepository;
        this.truckRepository = truckRepository;
    }

    private List<WeightTicket> activeTickets(String vehicle) {
        return weightTicketRepository.findByVehicle(vehicle).stream()
                .filter(t -> t.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                .collect(Collectors.toList());
    }

    private double tonnes(WeightTicket t) {
        return t.getNetWeight() / 1000.0;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public DriverDto.Profile profile(String vehicle) {
        List<WeightTicket> tickets = activeTickets(vehicle);
        YearMonth now = YearMonth.now();
        double tonnesMonth = 0, total = 0;
        long tripsMonth = 0;
        for (WeightTicket t : tickets) {
            double to = tonnes(t);
            total += to;
            if (t.getDate() != null && YearMonth.from(t.getDate()).equals(now)) {
                tonnesMonth += to;
                tripsMonth++;
            }
        }
        String transporterName = null;
        double capacity = 0, conso = 0;
        Truck truck = truckRepository.findByVehicle(vehicle).orElse(null);
        if (truck != null) {
            capacity = truck.getCapacityTonnes();
            conso = truck.getFuelConsumptionLPerKm();
            if (truck.getTransporter() != null) {
                transporterName = truck.getTransporter().getName();
            }
        }
        return new DriverDto.Profile(vehicle, transporterName, capacity, conso,
                round(tonnesMonth), tripsMonth, round(total), tickets.size());
    }

    public List<DriverDto.Trip> trips(String vehicle, LocalDate from, LocalDate to, String projectId) {
        Map<String, String> names = new HashMap<>();
        return activeTickets(vehicle).stream()
                .filter(t -> from == null || (t.getDate() != null && !t.getDate().isBefore(from)))
                .filter(t -> to == null || (t.getDate() != null && !t.getDate().isAfter(to)))
                .filter(t -> projectId == null || projectId.isBlank() || projectId.equals(t.getProjectId()))
                .sorted(Comparator.comparing(WeightTicket::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(t -> new DriverDto.Trip(
                        t.getId(), t.getTicketNumber(), t.getDate(), t.getVehicle(), t.getDriver(),
                        t.getProduct(), t.getProjectId(), projectName(names, t.getProjectId()),
                        t.getClient(), t.getSupplier(), t.getPurchaseOrderNumber(),
                        t.getOrigin(), t.getDestination(), t.getOperatorName(),
                        round(t.getEmptyWeight()), round(t.getLoadedWeight()), round(t.getNetWeight()),
                        round(tonnes(t)), t.getStatus() != null ? t.getStatus().name() : null,
                        t.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<DriverDto.Project> projects(String vehicle) {
        Map<String, List<WeightTicket>> byProject = activeTickets(vehicle).stream()
                .filter(t -> t.getProjectId() != null)
                .collect(Collectors.groupingBy(WeightTicket::getProjectId));

        List<DriverDto.Project> result = new ArrayList<>();
        for (Map.Entry<String, List<WeightTicket>> e : byProject.entrySet()) {
            Project p = projectRepository.findById(e.getKey()).orElse(null);
            double myTonnes = e.getValue().stream().mapToDouble(this::tonnes).sum();
            result.add(new DriverDto.Project(
                    e.getKey(),
                    p != null ? p.getName() : "(projet inconnu)",
                    p != null ? p.getClient() : null,
                    p != null ? p.getDestination() : null,
                    p != null ? p.getProvenance() : null,
                    p != null ? p.getProduct() : null,
                    (p != null && p.getStatus() != null) ? p.getStatus().name() : null,
                    p != null ? round(p.getTotalDeliveredTonnage()) : 0,
                    round(myTonnes),
                    e.getValue().size()));
        }
        result.sort(Comparator.comparingDouble(DriverDto.Project::myTonnes).reversed());
        return result;
    }

    public DriverDto.Stats stats(String vehicle) {
        List<WeightTicket> tickets = activeTickets(vehicle);
        double total = tickets.stream().mapToDouble(this::tonnes).sum();
        Map<String, String> names = new HashMap<>();

        List<DriverDto.StatBucket> byProject = tickets.stream()
                .filter(t -> t.getProjectId() != null)
                .collect(Collectors.groupingBy(WeightTicket::getProjectId))
                .entrySet().stream()
                .map(e -> new DriverDto.StatBucket(e.getKey(), projectName(names, e.getKey()),
                        round(e.getValue().stream().mapToDouble(this::tonnes).sum()), e.getValue().size()))
                .sorted(Comparator.comparingDouble(DriverDto.StatBucket::tonnes).reversed())
                .collect(Collectors.toList());

        List<DriverDto.StatBucket> byMonth = tickets.stream()
                .filter(t -> t.getDate() != null)
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())))
                .entrySet().stream()
                .sorted(Map.Entry.<YearMonth, List<WeightTicket>>comparingByKey().reversed())
                .limit(12)
                .map(e -> new DriverDto.StatBucket(e.getKey().toString(), e.getKey().toString(),
                        round(e.getValue().stream().mapToDouble(this::tonnes).sum()), e.getValue().size()))
                .collect(Collectors.toList());

        return new DriverDto.Stats(round(total), tickets.size(), byProject, byMonth);
    }

    private String projectName(Map<String, String> cache, String projectId) {
        if (projectId == null) {
            return null;
        }
        return cache.computeIfAbsent(projectId,
                pid -> projectRepository.findById(pid).map(Project::getName).orElse("(projet inconnu)"));
    }
}
