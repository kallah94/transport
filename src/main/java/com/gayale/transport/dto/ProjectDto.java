package com.gayale.transport.dto;

import com.gayale.transport.model.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {

    private String id;

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Client is required")
    private String client;

    private String destination;

    private String provenance;

    private String product;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Project.ProjectStatus status;

    private double totalDeliveredTonnage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

