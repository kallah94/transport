package com.gayale.transport.license.tool;

import com.gayale.transport.license.LicenseClaims;
import com.gayale.transport.license.LicenseFeature;
import com.gayale.transport.license.LicenseKeyCodec;
import com.gayale.transport.license.LicensePlan;

import java.io.PrintStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * OUTIL EDITEUR — generation des paires de cles et emission des licences clients.
 *
 * Cet outil ne contient aucun secret : la cle privee lui est fournie a l'execution
 * (variable d'environnement {@code LICENSE_PRIVATE_KEY} ou option {@code --private-key}).
 * Il est livre dans le JAR sans risque, puisqu'il ne peut rien signer sans cette cle.
 *
 * <h2>1. Generer la paire de cles (UNE FOIS, a conserver precieusement)</h2>
 * <pre>
 *   java -cp target/classes com.gayale.transport.license.tool.LicenseKeyTool keygen
 * </pre>
 * Sortie : la cle PUBLIQUE va dans {@code LICENSE_PUBLIC_KEY} des livraisons clients ;
 * la cle PRIVEE reste hors depot (coffre-fort / gestionnaire de secrets).
 *
 * <h2>2. Emettre une licence client</h2>
 * <pre>
 *   set LICENSE_PRIVATE_KEY=&lt;base64 PKCS#8&gt;
 *   java -cp target/classes com.gayale.transport.license.tool.LicenseKeyTool issue ^
 *        --customer "Client A SARL" --key clienta --plan PRO --months 12 ^
 *        --machine 4F2A9C1B77E0 --email contact@clienta.sn
 * </pre>
 *
 * <h2>3. Inspecter une cle recue du support</h2>
 * <pre>
 *   java -cp target/classes com.gayale.transport.license.tool.LicenseKeyTool inspect --key GYL1....
 * </pre>
 *
 * <h2>Options de « issue »</h2>
 * <ul>
 *   <li>{@code --customer}   raison sociale (obligatoire)</li>
 *   <li>{@code --key}        cle technique du client (= cle de tenant / de packaging)</li>
 *   <li>{@code --plan}       STARTER | PRO | ENTERPRISE (obligatoire)</li>
 *   <li>{@code --months}     duree en mois ; {@code --days} ; ou {@code --perpetual}</li>
 *   <li>{@code --machine}    empreinte du poste client (licence liee) ; omis = licence flottante</li>
 *   <li>{@code --email}      contact du client</li>
 *   <li>{@code --features}   add-ons en plus du plan, separes par des virgules</li>
 *   <li>{@code --max-users}, {@code --max-trucks}, {@code --max-tickets} : surcharges de quota (-1 = illimite)</li>
 *   <li>{@code --note}       reference de commande / remarque contractuelle</li>
 * </ul>
 */
public final class LicenseKeyTool {

    private static final PrintStream OUT = System.out;

    private LicenseKeyTool() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        Map<String, String> opts = parse(args);
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "keygen" -> keygen();
                case "issue" -> issue(opts);
                case "inspect" -> inspect(opts);
                default -> {
                    usage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("ERREUR : " + e.getMessage());
            System.exit(2);
        }
    }

    // ------------------------------------------------------------------ keygen

    private static void keygen() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        OUT.println("=== PAIRE DE CLES DE LICENCE (Ed25519) ===");
        OUT.println();
        OUT.println("# A publier dans chaque livraison client (application.yml / app.env) :");
        OUT.println("LICENSE_PUBLIC_KEY=" + pub);
        OUT.println();
        OUT.println("# A CONSERVER SECRETE — ne jamais committer, ne jamais livrer :");
        OUT.println("LICENSE_PRIVATE_KEY=" + priv);
        OUT.println();
        OUT.println("Perdre la cle privee = ne plus pouvoir emettre de licences pour les");
        OUT.println("installations deja livrees. Sauvegardez-la dans un coffre-fort.");
    }

    // ------------------------------------------------------------------ issue

    private static void issue(Map<String, String> opts) {
        String privateKeyB64 = value(opts, "private-key", System.getenv("LICENSE_PRIVATE_KEY"));
        require(privateKeyB64, "Cle privee absente : definissez LICENSE_PRIVATE_KEY ou --private-key.");
        PrivateKey privateKey = LicenseKeyCodec.privateKeyFromBase64(privateKeyB64);

        String customer = value(opts, "customer", null);
        require(customer, "--customer est obligatoire.");

        String planRaw = value(opts, "plan", null);
        require(planRaw, "--plan est obligatoire (STARTER | PRO | ENTERPRISE).");
        LicensePlan plan = LicensePlan.from(planRaw);
        if (plan == null) {
            throw new IllegalArgumentException("Plan inconnu : " + planRaw
                    + ". Valeurs possibles : " + Arrays.toString(LicensePlan.values()));
        }
        if (plan == LicensePlan.TRIAL) {
            throw new IllegalArgumentException(
                    "Le plan TRIAL est genere automatiquement par l'application et ne s'emet pas.");
        }

        LocalDate issued = LocalDate.now();
        LocalDate expires;
        if (opts.containsKey("perpetual")) {
            expires = null;
        } else if (opts.containsKey("days")) {
            expires = issued.plusDays(Long.parseLong(opts.get("days")));
        } else {
            expires = issued.plusMonths(Long.parseLong(value(opts, "months", "12")));
        }

        Set<String> extras = new LinkedHashSet<>();
        String features = value(opts, "features", null);
        if (features != null && !features.isBlank()) {
            for (String f : features.split(",")) {
                String name = f.trim().toUpperCase(Locale.ROOT);
                if (name.isEmpty()) {
                    continue;
                }
                // Echoue tot si l'add-on n'existe pas, plutot que d'emettre une cle inutile.
                LicenseFeature.valueOf(name);
                extras.add(name);
            }
        }

        LicenseClaims claims = LicenseClaims.builder()
                .version(1)
                .product(LicenseKeyCodec.PRODUCT)
                .licenseId(value(opts, "id", "LIC-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase(Locale.ROOT)))
                .customer(customer)
                .customerKey(value(opts, "key", null))
                .contactEmail(value(opts, "email", null))
                .plan(plan.name())
                .issuedAt(issued)
                .expiresAt(expires)
                .machineId(value(opts, "machine", null))
                .maxUsers(intOrNull(opts, "max-users"))
                .maxTrucks(intOrNull(opts, "max-trucks"))
                .maxTicketsPerMonth(intOrNull(opts, "max-tickets"))
                .extraFeatures(extras.isEmpty() ? null : extras)
                .note(value(opts, "note", null))
                .build();

        String key = LicenseKeyCodec.sign(claims, privateKey);

        OUT.println("=== LICENCE EMISE ===");
        OUT.println("Client       : " + claims.getCustomer());
        OUT.println("Cle client   : " + nvl(claims.getCustomerKey()));
        OUT.println("Identifiant  : " + claims.getLicenseId());
        OUT.println("Plan         : " + plan.name() + " (" + plan.getLabel() + ")");
        OUT.println("Emise le     : " + claims.getIssuedAt());
        OUT.println("Expire le    : " + (expires == null ? "jamais (perpetuelle)" : expires));
        OUT.println("Machine      : " + (claims.getMachineId() == null
                ? "non liee (flottante)" : claims.getMachineId()));
        OUT.println("Quotas       : users=" + fmt(claims.effectiveMaxUsers())
                + ", camions=" + fmt(claims.effectiveMaxTrucks())
                + ", tickets/mois=" + fmt(claims.effectiveMaxTicketsPerMonth()));
        OUT.println("Add-ons      : " + (extras.isEmpty() ? "aucun" : String.join(", ", extras)));
        OUT.println();
        OUT.println("--- CLE A TRANSMETTRE AU CLIENT ---");
        OUT.println(LicenseKeyCodec.pretty(key));
        OUT.println("-----------------------------------");
    }

    // ------------------------------------------------------------------ inspect

    private static void inspect(Map<String, String> opts) {
        String publicKeyB64 = value(opts, "public-key", System.getenv("LICENSE_PUBLIC_KEY"));
        require(publicKeyB64, "Cle publique absente : definissez LICENSE_PUBLIC_KEY ou --public-key.");
        String key = value(opts, "key", null);
        require(key, "--key est obligatoire (la cle a inspecter).");

        PublicKey publicKey = LicenseKeyCodec.publicKeyFromBase64(publicKeyB64);
        LicenseClaims claims = LicenseKeyCodec.verify(key, publicKey);

        OUT.println("Signature    : VALIDE");
        OUT.println("Identifiant  : " + claims.getLicenseId());
        OUT.println("Client       : " + claims.getCustomer() + " (" + nvl(claims.getCustomerKey()) + ")");
        OUT.println("Contact      : " + nvl(claims.getContactEmail()));
        OUT.println("Plan         : " + claims.getPlan());
        OUT.println("Emise le     : " + claims.getIssuedAt());
        OUT.println("Expire le    : " + (claims.getExpiresAt() == null ? "jamais" : claims.getExpiresAt()));
        OUT.println("Machine      : " + (claims.getMachineId() == null ? "non liee" : claims.getMachineId()));
        OUT.println("Quotas       : users=" + fmt(claims.effectiveMaxUsers())
                + ", camions=" + fmt(claims.effectiveMaxTrucks())
                + ", tickets/mois=" + fmt(claims.effectiveMaxTicketsPerMonth()));
        OUT.println("Modules      : " + claims.resolvedFeatures());
        OUT.println("Note         : " + nvl(claims.getNote()));
    }

    // ------------------------------------------------------------------ utilitaires

    private static void usage() {
        OUT.println("Outil de licence Gayale Transport");
        OUT.println();
        OUT.println("  keygen                            genere une paire de cles Ed25519");
        OUT.println("  issue   --customer .. --plan ..   emet une cle de licence client");
        OUT.println("  inspect --key GYL1....            verifie et decode une cle existante");
        OUT.println();
        OUT.println("Voir la javadoc de LicenseKeyTool ou .docs/licence-doc.rmd pour le detail.");
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                continue;
            }
            String name = args[i].substring(2);
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                opts.put(name, args[++i]);
            } else {
                opts.put(name, "true");
            }
        }
        return opts;
    }

    private static String value(Map<String, String> opts, String name, String fallback) {
        String v = opts.get(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static Integer intOrNull(Map<String, String> opts, String name) {
        String v = opts.get(name);
        return (v == null || v.isBlank()) ? null : Integer.valueOf(v.trim());
    }

    private static void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String fmt(int quota) {
        return quota < 0 ? "illimite" : String.valueOf(quota);
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
