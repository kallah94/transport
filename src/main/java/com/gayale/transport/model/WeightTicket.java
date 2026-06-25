package com.gayale.transport.model;

import com.gayale.transport.util.AuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Document(collection = "weight_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WeightTicket extends AuditableEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String ticketNumber;

    private LocalDate date;

    private double emptyWeight;

    private double loadedWeight;

    private double netWeight;

    private double variance;

    private String vehicle;

    private String driver;

    private String product;

    private String client;

    private String supplier;

    private String origin;

    private String destination;

    private String transporter;

    private String purchaseOrderNumber;

    private String projectId;

    private String purchaseOrderId;

    private String operatorName;

    private String qrCode;

    private TicketStatus status;

    @Indexed(unique = true, sparse = true)
    private String checksum;

    public enum TicketStatus {
        PENDING, VALIDATED, CANCELLED
    }

    // Calcule le poids net (charge - vide).
    // TODO(metier) : la variance attend une definition claire. Faute de poids attendu
    // sur le ticket, elle reste a -netWeight (placeholder). A preciser avec le metier.
    public void calculateWeights() {
        this.netWeight = this.loadedWeight - this.emptyWeight;
        this.variance = - this.netWeight;
    }

    public String calculateChecksum() {
        try {
            StringBuilder checksumBuilder = new StringBuilder();
            appendField(checksumBuilder, "client", this.client);
            appendField(checksumBuilder, "destination", this.destination);
            appendField(checksumBuilder, "driver", this.driver);
            appendNumericField(checksumBuilder, "emptyWeight", this.emptyWeight, 8);
            appendNumericField(checksumBuilder, "loadedWeight", this.loadedWeight, 8);

            appendField(checksumBuilder, "origin", this.origin);
            appendField(checksumBuilder, "product", this.product);
            appendField(checksumBuilder, "projectId", this.projectId);
            appendField(checksumBuilder, "purchaseOrderId", this.purchaseOrderId);
            appendField(checksumBuilder, "purchaseOrderNumber", this.purchaseOrderNumber);
            appendField(checksumBuilder, "supplier", this.supplier);
            appendField(checksumBuilder, "transporter", this.transporter);
            appendField(checksumBuilder, "vehicle", this.vehicle);
            appendField(checksumBuilder, "date", this.date != null ?
                    this.date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            appendField(checksumBuilder, "operatorName", this.operatorName);
            String input = checksumBuilder.toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] firstHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String saltedInput = input + ":SALT:" + input.length();
            byte[] secondHash = digest.digest(saltedInput.getBytes(StandardCharsets.UTF_8));
            StringBuilder finalHash = new StringBuilder();
            for (int i = 0; i < firstHash.length; i++) {
                byte combined = (byte) (firstHash[i] ^ secondHash[i]); // XOR
                finalHash.append(String.format("%02x", combined & 0xff));
            }

            return finalHash.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur algorithme checksum", e);
        }
    }

    private void appendField(StringBuilder builder, String fieldName, String value) {
        builder.append(fieldName)
               .append("=[")
               .append(sanitizeValue(value))
               .append("];");
    }
    private void appendNumericField(StringBuilder builder, String fieldName, double value, int precision) {
        BigDecimal bd = BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);

        builder.append(fieldName)
               .append("=[")
               .append(bd.toPlainString())
               .append("];");
    }

    private String sanitizeValue(String value) {
        if (value == null) {
            return "NULL";
        }

        return value.trim()
                    .replaceAll("[\\[\\];=]", "_")
                    .replaceAll("\\s+", " ");
    }


    public void updateChecksum() {
        this.checksum = calculateChecksum();
    }

    public boolean isChecksumValid() {
        if (this.checksum == null) {
            return false;
        }
        return this.checksum.equals(calculateChecksum());
    }

    public boolean verifyIntegrity() {
        return isChecksumValid();
    }

    public void calculateWeightsAndChecksum() {
        calculateWeights();
        updateChecksum();
    }
}
