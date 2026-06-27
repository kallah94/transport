package com.gayale.transport.service;

import com.gayale.transport.dto.ProjectDto;
import com.gayale.transport.dto.ProjectWithPurchaseOrders;
import com.gayale.transport.dto.PurchaseOrderDto;
import com.gayale.transport.exception.ResourceNotFoundException;
import com.gayale.transport.model.Project;
import com.gayale.transport.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, PurchaseOrderService purchaseOrderService) {
        this.projectRepository = projectRepository;
        this.purchaseOrderService = purchaseOrderService;
    }

    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                                .map(this::mapProjectToDto)
                                .collect(Collectors.toList());
    }

    public List<ProjectWithPurchaseOrders> getAllProjectsWithPurchaseOrders() {
      return projectRepository.findAll().stream()
                                .map(project -> {
                                    List<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getPurchaseOrdersByProject(project.getId());
                                    return mapProjectWithPurchaseOrdersToDto(project, purchaseOrders);
                                })
                                .collect(Collectors.toList());
    }
    public List<ProjectWithPurchaseOrders> getProjectsWithPurchaseOrdersByClient(String client) {
        return projectRepository.findByClient(client).stream()
                                .map(project -> {
                                    List<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getPurchaseOrdersByProject(project.getId());
                                    return mapProjectWithPurchaseOrdersToDto(project, purchaseOrders);
                                })
                                .collect(Collectors.toList());
    }

    public List<ProjectWithPurchaseOrders> getProjectsWithPurchaseOrdersByStatus(Project.ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                                .map(project -> {
                                    List<PurchaseOrderDto> purchaseOrders = purchaseOrderService.getPurchaseOrdersByProject(project.getId());
                                    return mapProjectWithPurchaseOrdersToDto(project, purchaseOrders);
                                })
                                .collect(Collectors.toList());
    }


    public ProjectDto getProjectById(String id) {
        Project project = projectRepository.findById(id)
                                           .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return mapProjectToDto(project);
    }

    public List<ProjectDto> getActiveProjects() {
        return projectRepository.findByStatus(Project.ProjectStatus.ACTIVE).stream()
                                .map(this::mapProjectToDto)
                                .collect(Collectors.toList());
    }

    public List<ProjectDto> getProjectsByClient(String client) {
        return projectRepository.findByClient(client).stream()
                                .map(this::mapProjectToDto)
                                .collect(Collectors.toList());
    }

    public ProjectDto createProject(ProjectDto projectDto) {
        Project project = new Project();
        project.setName(projectDto.getName());
        project.setClient(projectDto.getClient());
        project.setDestination(projectDto.getDestination());
        project.setProvenance(projectDto.getProvenance());
        project.setProduct(projectDto.getProduct());
        project.setDistanceKm(projectDto.getDistanceKm());
        project.setStartDate(projectDto.getStartDate());
        project.setEndDate(projectDto.getEndDate());
        project.setStatus(projectDto.getStatus() != null ? projectDto.getStatus() : Project.ProjectStatus.ACTIVE);
        project.setTotalDeliveredTonnage(0.0); // Initialize with zero

        Project savedProject = projectRepository.save(project);
        return mapProjectToDto(savedProject);
    }

    public ProjectDto updateProject(String id, ProjectDto projectDto) {
        Project existingProject = projectRepository.findById(id)
                                                   .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        existingProject.setName(projectDto.getName());
        existingProject.setClient(projectDto.getClient());
        existingProject.setDestination(projectDto.getDestination());
        existingProject.setProvenance(projectDto.getProvenance());
        existingProject.setProduct(projectDto.getProduct());
        existingProject.setDistanceKm(projectDto.getDistanceKm());
        existingProject.setStartDate(projectDto.getStartDate());
        existingProject.setEndDate(projectDto.getEndDate());
        existingProject.setStatus(projectDto.getStatus());

        // Don't update totalDeliveredTonnage from DTO as it's calculated from tickets
        // This field should only be updated through internal service calls

        Project updatedProject = projectRepository.save(existingProject);
        return mapProjectToDto(updatedProject);
    }

    public boolean deleteProject(String id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
        return true;
    }

    // Internal method to update total delivered tonnage
    public void updateTotalDeliveredTonnage(String projectId, double additionalTonnage) {
        Project project = projectRepository.findById(projectId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        project.setTotalDeliveredTonnage(project.getTotalDeliveredTonnage() + additionalTonnage);
        projectRepository.save(project);
    }

    private ProjectDto mapProjectToDto(Project project) {
        return ProjectDto.builder()
                         .id(project.getId())
                         .name(project.getName())
                         .provenance(project.getProvenance())
                         .product(project.getProduct())
                         .client(project.getClient())
                         .destination(project.getDestination())
                         .startDate(project.getStartDate())
                         .endDate(project.getEndDate())
                         .status(project.getStatus())
                         .totalDeliveredTonnage(project.getTotalDeliveredTonnage())
                         .distanceKm(project.getDistanceKm())
                         .createdAt(project.getCreatedAt())
                         .updatedAt(project.getUpdatedAt())
                         .build();
    }

    private ProjectWithPurchaseOrders mapProjectWithPurchaseOrdersToDto(Project project, List<PurchaseOrderDto> purchaseOrders) {
        return ProjectWithPurchaseOrders.builder()
                                        .id(project.getId())
                                        .name(project.getName())
                                        .client(project.getClient())
                                        .destination(project.getDestination())
                                        .provenance(project.getProvenance())
                                        .product(project.getProduct())
                                        .purchaseOrders(purchaseOrders)
                                        .startDate(project.getStartDate())
                                        .endDate(project.getEndDate())
                                        .status(project.getStatus())
                                        .totalDeliveredTonnage(project.getTotalDeliveredTonnage())
                                        .distanceKm(project.getDistanceKm())
                                        .createdAt(project.getCreatedAt())
                                        .updatedAt(project.getUpdatedAt())
                                        .build();
    }
}