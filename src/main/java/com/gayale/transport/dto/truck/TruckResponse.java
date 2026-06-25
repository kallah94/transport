package com.gayale.transport.dto.truck;

import com.gayale.transport.dto.transporterEntreprise.TransporterTruckDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TruckResponse {

    private String id;
    private String vehicle;
    private TransporterTruckDto transporter;
    private String driverName;
    private String phone;
    private String createdAt;
    private String updatedAt;
}
