package output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import config.PufferKonfiguration;
import model.AlgorithmusErgebnis;
import model.PlanAktivitaet;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

/** Maschinenlesbare Rechendaten und Eingabe-/Quellpruefsummen eines Java-Laufs. */
public final class ErgebnisExport {
    private ErgebnisExport() {}

    public static void exportiere(
            Path ziel,
            ProjektInstanz instanz,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> ergebnisse,
            PufferKonfiguration c,
            Path projekt,
            Path eventDatei)
            throws IOException {
        Files.createDirectories(ziel);
        StringBuilder aktiv =
                new StringBuilder(
                        "Verfahren;Plan;ID;Welle;Start;Ende;Basisdauer;Puffer;Ereignis;Verbrauch;Ueberhang");
        StringBuilder kenn =
                new StringBuilder(
                        "Verfahren;Plan;Makespan;Pufferbudget;Ereignissumme;Verbrauch;Ueberhang;Puffernutzungsgrad;Ueberhangquote");
        for (int k = 0; k < instanz.getAnzahlRessourcen(); k++) {
            aktiv.append(";R").append(k + 1);
            kenn.append(";AuslastungR").append(k + 1);
        }
        aktiv.append('\n');
        kenn.append('\n');
        for (AlgorithmusErgebnis ergebnis : ergebnisse) {
            schreibePlan(
                    aktiv,
                    kenn,
                    ergebnis.getAlgorithmusName(),
                    "base",
                    ergebnis.getBasisplan(),
                    ergebnis.getBasisbewertung(),
                    instanz);
            schreibePlan(
                    aktiv,
                    kenn,
                    ergebnis.getAlgorithmusName(),
                    "event",
                    ergebnis.getEreignisplan(),
                    ergebnis.getEreignisbewertung(),
                    instanz);
        }
        Files.writeString(ziel.resolve("aktivitaeten.csv"), aktiv, StandardCharsets.UTF_8);
        Files.writeString(ziel.resolve("kennzahlen.csv"), kenn, StandardCharsets.UTF_8);
        StringBuilder json =
                new StringBuilder("{\n  \"createdAt\": ")
                        .append(quote(Instant.now().toString()))
                        .append(",\n  \"runtime\": ")
                        .append(quote(System.getProperty("java.version")))
                        .append(",\n  \"instance\": ")
                        .append(quote(projekt.toString()))
                        .append(",\n  \"capacities\": ")
                        .append(java.util.Arrays.toString(instanz.getKapazitaeten()))
                        .append(",\n  \"parameters\": {\"fixed\": ")
                        .append(c.getFesterPuffer())
                        .append(", \"proportional\": ")
                        .append(c.getProportionalerFaktor())
                        .append(", \"depthStartReserve\": ")
                        .append(c.getTiefenabhaengigeStartReserve())
                        .append(", \"depthEndReserve\": ")
                        .append(c.getTiefenabhaengigeEndReserve())
                        .append("},\n  \"events\": [");
        for (int j = 0; j < ereignisse.size(); j++) {
            ProjektEreignis e = ereignisse.get(j);
            if (j > 0) json.append(',');
            json.append("{\"id\":")
                    .append(e.getAktivitaetId())
                    .append(",\"delay\":")
                    .append(e.getVerzoegerung())
                    .append('}');
        }
        json.append("],\n  \"sha256\": {\n    \"project\": ")
                .append(quote(hash(projekt)))
                .append(",\n    \"events\": ")
                .append(quote(hash(eventDatei)))
                .append(",\n    \"configuration\": ")
                .append(quote(hash(Path.of("config.properties"))))
                .append(",\n    \"sourceTree\": ")
                .append(quote(hashQuellen(Path.of("src"))))
                .append("\n  }\n}\n");
        Files.writeString(ziel.resolve("lauf.json"), json, StandardCharsets.UTF_8);
    }

    private static void schreibePlan(
            StringBuilder aktiv,
            StringBuilder kenn,
            String name,
            String sicht,
            Projektplan plan,
            PlanBewertung b,
            ProjektInstanz instanz) {
        Map<Integer, Integer> wellen = new LinkedHashMap<>();
        for (PlanAktivitaet a : plan.getAktivitaetenInPlanreihenfolge()) {
            int w = wellen.computeIfAbsent(a.getStartzeit(), s -> wellen.size() + 1);
            aktiv.append(name)
                    .append(';')
                    .append(sicht)
                    .append(';')
                    .append(a.getId())
                    .append(';')
                    .append(w)
                    .append(';')
                    .append(a.getStartzeit())
                    .append(';')
                    .append(a.getEndzeit())
                    .append(';')
                    .append(a.getBasisDauer())
                    .append(';')
                    .append(a.getPuffer())
                    .append(';')
                    .append(a.getVerzoegerung())
                    .append(';')
                    .append(a.getVerzoegerungImPuffer())
                    .append(';')
                    .append(a.getZusatzverzoegerungNachPuffer());
            for (int k = 0; k < instanz.getAnzahlRessourcen(); k++)
                aktiv.append(';').append(a.getRessource(k));
            aktiv.append('\n');
        }
        kenn.append(name)
                .append(';')
                .append(sicht)
                .append(';')
                .append(b.getMakespan())
                .append(';')
                .append(b.getGesamterPuffer())
                .append(';')
                .append(b.getGesamteVerzoegerung())
                .append(';')
                .append(b.getVerbrauchterPuffer())
                .append(';')
                .append(b.getEreignisUeberhang())
                .append(';')
                .append(zahl(b.getPufferNutzungsgrad()))
                .append(';')
                .append(zahl(b.getEreignisUeberhangquote()));
        for (int k = 0; k < instanz.getAnzahlRessourcen(); k++)
            kenn.append(';').append(b.getAuslastung(k));
        kenn.append('\n');
    }

    private static String zahl(double wert) {
        return Double.isNaN(wert) ? "n/a" : Double.toString(wert);
    }

    private static String quote(String text) {
        return "\""
                + text.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                + "\"";
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hash(Path p) throws IOException {
        return HexFormat.of().formatHex(digest().digest(Files.readAllBytes(p)));
    }

    private static String hashQuellen(Path ordner) throws IOException {
        MessageDigest md = digest();
        try (var pfade = Files.walk(ordner)) {
            for (Path p :
                    pfade.filter(Files::isRegularFile)
                            .filter(f -> f.toString().endsWith(".java"))
                            .sorted()
                            .toList()) {
                md.update(
                        ordner.relativize(p)
                                .toString()
                                .replace('\\', '/')
                                .getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(Files.readAllBytes(p));
                md.update((byte) 0);
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
