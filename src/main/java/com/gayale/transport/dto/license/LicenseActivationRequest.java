package com.gayale.transport.dto.license;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Corps de POST /api/license/activate : la cle collee par l'administrateur. */
@Data
public class LicenseActivationRequest {

    @NotBlank(message = "La cle de licence est obligatoire")
    private String licenseKey;
}
