package com.gayale.transport.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Encodage / signature / verification des cles de licence.
 *
 * Format : {@code GYL1.<payload>.<signature>}
 *  - {@code GYL1}      : magic + version de format ;
 *  - {@code payload}   : JSON compact des {@link LicenseClaims}, en base64url sans padding ;
 *  - {@code signature} : Ed25519 (RFC 8032) de la chaine {@code "GYL1." + payload}, base64url.
 *
 * Ed25519 est fourni nativement par le JDK 17 (SunEC) : aucune dependance supplementaire.
 * La cle PRIVEE ne quitte jamais le poste de l'editeur ; le produit livre au client ne
 * contient que la cle PUBLIQUE, ce qui rend la falsification d'une licence infaisable
 * meme avec le binaire en main.
 *
 * Espaces et retours a la ligne sont tolerants a la saisie : ils sont retires avant
 * analyse (le client colle souvent la cle depuis un e-mail, qui la replie sur plusieurs
 * lignes). ATTENTION : les tirets ne sont PAS retires — base64url utilise '-' et '_'
 * comme caracteres significatifs, les supprimer corromprait la cle.
 */
public final class LicenseKeyCodec {

    /** Prefixe magique + version du format de cle. */
    public static final String MAGIC = "GYL1";

    /** Identifiant produit attendu dans le payload. */
    public static final String PRODUCT = "GAYALE-TRANSPORT";

    private static final String ALGO = "Ed25519";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private LicenseKeyCodec() {
    }

    // ------------------------------------------------------------------ emission (editeur)

    /** Signe des claims et produit la cle a remettre au client. */
    public static String sign(LicenseClaims claims, PrivateKey privateKey) {
        try {
            if (claims.getProduct() == null) {
                claims.setProduct(PRODUCT);
            }
            String payload = b64(MAPPER.writeValueAsBytes(claims));
            String signingInput = MAGIC + "." + payload;
            Signature sig = Signature.getInstance(ALGO);
            sig.initSign(privateKey);
            sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + b64(sig.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Signature de la licence impossible : " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ verification (produit)

    /**
     * Verifie la signature et renvoie les claims. Ne juge PAS l'expiration ni la machine :
     * c'est le role du LicenseService, pour pouvoir distinguer EXPIRED de INVALID.
     *
     * @throws LicenseKeyException si le format, le produit ou la signature sont invalides.
     */
    public static LicenseClaims verify(String rawKey, PublicKey publicKey) {
        String key = normalize(rawKey);
        String[] parts = key.split("\\.");
        if (parts.length != 3 || !MAGIC.equals(parts[0])) {
            throw new LicenseKeyException(LicenseStatus.INVALID,
                    "Format de cle non reconnu (attendu : " + MAGIC + ".<payload>.<signature>).");
        }
        byte[] signature;
        byte[] payload;
        try {
            payload = unb64(parts[1]);
            signature = unb64(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new LicenseKeyException(LicenseStatus.INVALID, "Cle de licence corrompue (base64 invalide).", e);
        }

        try {
            Signature sig = Signature.getInstance(ALGO);
            sig.initVerify(publicKey);
            sig.update((MAGIC + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!sig.verify(signature)) {
                throw new LicenseKeyException(LicenseStatus.INVALID,
                        "Signature de licence invalide : cette cle n'a pas ete emise par l'editeur.");
            }
        } catch (LicenseKeyException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseKeyException(LicenseStatus.INVALID,
                    "Verification de la signature impossible : " + e.getMessage(), e);
        }

        LicenseClaims claims;
        try {
            claims = MAPPER.readValue(payload, LicenseClaims.class);
        } catch (Exception e) {
            throw new LicenseKeyException(LicenseStatus.INVALID, "Contenu de licence illisible.", e);
        }
        if (!PRODUCT.equalsIgnoreCase(claims.getProduct())) {
            throw new LicenseKeyException(LicenseStatus.INVALID,
                    "Cette cle a ete emise pour un autre produit (" + claims.getProduct() + ").");
        }
        if (claims.resolvedPlan() == null) {
            throw new LicenseKeyException(LicenseStatus.INVALID,
                    "Plan inconnu de cette version du logiciel : " + claims.getPlan()
                            + ". Mettez l'application a jour.");
        }
        return claims;
    }

    // ------------------------------------------------------------------ cles

    /** Charge une cle publique Ed25519 depuis son encodage X.509 en base64. */
    public static PublicKey publicKeyFromBase64(String base64X509) {
        try {
            byte[] der = Base64.getDecoder().decode(compact(base64X509));
            return KeyFactory.getInstance(ALGO).generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Cle publique de licence invalide (attendu : X.509 base64) : "
                    + e.getMessage(), e);
        }
    }

    /** Charge une cle privee Ed25519 depuis son encodage PKCS#8 en base64 (usage editeur). */
    public static PrivateKey privateKeyFromBase64(String base64Pkcs8) {
        try {
            byte[] der = Base64.getDecoder().decode(compact(base64Pkcs8));
            return KeyFactory.getInstance(ALGO).generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Cle privee de licence invalide (attendu : PKCS#8 base64) : "
                    + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ utilitaires

    /**
     * Retire les espaces et retours a la ligne introduits par la mise en forme des e-mails.
     * Ne touche PAS aux '-' ni aux '_' : ce sont des caracteres de l'alphabet base64url.
     */
    public static String normalize(String rawKey) {
        if (rawKey == null) {
            throw new LicenseKeyException(LicenseStatus.MISSING, "Aucune cle de licence fournie.");
        }
        String k = rawKey.replaceAll("\\s", "");
        if (k.isEmpty()) {
            throw new LicenseKeyException(LicenseStatus.MISSING, "Aucune cle de licence fournie.");
        }
        return k;
    }

    /** Affichage : coupe la cle en blocs de 48 caracteres pour la lisibilite. */
    public static String pretty(String key) {
        String k = normalize(key);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < k.length(); i += 48) {
            sb.append(k, i, Math.min(k.length(), i + 48)).append('\n');
        }
        return sb.toString().trim();
    }

    /** 6 derniers caracteres, pour tracer une licence dans les logs sans la divulguer. */
    public static String tail(String key) {
        String k = key == null ? "" : key.replaceAll("\\s", "");
        return k.length() <= 6 ? k : "..." + k.substring(k.length() - 6);
    }

    private static String compact(String s) {
        return s == null ? "" : s.replaceAll("(-----[A-Z ]+-----)|\\s", "");
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] unb64(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
