package com.gayale.transport.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGenerationRequest {

    @NotBlank(message = "L'ID du transporteur est obligatoire")
    private String transporterId;

    @NotBlank(message = "L'ID du bon de commande est obligatoire")
    private String purchaseOrderId;
}
