package com.gayale.transport.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentLineDto {
    private String ticketId;
    private String ticketNumber;
    private LocalDate date;
    private String vehicle;
    private String driver;
    private double tonnes;
    private double transportAmount;
    private double distanceKm;
    private double fuelLitres;
    private double fuelAmount;
    private String note;
}
