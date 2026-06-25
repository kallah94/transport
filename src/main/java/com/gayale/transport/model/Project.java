package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Project extends AuditableEntity {

    @Id
    private String id;

    private String name;

    private String client;

    private String destination;

    private String provenance;

    private String product;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status;

    private double totalDeliveredTonnage;

    public enum ProjectStatus {
        ACTIVE, COMPLETED, CANCELLED
    }
}