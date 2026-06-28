package com.gayale.transport.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverPaymentLineDto {
    private String driver;
    private int tripCount;
    private double tonnes;
    private double grossAmount;
    private double fuelDeducted;
    private double netAmount;
}
