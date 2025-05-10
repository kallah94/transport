package com.gayale.transport.service;

import com.gayale.transport.dto.statistics.GeneralStatistics;
import com.gayale.transport.dto.statistics.StatisticsEntity;
import com.gayale.transport.dto.statistics.StatisticsPeriod;
import com.gayale.transport.model.Project;
import com.gayale.transport.model.PurchaseOrder;
import com.gayale.transport.model.WeightTicket;
import com.gayale.transport.repository.ProjectRepository;
import com.gayale.transport.repository.PurchaseOrderRepository;
import com.gayale.transport.repository.WeightTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final ProjectRepository projectRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WeightTicketRepository weightTicketRepository;

    @Autowired
    public StatisticsService(ProjectRepository projectRepository,
                             PurchaseOrderRepository purchaseOrderRepository,
                             WeightTicketRepository weightTicketRepository) {
        this.projectRepository = projectRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.weightTicketRepository = weightTicketRepository;
    }

    public GeneralStatistics getGeneralStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<WeightTicket> allTickets = weightTicketRepository.findAll();
        List<WeightTicket> todayTickets = weightTicketRepository.findByDate(today);
        List<WeightTicket> weekTickets = weightTicketRepository.findByDateBetween(startOfWeek, today);
        List<WeightTicket> monthTickets = weightTicketRepository.findByDateBetween(startOfMonth, today);

        long activeProjectsCount = projectRepository.findByStatus(Project.ProjectStatus.ACTIVE).size();
        long activePurchaseOrdersCount = purchaseOrderRepository.findByStatus(PurchaseOrder.OrderStatus.ACTIVE).size();

        double totalTonnage = allTickets.stream()
                                        .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                        .mapToDouble(WeightTicket::getNetWeight)
                                        .sum();

        int totalTrips = (int) allTickets.stream()
                                         .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                         .count();

        double todayTonnage = todayTickets.stream()
                                          .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                          .mapToDouble(WeightTicket::getNetWeight)
                                          .sum();

        int todayTrips = (int) todayTickets.stream()
                                           .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                           .count();

        double weekTonnage = weekTickets.stream()
                                        .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                        .mapToDouble(WeightTicket::getNetWeight)
                                        .sum();

        int weekTrips = (int) weekTickets.stream()
                                         .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                         .count();

        double monthTonnage = monthTickets.stream()
                                          .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                          .mapToDouble(WeightTicket::getNetWeight)
                                          .sum();

        int monthTrips = (int) monthTickets.stream()
                                           .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                           .count();

        double averageTonnagePerTrip = totalTrips > 0 ? totalTonnage / totalTrips : 0;

        return GeneralStatistics.builder()
                                .totalTonnage(totalTonnage)
                                .totalTrips(totalTrips)
                                .activeProjects((int) activeProjectsCount)
                                .activePurchaseOrders((int) activePurchaseOrdersCount)
                                .averageTonnagePerTrip(averageTonnagePerTrip)
                                .todayTonnage(todayTonnage)
                                .todayTrips(todayTrips)
                                .weekTonnage(weekTonnage)
                                .weekTrips(weekTrips)
                                .monthTonnage(monthTonnage)
                                .monthTrips(monthTrips)
                                .build();
    }

    public StatisticsPeriod getStatisticsByPeriod(LocalDate startDate, LocalDate endDate) {
        List<WeightTicket> tickets = weightTicketRepository.findByDateBetween(startDate, endDate)
                                                           .stream()
                                                           .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                                           .collect(Collectors.toList());

        double totalTonnage = tickets.stream()
                                     .mapToDouble(WeightTicket::getNetWeight)
                                     .sum();

        int totalTrips = tickets.size();
        double averageTonnagePerTrip = totalTrips > 0 ? totalTonnage / totalTrips : 0;

        return StatisticsPeriod.builder()
                               .startDate(startDate)
                               .endDate(endDate)
                               .totalTonnage(totalTonnage)
                               .totalTrips(totalTrips)
                               .averageTonnagePerTrip(averageTonnagePerTrip)
                               .build();
    }

    public List<StatisticsEntity> getStatisticsByProject() {
        List<Project> projects = projectRepository.findAll();
        List<WeightTicket> allTickets = weightTicketRepository.findAll();

        Map<String, List<WeightTicket>> ticketsByProject = allTickets.stream()
                                                                     .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                                                     .collect(Collectors.groupingBy(WeightTicket::getProjectId));

        List<StatisticsEntity> result = new ArrayList<>();

        for (Project project : projects) {
            List<WeightTicket> projectTickets = ticketsByProject.getOrDefault(project.getId(), List.of());
            double totalTonnage = projectTickets.stream()
                                                .mapToDouble(WeightTicket::getNetWeight)
                                                .sum();
            int totalTrips = projectTickets.size();
            double averageTonnagePerTrip = totalTrips > 0 ? totalTonnage / totalTrips : 0;

            result.add(StatisticsEntity.builder()
                                       .entityType(StatisticsEntity.EntityType.PROJECT)
                                       .entityId(project.getId())
                                       .entityName(project.getName())
                                       .totalTonnage(totalTonnage)
                                       .totalTrips(totalTrips)
                                       .averageTonnagePerTrip(averageTonnagePerTrip)
                                       .build());
        }

        return result;
    }

    public List<StatisticsEntity> getStatisticsByPurchaseOrder() {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll();
        List<WeightTicket> allTickets = weightTicketRepository.findAll();

        Map<String, List<WeightTicket>> ticketsByPO = allTickets.stream()
                                                                .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                                                .collect(Collectors.groupingBy(WeightTicket::getPurchaseOrderId));

        List<StatisticsEntity> result = new ArrayList<>();

        for (PurchaseOrder po : purchaseOrders) {
            List<WeightTicket> poTickets = ticketsByPO.getOrDefault(po.getId(), List.of());
            double totalTonnage = poTickets.stream()
                                           .mapToDouble(WeightTicket::getNetWeight)
                                           .sum();
            int totalTrips = poTickets.size();
            double averageTonnagePerTrip = totalTrips > 0 ? totalTonnage / totalTrips : 0;

            result.add(StatisticsEntity.builder()
                                       .entityType(StatisticsEntity.EntityType.PURCHASE_ORDER)
                                       .entityId(po.getId())
                                       .entityName(po.getOrderNumber())
                                       .totalTonnage(totalTonnage)
                                       .totalTrips(totalTrips)
                                       .averageTonnagePerTrip(averageTonnagePerTrip)
                                       .build());
        }

        return result;
    }

    public List<StatisticsEntity> getStatisticsByTransporter() {
        List<WeightTicket> allTickets = weightTicketRepository.findAll().stream()
                                                              .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                                              .collect(Collectors.toList());

        Map<String, List<WeightTicket>> ticketsByTransporter = allTickets.stream()
                                                                         .collect(Collectors.groupingBy(WeightTicket::getTransporter));

        List<StatisticsEntity> result = new ArrayList<>();

        for (Map.Entry<String, List<WeightTicket>> entry : ticketsByTransporter.entrySet()) {
            String transporter = entry.getKey();
            List<WeightTicket> transporterTickets = entry.getValue();

            double totalTonnage = transporterTickets.stream()
                                                    .mapToDouble(WeightTicket::getNetWeight)
                                                    .sum();
            int totalTrips = transporterTickets.size();
            double averageTonnagePerTrip = totalTrips > 0 ? totalTonnage / totalTrips : 0;

            result.add(StatisticsEntity.builder()
                                       .entityType(StatisticsEntity.EntityType.TRANSPORTER)
                                       .entityId(transporter) // Using transporter name as ID
                                       .entityName(transporter)
                                       .totalTonnage(totalTonnage)
                                       .totalTrips(totalTrips)
                                       .averageTonnagePerTrip(averageTonnagePerTrip)
                                       .build());
        }

        return result;
    }

    public List<StatisticsEntity> getStatisticsByVehicle() {
        List<WeightTicket> allTickets = weightTicketRepository.findAll().stream()
                                                              .filter(ticket -> ticket.getStatus() != WeightTicket.TicketStatus.CANCELLED)
                                                              .collect(Collectors.toList());

        Map<String, List<WeightTicket>> ticketsByVehicle = allTickets.stream()
                                                                     .collect(Collectors.groupingBy(WeightTicket::getVehicle));

        List<StatisticsEntity> result = new ArrayList<>();

        for (Map.Entry<String, List<WeightTicket>> entry : ticketsByVehicle.entrySet()) {
            String vehicle = entry.getKey();
            List<WeightTicket> vehicleTickets = entry.getValue();

            double totalTonnage = vehicleTickets.stream()
                                                .mapToDouble(WeightTicket::getNetWeight)
                                                .sum();
            int totalTrips = vehicleTickets.size();
            double averageTonnagePerTrip = totalTrips > 0 ? totalTonnage / totalTrips : 0;

            result.add(StatisticsEntity.builder()
                                       .entityType(StatisticsEntity.EntityType.VEHICLE)
                                       .entityId(vehicle) // Using vehicle registration as ID
                                       .entityName(vehicle)
                                       .totalTonnage(totalTonnage)
                                       .totalTrips(totalTrips)
                                       .averageTonnagePerTrip(averageTonnagePerTrip)
                                       .build());
        }

        return result;
    }
}