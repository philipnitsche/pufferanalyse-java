package output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import model.AlgorithmusErgebnis;
import model.EreignisStudie;
import model.PlanAktivitaet;
import model.PlanBewertung;
import model.ProjektInstanz;
import planning.EreignisGenerator;

/** Einzelwerte bleiben erhalten; die Auswertung fasst die Szenarien je Verfahren zusammen. */
public final class StudienExport {
    private StudienExport() {}

    public static void exportiere(Path ziel, ProjektInstanz instanz, EreignisStudie studie)
            throws IOException {
        Files.createDirectories(ziel);
        StringBuilder events = new StringBuilder("Szenario;Seed;AktivitaetID;Verzoegerung\n");
        StringBuilder aktiv =
                new StringBuilder(
                        "Szenario;Seed;Verfahren;ID;Welle;Start;Ende;Basisdauer;Puffer;Ereignis;Verbrauch;Ueberhang");
        StringBuilder kenn =
                new StringBuilder(
                        "Szenario;Seed;Verfahren;BasisMakespan;EreignisMakespan;Delta;Basisaufschlag;Ereignisaufschlag;VermiedeneAbweichung;Pufferbudget;Ereignissumme;Verbrauch;Ueberhang;Nutzungsgrad;Aufnahmequote;VproP");
        for (int k = 1; k <= instanz.getAnzahlRessourcen(); k++) {
            aktiv.append(";R").append(k);
            kenn.append(";AuslastungR").append(k);
        }
        aktiv.append('\n');
        kenn.append('\n');
        for (var s : studie.szenarien()) {
            for (var e : s.ereignisse())
                events.append(s.nummer())
                        .append(';')
                        .append(s.seed())
                        .append(';')
                        .append(e.getAktivitaetId())
                        .append(';')
                        .append(e.getVerzoegerung())
                        .append('\n');
            var standard = s.ergebnisse().get(0);
            int delta0 = delta(standard);
            for (var e : s.ergebnisse()) {
                PlanBewertung b = e.getBasisbewertung(), v = e.getEreignisbewertung();
                int vermieden = delta0 - delta(e);
                kenn.append(s.nummer())
                        .append(';')
                        .append(s.seed())
                        .append(';')
                        .append(e.getAlgorithmusName())
                        .append(';')
                        .append(b.getMakespan())
                        .append(';')
                        .append(v.getMakespan())
                        .append(';')
                        .append(delta(e))
                        .append(';')
                        .append(b.getMakespan() - standard.getBasisbewertung().getMakespan())
                        .append(';')
                        .append(v.getMakespan() - standard.getEreignisbewertung().getMakespan())
                        .append(';')
                        .append(vermieden)
                        .append(';')
                        .append(b.getGesamterPuffer())
                        .append(';')
                        .append(v.getGesamteVerzoegerung())
                        .append(';')
                        .append(v.getVerbrauchterPuffer())
                        .append(';')
                        .append(v.getEreignisUeberhang())
                        .append(';')
                        .append(zahl(v.getPufferNutzungsgrad()))
                        .append(';')
                        .append(zahl(quote(v.getVerbrauchterPuffer(), v.getGesamteVerzoegerung())))
                        .append(';')
                        .append(zahl(quote(vermieden, b.getGesamterPuffer())));
                for (int k = 0; k < instanz.getAnzahlRessourcen(); k++)
                    kenn.append(';').append(v.getAuslastung(k));
                kenn.append('\n');
                Map<Integer, Integer> wellen = new LinkedHashMap<>();
                for (PlanAktivitaet a : e.getEreignisplan().getAktivitaetenInPlanreihenfolge()) {
                    int w = wellen.computeIfAbsent(a.getStartzeit(), x -> wellen.size() + 1);
                    aktiv.append(s.nummer())
                            .append(';')
                            .append(s.seed())
                            .append(';')
                            .append(e.getAlgorithmusName())
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
            }
        }
        schreibe(ziel, "ereignisse.csv", events);
        schreibe(ziel, "aktivitaeten.csv", aktiv);
        schreibe(ziel, "kennzahlen.csv", kenn);
        String zusammenfassung = zusammenfassung(studie);
        schreibe(ziel, "zusammenfassung.csv", zusammenfassung);
        schreibe(ziel, "auswertung.md", lesbarerBericht(studie, zusammenfassung));
        var c = studie.konfiguration();
        String manifest =
                "{\n  \"generator\": \""
                        + EreignisGenerator.VERSION
                        + "\",\n"
                        + "  \"scenarioCount\": "
                        + c.anzahl()
                        + ",\n  \"masterSeed\": "
                        + c.startSeed()
                        + ",\n"
                        + "  \"seedRule\": \"java.util.Random(masterSeed).nextLong() per scenario\",\n"
                        + "  \"eventCount\": "
                        + c.betroffeneAktivitaeten()
                        + ",\n  \"minDelay\": "
                        + c.minimaleZusatzdauer()
                        + ",\n"
                        + "  \"maxDelay\": "
                        + c.maximaleZusatzdauer()
                        + ",\n  \"includesDetailExample\": false,\n"
                        + "  \"p90Method\": \"nearest rank: ceil(0.9*n)\",\n"
                        + "  \"meanRatios\": \"arithmetic mean of per-scenario ratios\",\n"
                        + "  \"sha256\": {\n    \"runManifest\": \""
                        + ErgebnisExport.hash(ziel.getParent().resolve("lauf.json"))
                        + "\",\n"
                        + "    \"events\": \""
                        + ErgebnisExport.hash(ziel.resolve("ereignisse.csv"))
                        + "\",\n"
                        + "    \"activities\": \""
                        + ErgebnisExport.hash(ziel.resolve("aktivitaeten.csv"))
                        + "\",\n"
                        + "    \"metrics\": \""
                        + ErgebnisExport.hash(ziel.resolve("kennzahlen.csv"))
                        + "\",\n"
                        + "    \"summary\": \""
                        + ErgebnisExport.hash(ziel.resolve("zusammenfassung.csv"))
                        + "\"\n  }\n}\n";
        schreibe(ziel, "studie.json", manifest);
    }

    private static String zusammenfassung(EreignisStudie studie) {
        StringBuilder out =
                new StringBuilder(
                        "Verfahren;Anzahl;BasisMakespan;Pufferbudget;MittelDelta;MedianDelta;P90Delta;MaxDelta;OhneAbweichung;AnteilOhneAbweichung;MittelEreignisMakespan;MittelVerbrauch;MittlererNutzungsgrad;MittlereAufnahmequote;MittelVproP\n");
        int n = studie.szenarien().size();
        for (int k = 0; k < 4; k++) {
            int[] deltas = new int[n];
            double summeEnde = 0, summeU = 0, summeAufnahme = 0, summeV = 0;
            int nullen = 0;
            AlgorithmusErgebnis erstes = studie.szenarien().get(0).ergebnisse().get(k);
            int p = erstes.getBasisbewertung().getGesamterPuffer();
            for (int j = 0; j < n; j++) {
                var s = studie.szenarien().get(j);
                var e = s.ergebnisse().get(k);
                var v = e.getEreignisbewertung();
                deltas[j] = delta(e);
                if (deltas[j] == 0) nullen++;
                summeEnde += v.getMakespan();
                summeU += v.getVerbrauchterPuffer();
                summeAufnahme += quote(v.getVerbrauchterPuffer(), v.getGesamteVerzoegerung());
                summeV += delta(s.ergebnisse().get(0)) - deltas[j];
            }
            Arrays.sort(deltas);
            double median =
                    n % 2 == 1 ? deltas[n / 2] : ((double) deltas[n / 2 - 1] + deltas[n / 2]) / 2;
            out.append(erstes.getAlgorithmusName())
                    .append(';')
                    .append(n)
                    .append(';')
                    .append(erstes.getBasisbewertung().getMakespan())
                    .append(';')
                    .append(p)
                    .append(';')
                    .append(Arrays.stream(deltas).average().orElseThrow())
                    .append(';')
                    .append(median)
                    .append(';')
                    .append(deltas[(int) Math.ceil(.9 * n) - 1])
                    .append(';')
                    .append(deltas[n - 1])
                    .append(';')
                    .append(nullen)
                    .append(';')
                    .append((double) nullen / n)
                    .append(';')
                    .append(summeEnde / n)
                    .append(';')
                    .append(summeU / n)
                    .append(';')
                    .append(zahl(quote(summeU / n, p)))
                    .append(';')
                    .append(summeAufnahme / n)
                    .append(';')
                    .append(zahl(quote(summeV / n, p)))
                    .append('\n');
        }
        return out.toString();
    }

    private static int delta(AlgorithmusErgebnis e) {
        return e.getEreignisbewertung().getMakespan() - e.getBasisbewertung().getMakespan();
    }

    private static String lesbarerBericht(EreignisStudie studie, String zusammenfassung) {
        var c = studie.konfiguration();
        StringBuilder out =
                new StringBuilder("# Auswertung der Ereignisserie\n\n")
                        .append(c.anzahl())
                        .append(" erzeugte Szenarien; Startseed ")
                        .append(c.startSeed())
                        .append(". Je Szenario ")
                        .append(c.betroffeneAktivitaeten())
                        .append(" betroffene Aktivitaeten mit ")
                        .append(c.minimaleZusatzdauer())
                        .append(" bis ")
                        .append(c.maximaleZusatzdauer())
                        .append(" ZE Zusatzdauer.\n\n")
                        .append(
                                "Das gespeicherte Detailbeispiel wird nicht mitgezaehlt. Alle Verfahren erhalten je Szenario dieselben Ereignisse.\n\n")
                        .append(
                                "| Verfahren | Budget ZE | Mittlere Abweichung ZE | Median ZE | P90 ZE | Maximum ZE | Ohne Abweichung | Mittlere Nutzung |\n")
                        .append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        for (String zeile : zusammenfassung.lines().skip(1).toList()) {
            String[] v = zeile.split(";", -1);
            out.append("| ")
                    .append(v[0])
                    .append(" | ")
                    .append(v[3])
                    .append(" | ")
                    .append(dezimal(v[4]))
                    .append(" | ")
                    .append(dezimal(v[5]))
                    .append(" | ")
                    .append(v[6])
                    .append(" | ")
                    .append(v[7])
                    .append(" | ")
                    .append(v[8])
                    .append('/')
                    .append(v[1])
                    .append(" | ")
                    .append(
                            v[12].equals("n/a")
                                    ? "n/a"
                                    : dezimal(Double.toString(Double.parseDouble(v[12]) * 100))
                                            + " %")
                    .append(" |\n");
        }
        return out.append(
                        "\nAbweichung bedeutet Ereignisendtermin minus eigenen Basisendtermin. Median und P90 beschreiben die sortierten Abweichungen; P90 ist der Wert am naechsten Rang ceil(0.9*n). Nutzung ist lokal verbrauchter Puffer geteilt durch eingeplanten Puffer. n/a bedeutet fehlenden Nenner.\n\n")
                .append(
                        "Die absoluten Ereigniszeiten und weitere Quoten stehen in [zusammenfassung.csv](zusammenfassung.csv). [kennzahlen.csv](kennzahlen.csv) enthaelt alle Einzelbewertungen, [ereignisse.csv](ereignisse.csv) alle gezogenen Ereignisse.\n\n")
                .append(
                        "Die Budgets unterscheiden sich absichtlich. Quoten ersetzen weder absolute Endtermine noch einen Verteilungsvergleich bei gleichem Budget. Die Aussagen gelten fuer diese Instanz und diese Ereignisregeln.\n")
                .toString();
    }

    private static String dezimal(String x) {
        return String.format(java.util.Locale.GERMANY, "%.2f", Double.parseDouble(x));
    }

    private static double quote(double a, double b) {
        return b == 0 ? Double.NaN : a / b;
    }

    private static String zahl(double x) {
        return Double.isNaN(x) ? "n/a" : Double.toString(x);
    }

    private static void schreibe(Path ziel, String name, CharSequence inhalt) throws IOException {
        Files.writeString(ziel.resolve(name), inhalt, StandardCharsets.UTF_8);
    }
}
