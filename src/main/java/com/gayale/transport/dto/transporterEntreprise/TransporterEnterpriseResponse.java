package com.gayale.transport.dto.transporterEntreprise;

import java.time.LocalDateTime;

import com.gayale.transport.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransporterEnterpriseResponse {
    private String id;
    private String name;
    private String address;
    private UserDto representative;
    private Integer numberOfTrucks;
    private String phone;
    private String email;
    private String registrationNumber;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
