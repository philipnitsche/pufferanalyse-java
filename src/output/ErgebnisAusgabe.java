package output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import logging.Logger;
import model.AlgorithmusErgebnis;
import model.PlanAktivitaet;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

/** Gibt Planergebnisse auf PROGRAM-Level aus und erstellt den Gantt-Bericht. */
public final class ErgebnisAusgabe {
    private static final String PROZENT_FORMAT = "%.2f%%";

    private ErgebnisAusgabe() {}

    public static void gibErgebnisAus(
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> algorithmusErgebnisse) {
        gibErgebnisAus(projektInstanz, ereignisse, algorithmusErgebnisse, null);
    }

    /** Mit waehlbarem Berichtsordner; null verwendet den Standardordner. */
    public static Path gibErgebnisAus(
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> algorithmusErgebnisse,
            Path reportOrdner) {
        Logger.programSection("Programmergebnis");
        Logger.program("Aktivitaeten: " + projektInstanz.getAktivitaeten().size());
        for (int k = 0; k < projektInstanz.getAnzahlRessourcen(); k++)
            Logger.program("Kapazitaet R" + (k + 1) + "=" + projektInstanz.getKapazitaet(k));
        Logger.program("Projekt-Ereignisse:");
        if (ereignisse.isEmpty()) {
            Logger.program("- keine");
        } else {
            for (ProjektEreignis ereignis : ereignisse) {
                Logger.program(
                        "- Projekt-Ereignis auf Aktivitaet "
                                + ereignis.getAktivitaetId()
                                + " +"
                                + ereignis.getVerzoegerung()
                                + " ZE");
            }
        }

        Logger.programSection("Bewertungsmetriken");
        Logger.program(
                "Stoerungsrobustheit pro Puffer-ZE = (vermiedene zusaetzliche Stoerungsverzoegerung gegenueber dem Standard) / verplanter Gesamtpuffer.");
        Logger.program(
                "Pufferbudgets sind absichtlich verfahrensspezifisch und duerfen voneinander abweichen.");
        Logger.program(
                "Die Stoerungsrobustheit normalisiert nur den Schutz gegen zusaetzliche Ereigniswirkung; sie bewertet nicht die gesamte Projektdauer.");
        Logger.program(
                "Basis- und Ereignis-Makespan-Aufschlag vergleichen die absolute Projektdauer direkt mit dem jeweiligen Standardplan.");
        Logger.program(
                "Reservierte Ressourcenauslastung = Summe(Bedarf * effektive Dauer) / (Kapazitaet * Makespan); enthaltene Pufferzeit zaehlt als Belegung.");
        Logger.program(
                "Planungslogik: prioritaetsregelbasierte wellengebundene Planung; die naechste Welle startet nach dem spaetesten Ende der aktuellen Welle.");
        Logger.program(
                "Vermiedene zusaetzliche Stoerungsverzoegerung = Delta Standard - Delta Verfahren, mit Delta = Makespan(mit Ereignis) - Makespan(ohne Ereignis).");
        Logger.program(
                "Relative Projektverlaengerung durch Ereignisse = (Makespan(mit Ereignis) - Makespan(ohne Ereignis)) / Makespan(ohne Ereignis).");
        Logger.program("Puffer-Nutzungsgrad = verbrauchter Puffer / verplanter Puffer.");
        Logger.program(
                "Ereignis-Ueberhangquote = Ereignis-Ueberhang / gesamte Ereignisverzoegerung.");
        Logger.program(
                "Interpretation: Eine hohe Stoerungsrobustheit bedeutet nur einen hohen Schutzbeitrag je Puffer-ZE; fuer die Gesamtbewertung sind die Makespan-Aufschlaege mitzulesen.");
        Logger.program(
                "Interpretation: 'n/a' bedeutet, dass fuer diese Kennzahl kein sinnvoller Nenner vorhanden ist, also z. B. kein Puffer oder kein Ereignis.");
        Logger.program(
                "Vergleichsbloecke stellen immer Basis- und Ereignisplan desselben Verfahrens gegenueber; "
                        + "nur die Kennzahlen zur Standard-Referenz ziehen das Standardverfahren zusaetzlich heran.");

        StandardReferenz standardReferenz = StandardReferenz.aus(algorithmusErgebnisse);

        for (AlgorithmusErgebnis algorithmusErgebnis : algorithmusErgebnisse) {
            Logger.programSection("Verfahren: " + algorithmusErgebnis.getAlgorithmusName());
            gibPlanAus(
                    algorithmusErgebnis.getAlgorithmusName(),
                    algorithmusErgebnis.getBasisplan(),
                    algorithmusErgebnis.getBasisbewertung(),
                    "ohne Ereignisse");
            gibPlanAus(
                    algorithmusErgebnis.getAlgorithmusName(),
                    algorithmusErgebnis.getEreignisplan(),
                    algorithmusErgebnis.getEreignisbewertung(),
                    "mit Ereignissen");

            Logger.program("Vergleich fuer " + algorithmusErgebnis.getAlgorithmusName() + ":");
            Logger.program(
                    "- Basis-Makespan-Aufschlag gegenueber Standard: "
                            + standardReferenz.basisMakespanAufschlag(algorithmusErgebnis));
            Logger.program(
                    "- Ereignis-Makespan-Aufschlag gegenueber Standard: "
                            + standardReferenz.ereignisMakespanAufschlag(algorithmusErgebnis));
            Logger.program(
                    "- Makespan-Differenz: "
                            + (algorithmusErgebnis.getEreignisbewertung().getMakespan()
                                    - algorithmusErgebnis.getBasisbewertung().getMakespan()));
            Logger.program(
                    "- Relative Projektverlaengerung durch Ereignisse: "
                            + formatiereQuote(
                                    berechneRelativeProjektverlaengerung(algorithmusErgebnis)));
            Logger.program(
                    "- Zusaetzliche Gesamtverzoegerung aus Ereignissen: "
                            + algorithmusErgebnis.getEreignisbewertung().getGesamteVerzoegerung());
            Logger.program(
                    "- Verbrauchter Puffer im Ereignisplan: "
                            + algorithmusErgebnis.getEreignisbewertung().getVerbrauchterPuffer());
            Logger.program(
                    "- Puffer-Nutzungsgrad im Ereignisplan: "
                            + formatiereQuote(
                                    algorithmusErgebnis
                                            .getEreignisbewertung()
                                            .getPufferNutzungsgrad()));
            Logger.program(
                    "- Ereignis-Ueberhang im Ereignisplan: "
                            + algorithmusErgebnis.getEreignisbewertung().getEreignisUeberhang());
            Logger.program(
                    "- Ereignis-Ueberhangquote im Ereignisplan: "
                            + formatiereQuote(
                                    algorithmusErgebnis
                                            .getEreignisbewertung()
                                            .getEreignisUeberhangquote()));
            Logger.program(
                    "- Vermiedene zusaetzliche Stoerungsverzoegerung gegenueber Standard: "
                            + standardReferenz.vermiedeneZusaetzlicheStoerungsverzoegerung(
                                    algorithmusErgebnis));
            Logger.program(
                    "- Stoerungsrobustheit pro Puffer-ZE: "
                            + formatiereZahl(
                                    standardReferenz.stoerungsrobustheitProPufferZe(
                                            algorithmusErgebnis)));
        }

        try {
            Path reportPfad =
                    reportOrdner == null
                            ? GanttDiagrammExport.exportiereFuerAktuellenLauf(
                                    projektInstanz, ereignisse, algorithmusErgebnisse)
                            : GanttDiagrammExport.exportiere(
                                    reportOrdner,
                                    Logger.getRunId(),
                                    projektInstanz,
                                    ereignisse,
                                    algorithmusErgebnisse);
            Logger.program("Gantt-Report: " + reportPfad);
            return reportPfad;
        } catch (IOException e) {
            Logger.warning("Gantt-Report konnte nicht geschrieben werden: " + e.getMessage());
            return null;
        }
    }

    private static void gibPlanAus(
            String algorithmusName,
            Projektplan projektplan,
            PlanBewertung bewertung,
            String szenarioName) {
        Logger.program(algorithmusName + " (" + szenarioName + "):");
        Logger.program("- Makespan: " + bewertung.getMakespan());
        for (int k = 0; k < bewertung.getAnzahlRessourcen(); k++)
            Logger.program(
                    "- Reservierte Ressourcenauslastung R"
                            + (k + 1)
                            + ": "
                            + String.format(PROZENT_FORMAT, bewertung.getAuslastung(k) * 100));
        Logger.program("- Gesamter Puffer: " + bewertung.getGesamterPuffer());
        Logger.program(
                "- Gesamte Projekt-Ereignisverzoegerung: " + bewertung.getGesamteVerzoegerung());
        Logger.program("- Verbrauchter Puffer: " + bewertung.getVerbrauchterPuffer());
        Logger.program(
                "- Puffer-Nutzungsgrad: " + formatiereQuote(bewertung.getPufferNutzungsgrad()));
        Logger.program("- Ereignis-Ueberhang: " + bewertung.getEreignisUeberhang());
        Logger.program(
                "- Ereignis-Ueberhangquote: "
                        + formatiereQuote(bewertung.getEreignisUeberhangquote()));
        Logger.program(
                "- Lesart Aktivitaetszeile: Dauer = BasisDauer + Puffer + ZusaetzlichNachPuffer; "
                        + "Verzoegerung ist die gesamte Stoerung, VerzoegerungImPuffer der davon im reservierten Puffer "
                        + "aufgefangene Anteil.");
        Logger.program("- Aktivitaeten:");

        for (PlanAktivitaet planAktivitaet : projektplan.getAktivitaetenSortiert()) {
            Logger.program(
                    "  ID "
                            + planAktivitaet.getId()
                            + ": Start="
                            + planAktivitaet.getStartzeit()
                            + ", Ende="
                            + planAktivitaet.getEndzeit()
                            + ", BasisDauer="
                            + planAktivitaet.getBasisDauer()
                            + ", Dauer="
                            + planAktivitaet.getEffektiveDauer()
                            + ", Puffer="
                            + planAktivitaet.getPuffer()
                            + ", Verzoegerung="
                            + planAktivitaet.getVerzoegerung()
                            + ", VerzoegerungImPuffer="
                            + planAktivitaet.getVerzoegerungImPuffer()
                            + ", ZusaetzlichNachPuffer="
                            + planAktivitaet.getZusatzverzoegerungNachPuffer()
                            + ", Score="
                            + String.format("%.4f", planAktivitaet.getScoreBeiAuswahl()));
        }
    }

    private static double berechneRelativeProjektverlaengerung(
            AlgorithmusErgebnis algorithmusErgebnis) {
        int basisMakespan = algorithmusErgebnis.getBasisbewertung().getMakespan();
        if (basisMakespan == 0) {
            return Double.NaN;
        }
        return (double)
                        (algorithmusErgebnis.getEreignisbewertung().getMakespan()
                                - algorithmusErgebnis.getBasisbewertung().getMakespan())
                / basisMakespan;
    }

    private static String formatiereQuote(double wert) {
        return Double.isNaN(wert) ? "n/a" : String.format(PROZENT_FORMAT, wert * 100);
    }

    private static String formatiereZahl(double wert) {
        return Double.isNaN(wert) ? "n/a" : String.format("%.4f", wert);
    }
}
