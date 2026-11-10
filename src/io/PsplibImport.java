package io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Datensatz;

/** Liest Single-Mode-PSPLIB mit erneuerbaren Ressourcen; keine Modusauswahl. */
final class PsplibImport {
    record Ergebnis(List<Datensatz> aktivitaeten, int[] kapazitaeten) {}

    private PsplibImport() {}

    static Ergebnis lese(Path pfad) throws IOException {
        Map<Integer, int[]> nachfolger = new LinkedHashMap<>();
        Map<Integer, int[]> werte = new LinkedHashMap<>();
        int[] kapazitaeten = null;
        String abschnitt = "";
        List<String> zeilen = Files.readAllLines(pfad, StandardCharsets.UTF_8);
        for (int z = 0; z < zeilen.size(); z++) {
            String zeile = zeilen.get(z).trim();
            if (zeile.startsWith("- nonrenewable") || zeile.startsWith("- doubly constrained")) {
                String anzahl = zeile.substring(zeile.indexOf(':') + 1).trim().split("\\s+")[0];
                if (!anzahl.equals("0"))
                    throw new IllegalArgumentException(
                            pfad + ": Nur erneuerbare Ressourcen werden unterstuetzt");
            }
            if (zeile.equals("PRECEDENCE RELATIONS:")
                    || zeile.equals("REQUESTS/DURATIONS:")
                    || zeile.equals("RESOURCEAVAILABILITIES:")) {
                abschnitt = zeile;
                continue;
            }
            if (zeile.isEmpty() || !Character.isDigit(zeile.charAt(0))) continue;
            try {
                int[] zahlen =
                        Arrays.stream(zeile.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                if (abschnitt.equals("PRECEDENCE RELATIONS:")) {
                    if (zahlen.length < 3 || zahlen[1] != 1 || zahlen[2] != zahlen.length - 3)
                        throw new IllegalArgumentException("Single-Mode-Nachfolgerzeile ungueltig");
                    if (nachfolger.putIfAbsent(
                                    zahlen[0], Arrays.copyOfRange(zahlen, 3, zahlen.length))
                            != null) throw new IllegalArgumentException("Doppelte Aktivitaets-ID");
                } else if (abschnitt.equals("REQUESTS/DURATIONS:")) {
                    if (zahlen.length < 4 || zahlen[1] != 1)
                        throw new IllegalArgumentException("Single-Mode-Dauerzeile ungueltig");
                    if (werte.putIfAbsent(zahlen[0], Arrays.copyOfRange(zahlen, 2, zahlen.length))
                            != null) throw new IllegalArgumentException("Doppelte Dauerzeile");
                } else if (abschnitt.equals("RESOURCEAVAILABILITIES:")) {
                    if (kapazitaeten != null)
                        throw new IllegalArgumentException("Mehrere Kapazitaetszeilen");
                    kapazitaeten = zahlen;
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        pfad + ": Zeile " + (z + 1) + ": " + e.getMessage(), e);
            }
        }
        if (kapazitaeten == null || !nachfolger.keySet().equals(werte.keySet()))
            throw new IllegalArgumentException(pfad + ": Unvollstaendige PSPLIB-Struktur");
        int n = kapazitaeten.length;
        for (var eintrag : werte.entrySet()) {
            int[] row = eintrag.getValue();
            if (row.length != n + 1 || row[0] < 0)
                throw new IllegalArgumentException(
                        pfad + ": Ungueltige Ressourcenzahl oder Dauer bei ID " + eintrag.getKey());
            for (int ziel : nachfolger.get(eintrag.getKey()))
                if (!werte.containsKey(ziel))
                    throw new IllegalArgumentException(pfad + ": Unbekannter Nachfolger " + ziel);
        }
        List<Integer> dummies = werte.keySet().stream().filter(i -> werte.get(i)[0] == 0).toList();
        if (dummies.size() != 2)
            throw new IllegalArgumentException(pfad + ": Genau zwei Rand-Dummies erwartet");
        int start = dummies.get(0), ende = dummies.get(1);
        if (nachfolger.get(start).length == 0
                || nachfolger.get(ende).length != 0
                || nachfolger.values().stream()
                        .anyMatch(ids -> Arrays.stream(ids).anyMatch(i -> i == start)))
            throw new IllegalArgumentException(pfad + ": Ungueltige Rand-Dummies");
        for (int id : dummies)
            if (Arrays.stream(werte.get(id)).anyMatch(v -> v != 0))
                throw new IllegalArgumentException(pfad + ": Dummy hat Ressourcenbedarf");
        List<Datensatz> result = new ArrayList<>();
        for (var eintrag : werte.entrySet()) {
            int id = eintrag.getKey();
            int[] row = eintrag.getValue();
            if (row[0] == 0) continue;
            List<Integer> kanten =
                    Arrays.stream(nachfolger.get(id)).filter(i -> i != ende).boxed().toList();
            Datensatz d =
                    new Datensatz(
                            id, kanten, List.of(), row[0], Arrays.copyOfRange(row, 1, row.length));
            d.setOriginalNachfolgerAnzahl(nachfolger.get(id).length);
            result.add(d);
        }
        return new Ergebnis(result, kapazitaeten);
    }
}
