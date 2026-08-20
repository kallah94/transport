package com.gayale.transport.license;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ces tests protegent la propriete qui fait tenir tout le modele commercial :
 * une cle de licence ne peut etre ni fabriquee, ni modifiee sans la cle privee de l'editeur.
 */
class LicenseKeyCodecTest {

    private static KeyPair keyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static LicenseClaims proClaims() {
        return LicenseClaims.builder()
                .product(LicenseKeyCodec.PRODUCT)
                .licenseId("LIC-TEST01")
                .customer("Client A SARL")
                .customerKey("clienta")
                .plan("PRO")
                .issuedAt(LocalDate.of(2026, 1, 1))
                .expiresAt(LocalDate.of(2027, 1, 1))
                .extraFeatures(new LinkedHashSet<>(List.of("DRIVER_MOBILE_APP")))
                .build();
    }

    @Test
    @DisplayName("Une cle signee se relit a l'identique")
    void roundTrip() {
        KeyPair kp = keyPair();
        String key = LicenseKeyCodec.sign(proClaims(), kp.getPrivate());

        LicenseClaims back = LicenseKeyCodec.verify(key, kp.getPublic());

        assertEquals("Client A SARL", back.getCustomer());
        assertEquals(LicensePlan.PRO, back.resolvedPlan());
        assertEquals(LocalDate.of(2027, 1, 1), back.getExpiresAt());
        assertTrue(back.resolvedFeatures().contains(LicenseFeature.PAYMENTS), "socle du plan PRO");
        assertTrue(back.resolvedFeatures().contains(LicenseFeature.DRIVER_MOBILE_APP), "add-on vendu");
        assertEquals(LicensePlan.PRO.getMaxTrucks(), back.effectiveMaxTrucks());
    }

    @Test
    @DisplayName("Les retours a la ligne du courriel n'empechent pas la relecture")
    void toleratesLineBreaks() {
        KeyPair kp = keyPair();
        String key = LicenseKeyCodec.sign(proClaims(), kp.getPrivate());

        String pretty = LicenseKeyCodec.pretty(key);

        assertEquals("LIC-TEST01", LicenseKeyCodec.verify(pretty, kp.getPublic()).getLicenseId());
    }

    @Test
    @DisplayName("Un payload retouche (STARTER -> ENTERPRISE) est rejete")
    void rejectsTamperedPayload() {
        KeyPair kp = keyPair();
        String key = LicenseKeyCodec.sign(proClaims(), kp.getPrivate());
        String[] parts = key.split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"PRO\"", "\"ENTERPRISE\"");
        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        LicenseKeyException e = assertThrows(LicenseKeyException.class,
                () -> LicenseKeyCodec.verify(forged, kp.getPublic()));

        assertEquals(LicenseStatus.INVALID, e.getStatus());
    }

    @Test
    @DisplayName("Une cle signee par un tiers est rejetee")
    void rejectsForeignSignature() {
        KeyPair mine = keyPair();
        KeyPair attacker = keyPair();
        String forged = LicenseKeyCodec.sign(proClaims(), attacker.getPrivate());

        LicenseKeyException e = assertThrows(LicenseKeyException.class,
                () -> LicenseKeyCodec.verify(forged, mine.getPublic()));

        assertEquals(LicenseStatus.INVALID, e.getStatus());
    }

    @Test
    @DisplayName("Une cle emise pour un autre produit est rejetee")
    void rejectsOtherProduct() {
        KeyPair kp = keyPair();
        LicenseClaims other = LicenseClaims.builder()
                .product("AUTRE-LOGICIEL").plan("PRO").customer("X").build();
        String key = LicenseKeyCodec.sign(other, kp.getPrivate());

        assertThrows(LicenseKeyException.class, () -> LicenseKeyCodec.verify(key, kp.getPublic()));
    }

    @Test
    @DisplayName("Un plan inconnu de cette version est rejete plutot qu'ignore")
    void rejectsUnknownPlan() {
        KeyPair kp = keyPair();
        LicenseClaims future = LicenseClaims.builder()
                .product(LicenseKeyCodec.PRODUCT).plan("PLATINUM").customer("Y").build();
        String key = LicenseKeyCodec.sign(future, kp.getPrivate());

        assertThrows(LicenseKeyException.class, () -> LicenseKeyCodec.verify(key, kp.getPublic()));
    }

    @Test
    @DisplayName("Les formats fantaisistes sont rejetes sans exception technique")
    void rejectsGarbage() {
        KeyPair kp = keyPair();
        for (String bad : List.of("n'importe quoi", "GYL1.abc", "GYL9.a.b", "")) {
            assertThrows(LicenseKeyException.class, () -> LicenseKeyCodec.verify(bad, kp.getPublic()), bad);
        }
    }

    @Test
    @DisplayName("Expiration absente = licence perpetuelle ; surcharge de quota respectee")
    void perpetualAndQuotaOverride() {
        KeyPair kp = keyPair();
        LicenseClaims claims = LicenseClaims.builder()
                .product(LicenseKeyCodec.PRODUCT).plan("STARTER").customer("Z")
                .maxTrucks(-1)
                .build();

        LicenseClaims back = LicenseKeyCodec.verify(LicenseKeyCodec.sign(claims, kp.getPrivate()), kp.getPublic());

        assertNull(back.getExpiresAt());
        assertEquals(-1, back.effectiveMaxTrucks());
        assertEquals(LicensePlan.STARTER.getMaxUsers(), back.effectiveMaxUsers());
    }

    @Test
    @DisplayName("L'empreinte machine est stable d'un appel a l'autre")
    void machineFingerprintIsStable() {
        assertEquals(MachineFingerprint.current(), MachineFingerprint.current());
    }
}
