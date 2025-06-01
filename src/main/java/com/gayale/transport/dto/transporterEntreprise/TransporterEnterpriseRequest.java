package com.gayale.transport.dto.transporterEntreprise;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransporterEnterpriseRequest {

    @NotBlank(message = "Le nom de l'entreprise de transport est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    @NotNull(message = "L'ID du représentant est obligatoire")
    private String representativeId;

    @Min(value = 0, message = "Le nombre de camions ne peut pas être négatif")
    private Integer numberOfTrucks = 0;

    private String phone;

    @Email(message = "Format d'email invalide")
    private String email;

    private String registrationNumber;

    private Boolean active = true;
}
