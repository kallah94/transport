package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@CompoundIndexes({
        @CompoundIndex(name = "uk_tenant_username", def = "{'tenantId': 1, 'username': 1}", unique = true),
        @CompoundIndex(name = "uk_tenant_email", def = "{'tenantId': 1, 'email': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends AuditableEntity {

    @Id
    private String id;

    private String username;

    private String password;

    private String fullName;

    private String email;

    private UserRole role;

    /** Camion rattaché pour un compte chauffeur (rôle DRIVER) : correspond à Truck.vehicle. */
    private String vehicle;

    private LocalDateTime lastLogin;

    public enum UserRole {
        SUPER_ADMIN, ADMIN, AGENT, GUEST, DRIVER
    }
}
