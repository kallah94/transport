package com.gayale.transport.license;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Empreinte stable du poste sur lequel tourne le backend embarque (livraison Electron dediee).
 * Elle lie une licence a une installation : la cle du client A ne demarre pas chez le client B.
 *
 * Composition : adresses MAC physiques (triees, hors loopback/virtuelles), nom d'hote,
 * OS et architecture -> SHA-256 -> 24 caracteres hexadecimaux en majuscules.
 *
 * Compromis assume : une carte reseau remplacee change l'empreinte. C'est pourquoi le
 * support peut reemettre une cle (procedure « transfert de machine », cf. licence-doc.rmd),
 * et pourquoi les licences ENTERPRISE sont generalement emises SANS liaison machine.
 */
@Slf4j
public final class MachineFingerprint {

    private static volatile String cached;

    private MachineFingerprint() {
    }

    /** Empreinte de la machine courante (calculee une fois, puis mise en cache). */
    public static String current() {
        String v = cached;
        if (v == null) {
            synchronized (MachineFingerprint.class) {
                if (cached == null) {
                    cached = compute();
                }
                v = cached;
            }
        }
        return v;
    }

    private static String compute() {
        StringBuilder raw = new StringBuilder();
        for (String mac : macAddresses()) {
            raw.append(mac).append('|');
        }
        raw.append(hostname()).append('|')
                .append(System.getProperty("os.name", "?")).append('|')
                .append(System.getProperty("os.arch", "?"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                hex.append(String.format("%02X", digest[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            log.warn("Empreinte machine indisponible : {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private static List<String> macAddresses() {
        List<String> macs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (nic.isLoopback() || nic.isVirtual() || !nic.isUp()) {
                    continue;
                }
                byte[] mac = nic.getHardwareAddress();
                if (mac == null || mac.length == 0) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) {
                    sb.append(String.format("%02X", b));
                }
                macs.add(sb.toString());
            }
        } catch (Exception e) {
            log.warn("Lecture des interfaces reseau impossible : {}", e.getMessage());
        }
        Collections.sort(macs);
        return macs;
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String env = System.getenv("COMPUTERNAME");
            return env != null ? env : "unknown-host";
        }
    }
}
