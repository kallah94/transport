package com.gayale.transport.controller;

import com.gayale.transport.dto.ProjectDto;
import com.gayale.transport.dto.ProjectWithPurchaseOrders;
import com.gayale.transport.model.Project;
import com.gayale.transport.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@SecurityRequirement(name = "JWT")
@Tag(name = "Projects", description = "API for project management")
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "Get all projects", description = "Returns a list of all projects")
    public ResponseEntity<List<ProjectDto>> getAllProjects() {
        List<ProjectDto> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Returns a project by its ID")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable String id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active projects", description = "Returns a list of all active projects")
    public ResponseEntity<List<ProjectDto>> getActiveProjects() {
        List<ProjectDto> projects = projectService.getActiveProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/client/{client}")
    @Operation(summary = "Get projects by client", description = "Returns a list of projects for a specific client")
    public ResponseEntity<List<ProjectDto>> getProjectsByClient(@PathVariable String client) {
        List<ProjectDto> projects = projectService.getProjectsByClient(client);
        return ResponseEntity.ok(projects);
    }

    @PostMapping
  //  @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create a new project", description = "Creates a new project and returns the created project")
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody ProjectDto projectDto) {
        ProjectDto createdProject = projectService.createProject(projectDto);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Update a project", description = "Updates a project by its ID and returns the updated project")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable String id, @Valid @RequestBody ProjectDto projectDto) {
        ProjectDto updatedProject = projectService.updateProject(id, projectDto);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a project", description = "Deletes a project by its ID")
    public ResponseEntity<Boolean> deleteProject(@PathVariable String id) {
        boolean deleted = projectService.deleteProject(id);
        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/client/{client}/purchase-orders")
    @Operation(summary = "Get projects with purchase orders by client", description = "Returns a list of projects with purchase orders for a specific client")
    public ResponseEntity<List<ProjectWithPurchaseOrders>> getProjectsWithPurchaseOrdersByClient(@PathVariable String client) {
        List<ProjectWithPurchaseOrders> projects = projectService.getProjectsWithPurchaseOrdersByClient(client);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/status/{status}/purchase-orders")
    @Operation(summary = "Get projects with purchase orders by status", description = "Returns a list of projects with purchase orders by status")
    public ResponseEntity<List<ProjectWithPurchaseOrders>> getProjectsWithPurchaseOrdersByStatus(@PathVariable Project.ProjectStatus status) {
        List<ProjectWithPurchaseOrders> projects = projectService.getProjectsWithPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/purchase-orders")
    @Operation(summary = "Get all projects with purchase orders", description = "Returns a list of all projects with purchase orders")
    public ResponseEntity<List<ProjectWithPurchaseOrders>> getAllProjectsWithPurchaseOrders() {
        List<ProjectWithPurchaseOrders> projects = projectService.getAllProjectsWithPurchaseOrders();
        return ResponseEntity.ok(projects);
    }
}