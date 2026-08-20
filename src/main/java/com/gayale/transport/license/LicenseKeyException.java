package com.gayale.transport.license;

import lombok.Getter;

/**
 * Cle de licence illisible, mal signee, destinee a un autre produit ou a une autre machine.
 * Porte le {@link LicenseStatus} a afficher au client.
 */
@Getter
public class LicenseKeyException extends RuntimeException {

    private final LicenseStatus status;

    public LicenseKeyException(LicenseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public LicenseKeyException(LicenseStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
