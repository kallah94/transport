package com.gayale.transport.dto.truck;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TruckRequest {

    @NotBlank(message = "Matricule ne peut pas etre null")
    private String vehicle;

    @NotBlank(message = "L'ID du Transporter ne peut pas etre null")
    private String transporterId;

    @NotBlank(message = "Driver Name ne peut pas etre null")
    private String driverName;

    @Pattern(regexp = "^(\\+221|00221)?[ ]?[7][0-9]{1}[ ]?[0-9]{3}[ ]?[0-9]{2}[ ]?[0-9]{2}$",
            message = "Must be a valid Senegal phone number")
    private String phone;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
