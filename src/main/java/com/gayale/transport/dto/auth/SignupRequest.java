package com.gayale.transport.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Auto-inscription d'un transporteur (mode shared) : cree un tenant + son administrateur. */
@Data
public class SignupRequest {

    /** Cle = sous-domaine souhaite (lettres/chiffres/tirets). */
    @NotBlank
    @Size(min = 2, max = 40)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "La cle ne doit contenir que des minuscules, chiffres et tirets")
    private String tenantKey;

    @NotBlank
    private String title;

    @NotBlank
    private String adminUsername;

    @NotBlank
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caracteres")
    private String adminPassword;

    @NotBlank
    @Email
    private String adminEmail;

    private String adminFullName;
}
