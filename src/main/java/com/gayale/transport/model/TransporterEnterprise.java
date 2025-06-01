package com.gayale.transport.model;

import java.time.LocalDateTime;

import com.gayale.transport.util.AuditableEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "transporter_enterprises")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransporterEnterprise extends AuditableEntity {

    @Id
    private String id;

    @NotBlank(message = "Le nom de l'entreprise de transport est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Indexed(unique = true)
    private String name;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    @DBRef
    @NotNull(message = "Le représentant est obligatoire")
    private User representative;

    @Min(value = 0, message = "Le nombre de camions ne peut pas être négatif")
    private Integer numberOfTrucks;

    private String phone;

    private String email;

    private String registrationNumber;

    private boolean active = true;
}
