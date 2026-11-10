package output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logging.Logger;
import model.AlgorithmusErgebnis;
import model.PlanAktivitaet;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

/**
 * Schreibt Gantt-Diagramme als Markdown mit eingebettetem SVG; der Betrachter muss SVG anzeigen.
 * Die Zeitachse bleibt linear und wird bei mehr als 200 ZE komprimiert.
 */
public final class GanttDiagrammExport {
    private static final String REPORT_DIR = "reports";
    private static final String BASIS_FARBE = "#4e79a7";
    private static final String PUFFER_FARBE = "#f28e2b";
    private static final String EREIGNIS_FARBE = "#e15759";
    private static final String EREIGNIS_UEBERHANG_FARBE = "#b22222";
    private static final String GITTER_FARBE = "#d7dde5";
    private static final String TEXT_FARBE = "#1f2933";
    private static final int ZE_BREITE = 28;
    private static final int LABEL_BREITE = 250;
    private static final int RECHTE_BREITE = 180;
    private static final int KOPF_HOEHE = 54;
    private static final int ZEILEN_HOEHE = 28;
    private static final int ZEILEN_ABSTAND = 14;
    private static final int UNTERER_RAND = 18;
    private static final int MAX_ZEITACHSEN_EINHEITEN_OHNE_KOMPRESSION = 200;
    private static final int MAX_BESCHRIFTETE_INTERVALLE_BEI_KOMPRESSION = 12;
    private static final String PROZENT_FORMAT = "%.2f%%";
    private static final String LEGENDEN_FARBFELD_PREFIX =
            "- <span style=\"display:inline-block;width:12px;height:12px;background:";
    private static final String SVG_RECT_PREFIX = "<rect x=\"";
    private static final String SVG_Y_ATTR = "\" y=\"";
    private static final String SVG_WIDTH_ATTR = "\" width=\"";
    private static final String SVG_HEIGHT_ATTR = "\" height=\"";
    private static final String SVG_ROUNDED_FILL_ATTR = "\" rx=\"4\" ry=\"4\" fill=\"";
    private static final String SVG_EMPTY_ELEMENT_END = "\"/>\n";
    private static final String SVG_TEXT_END = "</text>\n";
    private static final String MARKDOWN_CODE_LINE_END = "`\n";
    private static final String MARKDOWN_CODE_PARAGRAPH_END = "`\n\n";

    private GanttDiagrammExport() {}

    /** Schreibt den Bericht des aktuellen Laufs nach reports. */
    public static Path exportiereFuerAktuellenLauf(
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> algorithmusErgebnisse)
            throws IOException {
        return exportiere(
                Path.of(REPORT_DIR),
                Logger.getRunId(),
                projektInstanz,
                ereignisse,
                algorithmusErgebnisse);
    }

    public static Path exportiere(
            Path zielOrdner,
            String laufId,
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> algorithmusErgebnisse)
            throws IOException {
        Files.createDirectories(zielOrdner);

        Path datei = zielOrdner.resolve("gantt-run-" + laufId + ".md");
        Files.writeString(
                datei,
                baueMarkdown(laufId, projektInstanz, ereignisse, algorithmusErgebnisse),
                StandardCharsets.UTF_8);

        return datei.toAbsolutePath().normalize();
    }

    private static String baueMarkdown(
            String laufId,
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> algorithmusErgebnisse) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Gantt-Report Lauf ").append(laufId).append("\n\n");
        markdown.append("- Logdatei: `")
                .append(Logger.getLogFilePath())
                .append(MARKDOWN_CODE_LINE_END);
        markdown.append("- Aktivitaeten: `")
                .append(projektInstanz.getAktivitaeten().size())
                .append(MARKDOWN_CODE_LINE_END);
        markdown.append("- Kapazitaeten: `");
        for (int k = 0; k < projektInstanz.getAnzahlRessourcen(); k++) {
            if (k > 0) markdown.append(", ");
            markdown.append("R").append(k + 1).append("=").append(projektInstanz.getKapazitaet(k));
        }
        markdown.append(MARKDOWN_CODE_PARAGRAPH_END);

        markdown.append("## Legende\n\n");
        markdown.append(LEGENDEN_FARBFELD_PREFIX)
                .append(BASIS_FARBE)
                .append(";border-radius:2px;\"></span> Basisdauer\n");
        markdown.append(LEGENDEN_FARBFELD_PREFIX)
                .append(PUFFER_FARBE)
                .append(";border-radius:2px;\"></span> Verbleibender Puffer\n");
        markdown.append(LEGENDEN_FARBFELD_PREFIX)
                .append(PUFFER_FARBE)
                .append(";border-radius:2px;border:1px solid ")
                .append(EREIGNIS_FARBE)
                .append(";background-image:repeating-linear-gradient(135deg, transparent 0 4px, ")
                .append(EREIGNIS_FARBE)
                .append(" 4px 7px);\"></span> Durch Ereignis verbrauchter Puffer\n");
        markdown.append(LEGENDEN_FARBFELD_PREFIX)
                .append(EREIGNIS_UEBERHANG_FARBE)
                .append(";border-radius:2px;\"></span> Reiner Ereignis-Ueberhang\n");
        markdown.append(
                "- Im Ereignisfall zeigt das orange-rote Muster den lokal verbrauchten Puffer; dunkelrot verlaengert das reservierte Aktivitaetsintervall. Der Projektendtermin verschiebt sich nur, soweit das Wellenmaximum steigt.\n");
        markdown.append(
                "- Die gesamte Balkenlaenge eines Vorgangs entspricht `Basisdauer + verbleibender Puffer + verbrauchter Puffer + Ereignis-Ueberhang`.\n");
        markdown.append(
                "- Links steht pro Balken die Planungswelle und das belegte Ressourcenprofil `R1/.../Rm` der Aktivitaet.\n");
        markdown.append(
                "- Eine neue Planungswelle beginnt erst, wenn alle Aktivitaeten der vorherigen Welle beendet sind.\n");
        markdown.append(
                "- Leerraum auf der Zeitachse bedeutet: In diesem Intervall wurde keine weitere Aktivitaet gestartet.\n");
        markdown.append(
                "- Bei mehr als 200 Zeiteinheiten wird die Achse proportional komprimiert und mit groesseren Tick-Abstaenden beschriftet.\n\n");

        markdown.append("## Bewertungsmetriken\n\n");
        markdown.append(
                "- `Stoerungsrobustheit pro Puffer-ZE = (vermiedene zusaetzliche Stoerungsverzoegerung gegenueber dem Standard) / verplanter Gesamtpuffer`\n");
        markdown.append(
                "- Pufferbudgets sind absichtlich verfahrensspezifisch und duerfen voneinander abweichen.\n");
        markdown.append(
                "- Bei festen Wellen gilt je Welle Ereignisdauer=max(gepufferte Basisdauer, ungepufferte Ereignisdauer). Die eigene Endterminabweichung kann deshalb nicht groesser als bei Standard sein; der Vergleich quantifiziert deren Hoehe und den Reserveaufwand.\n");
        markdown.append(
                "- Die Stoerungsrobustheit normalisiert nur den Schutz gegen zusaetzliche Ereigniswirkung; sie bewertet nicht die gesamte Projektdauer.\n");
        markdown.append(
                "- Basis- und Ereignis-Makespan-Aufschlag vergleichen die absolute Projektdauer direkt mit dem jeweiligen Standardplan.\n");
        markdown.append(
                "- `reservierte Ressourcenauslastung = Summe(Bedarf * effektive Dauer) / (Kapazitaet * Makespan)`; enthaltene Pufferzeit zaehlt als Belegung.\n");
        markdown.append(
                "- Die Planung nutzt prioritaetsregelbasierte wellengebundene Planung; die naechste Welle startet nach dem spaetesten Ende der aktuellen Welle.\n");
        markdown.append(
                "- `vermiedene zusaetzliche Stoerungsverzoegerung = Delta Standard - Delta Verfahren`, mit `Delta = Makespan(mit Ereignissen) - Makespan(ohne Ereignisse)`\n");
        markdown.append(
                "- `relative Projektverlaengerung durch Ereignisse = (Makespan(mit Ereignissen) - Makespan(ohne Ereignisse)) / Makespan(ohne Ereignisse)`\n");
        markdown.append("- `Puffer-Nutzungsgrad = verbrauchter Puffer / verplanter Puffer`\n");
        markdown.append(
                "- `Ereignis-Ueberhangquote = Ereignis-Ueberhang / gesamte Ereignisverzoegerung`\n");
        markdown.append(
                "- Interpretation: Eine hohe Stoerungsrobustheit bedeutet nur einen hohen Schutzbeitrag je Puffer-ZE; fuer die Gesamtbewertung sind die Makespan-Aufschlaege mitzulesen.\n");
        markdown.append(
                "- Interpretation: `n/a` bedeutet, dass fuer die Kennzahl kein sinnvoller Nenner vorhanden ist, also z. B. kein Puffer oder kein Ereignis.\n");
        markdown.append(
                "- Vergleichsbloecke stellen immer `ohne Ereignisse` und `mit Ereignissen` desselben Verfahrens gegenueber; nur die Kennzahlen zur Standard-Referenz ziehen das Standardverfahren zusaetzlich heran.\n\n");

        markdown.append("## Projekt-Ereignisse\n\n");
        if (ereignisse.isEmpty()) {
            markdown.append("- keine\n\n");
        } else {
            for (ProjektEreignis ereignis : ereignisse) {
                markdown.append("- Aktivitaet `")
                        .append(ereignis.getAktivitaetId())
                        .append("` +`")
                        .append(ereignis.getVerzoegerung())
                        .append("` ZE\n");
            }
            markdown.append("\n");
        }

        StandardReferenz standardReferenz = StandardReferenz.aus(algorithmusErgebnisse);

        for (AlgorithmusErgebnis algorithmusErgebnis : algorithmusErgebnisse) {
            markdown.append("## ").append(algorithmusErgebnis.getAlgorithmusName()).append("\n\n");
            markdown.append(
                    bauePlanAbschnitt(
                            "ohne Ereignisse",
                            algorithmusErgebnis.getBasisplan(),
                            algorithmusErgebnis.getBasisbewertung()));
            markdown.append(
                    bauePlanAbschnitt(
                            "mit Ereignissen",
                            algorithmusErgebnis.getEreignisplan(),
                            algorithmusErgebnis.getEreignisbewertung()));
            markdown.append(baueVergleichsAbschnitt(algorithmusErgebnis, standardReferenz));
        }

        return markdown.toString();
    }

    private static String bauePlanAbschnitt(
            String szenarioName, Projektplan projektplan, PlanBewertung planBewertung) {
        StringBuilder abschnitt = new StringBuilder();
        abschnitt.append("### ").append(szenarioName).append("\n\n");
        abschnitt
                .append("- Planname: `")
                .append(projektplan.getName())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Makespan: `")
                .append(planBewertung.getMakespan())
                .append(MARKDOWN_CODE_LINE_END);
        for (int k = 0; k < planBewertung.getAnzahlRessourcen(); k++)
            abschnitt
                    .append("- Reservierte Ressourcenauslastung R")
                    .append(k + 1)
                    .append(": `")
                    .append(formatiereProzent(planBewertung.getAuslastung(k)))
                    .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Gesamter verplanter Puffer: `")
                .append(planBewertung.getGesamterPuffer())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Gesamte Projekt-Ereignisverzoegerung: `")
                .append(planBewertung.getGesamteVerzoegerung())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Verbrauchter Puffer: `")
                .append(planBewertung.getVerbrauchterPuffer())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Puffer-Nutzungsgrad: `")
                .append(formatiereQuote(planBewertung.getPufferNutzungsgrad()))
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Ereignis-Ueberhang: `")
                .append(planBewertung.getEreignisUeberhang())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Ereignis-Ueberhangquote: `")
                .append(formatiereQuote(planBewertung.getEreignisUeberhangquote()))
                .append(MARKDOWN_CODE_PARAGRAPH_END);
        abschnitt.append(
                "- Diagramm-Lesart: Gesamte Balkenlaenge = Basisdauer + verbleibender Puffer + verbrauchter Puffer + Ereignis-Ueberhang.\n\n");
        abschnitt.append(baueSvg(projektplan)).append("\n\n");
        return abschnitt.toString();
    }

    private static String baueVergleichsAbschnitt(
            AlgorithmusErgebnis algorithmusErgebnis, StandardReferenz standardReferenz) {
        StringBuilder abschnitt = new StringBuilder();
        PlanBewertung basis = algorithmusErgebnis.getBasisbewertung();
        PlanBewertung ereignis = algorithmusErgebnis.getEreignisbewertung();
        int verfahrensDelta = ereignis.getMakespan() - basis.getMakespan();
        double relativeProjektverlaengerung = berechneRelativeProjektverlaengerung(basis, ereignis);

        abschnitt.append("### Vergleich\n\n");
        abschnitt.append(
                "- Dieser Abschnitt zeigt sowohl die Ereigniswirkung innerhalb des Verfahrens als auch die absoluten Makespan-Aufschlaege gegenueber dem Standard.\n");
        abschnitt
                .append("- Basis-Makespan-Aufschlag gegenueber Standard: `")
                .append(standardReferenz.basisMakespanAufschlag(algorithmusErgebnis))
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Ereignis-Makespan-Aufschlag gegenueber Standard: `")
                .append(standardReferenz.ereignisMakespanAufschlag(algorithmusErgebnis))
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Makespan-Differenz: `")
                .append(ereignis.getMakespan() - basis.getMakespan())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Relative Projektverlaengerung durch Ereignisse: `")
                .append(formatiereQuote(relativeProjektverlaengerung))
                .append(MARKDOWN_CODE_LINE_END);
        for (int k = 0; k < ereignis.getAnzahlRessourcen(); k++)
            abschnitt
                    .append("- Delta reservierte Ressourcenauslastung R")
                    .append(k + 1)
                    .append(": `")
                    .append(
                            formatiereProzent(ereignis.getAuslastung(k) - basis.getAuslastung(k))
                                    .replace("%", " Prozentpunkte"))
                    .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Zusaetzlicher verplanter Puffer: `")
                .append(ereignis.getGesamterPuffer() - basis.getGesamterPuffer())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Zusaetzliche Gesamtverzoegerung aus Ereignissen: `")
                .append(ereignis.getGesamteVerzoegerung() - basis.getGesamteVerzoegerung())
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Vermiedene zusaetzliche Stoerungsverzoegerung gegenueber Standard: `")
                .append(
                        standardReferenz.vermiedeneZusaetzlicheStoerungsverzoegerung(
                                algorithmusErgebnis))
                .append(MARKDOWN_CODE_LINE_END);
        abschnitt
                .append("- Stoerungsrobustheit pro Puffer-ZE: `")
                .append(
                        formatiereZahl(
                                standardReferenz.stoerungsrobustheitProPufferZe(
                                        algorithmusErgebnis)))
                .append(MARKDOWN_CODE_PARAGRAPH_END);
        return abschnitt.toString();
    }

    private static String formatiereProzent(double wert) {
        return String.format(PROZENT_FORMAT, wert * 100);
    }

    private static String formatiereQuote(double wert) {
        return Double.isNaN(wert) ? "n/a" : String.format(PROZENT_FORMAT, wert * 100);
    }

    private static String formatiereZahl(double wert) {
        return Double.isNaN(wert) ? "n/a" : String.format("%.4f", wert);
    }

    private static double berechneRelativeProjektverlaengerung(
            PlanBewertung basis, PlanBewertung ereignis) {
        if (basis.getMakespan() == 0) {
            return Double.NaN;
        }
        return (double) (ereignis.getMakespan() - basis.getMakespan()) / basis.getMakespan();
    }

    private static String baueSvg(Projektplan projektplan) {
        List<PlanAktivitaet> aktivitaeten = projektplan.getAktivitaetenSortiert();
        Map<Integer, Integer> welleProStartzeit = berechneWellen(aktivitaeten);
        int makespan = Math.max(1, projektplan.getMakespan());
        int zeitachsenBreite =
                Math.min(makespan, MAX_ZEITACHSEN_EINHEITEN_OHNE_KOMPRESSION) * ZE_BREITE;
        int diagrammBreite = LABEL_BREITE + zeitachsenBreite + RECHTE_BREITE;
        int diagrammHoehe = berechneDiagrammHoehe(aktivitaeten.size());

        StringBuilder svg = new StringBuilder();
        fuegeSvgKopfHinzu(svg, diagrammBreite, diagrammHoehe);
        fuegeZeitachseHinzu(svg, makespan, zeitachsenBreite, diagrammHoehe);

        for (int index = 0; index < aktivitaeten.size(); index++) {
            fuegeAktivitaetsZeileHinzu(
                    svg,
                    aktivitaeten.get(index),
                    index,
                    makespan,
                    zeitachsenBreite,
                    diagrammBreite,
                    welleProStartzeit);
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private static void fuegeSvgKopfHinzu(
            StringBuilder svg, int diagrammBreite, int diagrammHoehe) {
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(diagrammBreite)
                .append(" ")
                .append(diagrammHoehe)
                .append(SVG_WIDTH_ATTR)
                .append(diagrammBreite)
                .append(SVG_HEIGHT_ATTR)
                .append(diagrammHoehe)
                .append("\">\n");
        fuegeEinfachesRechteckHinzu(svg, 0, 0, diagrammBreite, diagrammHoehe, "#ffffff");
        svg.append("<defs>\n");
        // Das Muster kennzeichnet den verbrauchten Anteil der Reserve.
        svg.append(
                "<pattern id=\"verbrauchterPuffer\" patternUnits=\"userSpaceOnUse\" width=\"8\" height=\"8\" patternTransform=\"rotate(45)\">\n");
        svg.append("<rect width=\"8\" height=\"8\" fill=\"")
                .append(PUFFER_FARBE)
                .append(SVG_EMPTY_ELEMENT_END);
        svg.append("<line x1=\"0\" y1=\"0\" x2=\"0\" y2=\"8\" stroke=\"")
                .append(EREIGNIS_FARBE)
                .append("\" stroke-width=\"3\"/>\n");
        svg.append("</pattern>\n");
        svg.append("</defs>\n");
        fuegeSvgStylesHinzu(svg);
    }

    private static void fuegeSvgStylesHinzu(StringBuilder svg) {
        svg.append("<style>");
        fuegeCssKlasseHinzu(svg, "axis", "font:12px sans-serif;fill:" + TEXT_FARBE);
        fuegeCssKlasseHinzu(svg, "label", "font:12px sans-serif;fill:" + TEXT_FARBE);
        fuegeCssKlasseHinzu(svg, "sublabel", "font:11px sans-serif;fill:" + TEXT_FARBE);
        fuegeCssKlasseHinzu(svg, "bartext", "font:12px sans-serif;font-weight:bold;fill:#ffffff");
        fuegeCssKlasseHinzu(svg, "detail", "font:11px sans-serif;fill:" + TEXT_FARBE);
        svg.append("</style>\n");
    }

    private static void fuegeCssKlasseHinzu(StringBuilder svg, String klasse, String regeln) {
        svg.append('.').append(klasse).append('{').append(regeln).append(";}");
    }

    private static void fuegeZeitachseHinzu(
            StringBuilder svg, int makespan, int zeitachsenBreite, int diagrammHoehe) {
        int beschrifteteIntervalle =
                makespan <= MAX_ZEITACHSEN_EINHEITEN_OHNE_KOMPRESSION
                        ? makespan
                        : MAX_BESCHRIFTETE_INTERVALLE_BEI_KOMPRESSION;
        long schrittweite =
                Math.max(
                        1L,
                        ((long) makespan + beschrifteteIntervalle - 1L) / beschrifteteIntervalle);

        for (long zeit = 0; zeit < makespan; zeit += schrittweite) {
            fuegeZeitmarkeHinzu(svg, zeit, makespan, zeitachsenBreite, diagrammHoehe);
        }
        fuegeZeitmarkeHinzu(svg, makespan, makespan, zeitachsenBreite, diagrammHoehe);
    }

    private static void fuegeAktivitaetsZeileHinzu(
            StringBuilder svg,
            PlanAktivitaet aktivitaet,
            int index,
            int makespan,
            int zeitachsenBreite,
            int diagrammBreite,
            Map<Integer, Integer> welleProStartzeit) {
        int y = KOPF_HOEHE + (index * (ZEILEN_HOEHE + ZEILEN_ABSTAND));
        long start = aktivitaet.getStartzeit();
        long basisEnde = start + aktivitaet.getBasisDauer();
        long verbrauchtesPufferEnde = basisEnde + aktivitaet.getVerzoegerungImPuffer();
        long pufferEnde = verbrauchtesPufferEnde + aktivitaet.getVerbleibenderPuffer();
        long aktivitaetsEnde = pufferEnde + aktivitaet.getZusatzverzoegerungNachPuffer();

        int startX = LABEL_BREITE + skaliereZeit(start, makespan, zeitachsenBreite);
        int basisEndeX = LABEL_BREITE + skaliereZeit(basisEnde, makespan, zeitachsenBreite);
        int verbrauchtesPufferEndeX =
                LABEL_BREITE + skaliereZeit(verbrauchtesPufferEnde, makespan, zeitachsenBreite);
        int pufferEndeX = LABEL_BREITE + skaliereZeit(pufferEnde, makespan, zeitachsenBreite);
        int aktivitaetsEndeX =
                LABEL_BREITE + skaliereZeit(aktivitaetsEnde, makespan, zeitachsenBreite);
        int basisBreite = berechneSichtbareBreite(aktivitaet.getBasisDauer(), basisEndeX - startX);
        int verzoegerungImPufferBreite =
                berechneSichtbareBreite(
                        aktivitaet.getVerzoegerungImPuffer(), verbrauchtesPufferEndeX - basisEndeX);
        int verbleibenderPufferBreite =
                berechneSichtbareBreite(
                        aktivitaet.getVerbleibenderPuffer(), pufferEndeX - verbrauchtesPufferEndeX);
        int zusaetzlicheVerzoegerungBreite =
                berechneSichtbareBreite(
                        aktivitaet.getZusatzverzoegerungNachPuffer(),
                        aktivitaetsEndeX - pufferEndeX);
        int sichtbareGesamtBreite =
                basisBreite
                        + verzoegerungImPufferBreite
                        + verbleibenderPufferBreite
                        + zusaetzlicheVerzoegerungBreite;
        startX = Math.min(startX, LABEL_BREITE + zeitachsenBreite - sichtbareGesamtBreite);
        int welle = welleProStartzeit.getOrDefault(aktivitaet.getStartzeit(), 1);

        fuegeAktivitaetsBeschriftungHinzu(svg, aktivitaet, y, welle);
        fuegeTrennlinieHinzu(svg, y, diagrammBreite);
        fuegeBalkenWennSichtbar(svg, startX, y, basisBreite, BASIS_FARBE);
        fuegeBalkenMitKonturWennSichtbar(
                svg,
                startX + basisBreite,
                y,
                verzoegerungImPufferBreite,
                "url(#verbrauchterPuffer)",
                EREIGNIS_FARBE);
        fuegeBalkenWennSichtbar(
                svg,
                startX + basisBreite + verzoegerungImPufferBreite,
                y,
                verbleibenderPufferBreite,
                PUFFER_FARBE);
        fuegeBalkenWennSichtbar(
                svg,
                startX + basisBreite + verzoegerungImPufferBreite + verbleibenderPufferBreite,
                y,
                zusaetzlicheVerzoegerungBreite,
                EREIGNIS_UEBERHANG_FARBE);
        fuegeAktivitaetsIdHinzu(svg, aktivitaet, startX, y, basisBreite);
        fuegeZeitBeschriftungHinzu(svg, aktivitaet, y, zeitachsenBreite);
    }

    private static void fuegeAktivitaetsBeschriftungHinzu(
            StringBuilder svg, PlanAktivitaet aktivitaet, int y, int welle) {
        svg.append("<text class=\"label\" x=\"12")
                .append(SVG_Y_ATTR)
                .append(y + 12)
                .append("\">Welle ")
                .append(welle)
                .append(" | ID ")
                .append(aktivitaet.getId())
                .append(SVG_TEXT_END);
        svg.append("<text class=\"sublabel\" x=\"12")
                .append(SVG_Y_ATTR)
                .append(y + 25)
                .append("\">belegt ");
        for (int k = 0; k < aktivitaet.getAnzahlRessourcen(); k++) {
            if (k > 0) svg.append(", ");
            svg.append("R").append(k + 1).append("=").append(aktivitaet.getRessource(k));
        }
        svg.append(SVG_TEXT_END);
    }

    private static void fuegeTrennlinieHinzu(StringBuilder svg, int y, int diagrammBreite) {
        svg.append("<line x1=\"0\" y1=\"")
                .append(y + ZEILEN_HOEHE + 6)
                .append("\" x2=\"")
                .append(diagrammBreite)
                .append("\" y2=\"")
                .append(y + ZEILEN_HOEHE + 6)
                .append("\" stroke=\"#eef2f6\" stroke-width=\"1\"/>\n");
    }

    private static void fuegeBalkenWennSichtbar(
            StringBuilder svg, int x, int y, int breite, String fill) {
        if (breite > 0) {
            fuegeGerundetesRechteckHinzu(svg, x, y, breite, fill);
        }
    }

    private static void fuegeBalkenMitKonturWennSichtbar(
            StringBuilder svg, int x, int y, int breite, String fill, String stroke) {
        if (breite > 0) {
            fuegeGerundetesRechteckMitKonturHinzu(svg, x, y, breite, fill, stroke);
        }
    }

    private static void fuegeAktivitaetsIdHinzu(
            StringBuilder svg, PlanAktivitaet aktivitaet, int startX, int y, int basisBreite) {
        if (basisBreite >= 26) {
            svg.append("<text class=\"bartext\" x=\"")
                    .append(startX + (basisBreite / 2))
                    .append(SVG_Y_ATTR)
                    .append(y + 18)
                    .append("\" text-anchor=\"middle\">")
                    .append(aktivitaet.getId())
                    .append(SVG_TEXT_END);
        } else {
            svg.append("<text class=\"detail\" x=\"")
                    .append(startX + basisBreite + 4)
                    .append(SVG_Y_ATTR)
                    .append(y + 18)
                    .append("\">ID ")
                    .append(aktivitaet.getId())
                    .append(SVG_TEXT_END);
        }
    }

    private static void fuegeZeitBeschriftungHinzu(
            StringBuilder svg, PlanAktivitaet aktivitaet, int y, int zeitachsenBreite) {
        svg.append("<text class=\"detail\" x=\"")
                .append(LABEL_BREITE + zeitachsenBreite + 12)
                .append(SVG_Y_ATTR)
                .append(y + 18)
                .append("\">")
                .append("Start ")
                .append(aktivitaet.getStartzeit())
                .append(", Ende ")
                .append(aktivitaet.getEndzeit())
                .append(SVG_TEXT_END);
    }

    private static void fuegeEinfachesRechteckHinzu(
            StringBuilder svg, int x, int y, int breite, int hoehe, String fill) {
        svg.append(SVG_RECT_PREFIX)
                .append(x)
                .append(SVG_Y_ATTR)
                .append(y)
                .append(SVG_WIDTH_ATTR)
                .append(breite)
                .append(SVG_HEIGHT_ATTR)
                .append(hoehe)
                .append("\" fill=\"")
                .append(fill)
                .append(SVG_EMPTY_ELEMENT_END);
    }

    private static void fuegeGerundetesRechteckHinzu(
            StringBuilder svg, int x, int y, int breite, String fill) {
        svg.append(SVG_RECT_PREFIX)
                .append(x)
                .append(SVG_Y_ATTR)
                .append(y)
                .append(SVG_WIDTH_ATTR)
                .append(breite)
                .append(SVG_HEIGHT_ATTR)
                .append(ZEILEN_HOEHE)
                .append(SVG_ROUNDED_FILL_ATTR)
                .append(fill)
                .append(SVG_EMPTY_ELEMENT_END);
    }

    private static void fuegeGerundetesRechteckMitKonturHinzu(
            StringBuilder svg, int x, int y, int breite, String fill, String stroke) {
        svg.append(SVG_RECT_PREFIX)
                .append(x)
                .append(SVG_Y_ATTR)
                .append(y)
                .append(SVG_WIDTH_ATTR)
                .append(breite)
                .append(SVG_HEIGHT_ATTR)
                .append(ZEILEN_HOEHE)
                .append(SVG_ROUNDED_FILL_ATTR)
                .append(fill)
                .append("\" stroke=\"")
                .append(stroke)
                .append("\" stroke-width=\"1\"/>\n");
    }

    private static Map<Integer, Integer> berechneWellen(List<PlanAktivitaet> aktivitaeten) {
        Map<Integer, Integer> welleProStartzeit = new HashMap<>();
        int aktuelleWelle = 1;

        for (PlanAktivitaet aktivitaet : aktivitaeten) {
            // Gleiche Startzeit bedeutet im verwendeten Modell dieselbe Welle.
            if (!welleProStartzeit.containsKey(aktivitaet.getStartzeit())) {
                welleProStartzeit.put(aktivitaet.getStartzeit(), aktuelleWelle);
                aktuelleWelle++;
            }
        }

        return welleProStartzeit;
    }

    private static int berechneDiagrammHoehe(int anzahlAktivitaeten) {
        try {
            return Math.addExact(
                    Math.addExact(
                            KOPF_HOEHE,
                            Math.multiplyExact(anzahlAktivitaeten, ZEILEN_HOEHE + ZEILEN_ABSTAND)),
                    UNTERER_RAND);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Die Anzahl der Aktivitaeten ist fuer den Gantt-Export zu gross", e);
        }
    }

    private static void fuegeZeitmarkeHinzu(
            StringBuilder svg, long zeit, int makespan, int zeitachsenBreite, int diagrammHoehe) {
        int x = LABEL_BREITE + skaliereZeit(zeit, makespan, zeitachsenBreite);
        svg.append("<line x1=\"")
                .append(x)
                .append("\" y1=\"24\" x2=\"")
                .append(x)
                .append("\" y2=\"")
                .append(diagrammHoehe - UNTERER_RAND)
                .append("\" stroke=\"")
                .append(GITTER_FARBE)
                .append("\" stroke-width=\"1\"/>\n");
        svg.append("<text class=\"axis\" x=\"")
                .append(x)
                .append(SVG_Y_ATTR)
                .append("18\" text-anchor=\"middle\">")
                .append(zeit)
                .append(SVG_TEXT_END);
    }

    private static int skaliereZeit(long zeit, int makespan, int zeitachsenBreite) {
        return (int) Math.round(((double) zeit / makespan) * zeitachsenBreite);
    }

    private static int berechneSichtbareBreite(long dauer, int skalierteBreite) {
        return dauer > 0 ? Math.max(1, skalierteBreite) : 0;
    }
}
