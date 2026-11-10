package tests.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import model.AlgorithmusErgebnis;
import model.Datensatz;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import output.GanttDiagrammExport;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Prueft den Markdown- und SVG-Export fuer den Laufbericht.
 *
 * Der Test sichert vor allem die lesbare Struktur des Reports, weil diese
 * fuer die Nachvollziehbarkeit des Programmlaufs fachlich relevant ist.
 */
public final class GanttDiagrammExportTests implements TestSuite {
    @Override
    public String getName() {
        return "output/GanttDiagrammExport";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "schreibt Markdown-Report mit SVG und Legende", this::writesMarkdownReportWithSvg);
        registry.add(getName(), "gruppiert gleiche Startzeiten in Planungswellen", this::groupsEqualStartTimesIntoWaves);
        registry.add(getName(), "skaliert sehr grosse Zeitwerte ohne Ueberlauf", this::scalesVeryLargeMakespan);
        registry.add(getName(), "haelt kurze Aktivitaeten auf komprimierter Achse sichtbar", this::keepsShortActivitiesVisibleOnCompressedAxis);
    }

    private void writesMarkdownReportWithSvg() throws IOException {
        // Der Test prueft nicht nur das SVG, sondern auch die erklaerenden
        // Markdown-Texte, damit der Report ohne Zusatzwissen lesbar bleibt.
        Path reportOrdner = Files.createTempDirectory("ba-gantt-export-");
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(TestDataFactory.datensatz(1, List.of(), 2, 1, 0, 0)),
            1,
            1,
            1
        );

        Datensatz aktivitaet = TestDataFactory.datensatz(1, List.of(), 2, 1, 0, 0);
        Projektplan basisplan = new Projektplan("Basis");
        basisplan.addAktivitaet(TestDataFactory.planAktivitaet(
            aktivitaet
        )
            .zeit(0, 5, 5)
            .puffer(3, 0)
            .auswahl(0.5, 0)
            .build());
        Projektplan ereignisplan = new Projektplan("Ereignis");
        ereignisplan.addAktivitaet(TestDataFactory.planAktivitaet(aktivitaet)
            .zeit(0, 5, 5)
            .puffer(3, 1)
            .auswahl(0.5, 0)
            .build());
        List<ProjektEreignis> ereignisse = List.of(new ProjektEreignis(1, 1));
        PlanBewertung basisbewertung = PlanBewertung.ausPlan(basisplan, instanz, List.of());
        PlanBewertung ereignisbewertung = PlanBewertung.ausPlan(ereignisplan, instanz, ereignisse);
        AlgorithmusErgebnis ergebnis = new AlgorithmusErgebnis(
            "Standard",
            basisplan,
            basisbewertung,
            ereignisplan,
            ereignisbewertung
        );

        Path reportPfad = GanttDiagrammExport.exportiere(
            reportOrdner,
            "testrun",
            instanz,
            ereignisse,
            List.of(ergebnis)
        );

        String inhalt = Files.readString(reportPfad);
        Assert.isTrue(Files.exists(reportPfad), "Die Report-Datei muss erzeugt werden");
        Assert.contains(inhalt, "# Gantt-Report Lauf testrun", "Der Report-Titel muss enthalten sein");
        Assert.contains(inhalt, "## Legende", "Die Legende muss enthalten sein");
        Assert.contains(inhalt, "Basisdauer", "Die Basisdauer muss beschriftet werden");
        Assert.contains(inhalt, "Verbleibender Puffer", "Der verbleibende Puffer muss beschriftet werden");
        Assert.contains(inhalt, "Durch Ereignis verbrauchter Puffer", "Der verbrauchte Puffer muss beschriftet werden");
        Assert.contains(inhalt, "Reiner Ereignis-Ueberhang", "Der reine Ereignis-Ueberhang muss beschriftet werden");
        Assert.contains(inhalt, "zeigt das orange-rote Muster den lokal verbrauchten Puffer", "Die lokale Verrechnung von Ereignis und Puffer muss erklaert werden");
        Assert.contains(inhalt, "Der Projektendtermin verschiebt sich nur, soweit das Wellenmaximum steigt", "Lokale und projektweite Wirkung muessen unterschieden werden");
        Assert.contains(inhalt, "gesamte Balkenlaenge eines Vorgangs entspricht `Basisdauer + verbleibender Puffer + verbrauchter Puffer + Ereignis-Ueberhang`", "Die Balkenlaenge muss im Report erklaert werden");
        Assert.contains(inhalt, "Planungswelle und das belegte Ressourcenprofil", "Die linke Beschriftung muss erklaert werden");
        Assert.contains(inhalt, "Eine neue Planungswelle beginnt erst, wenn alle Aktivitaeten der vorherigen Welle beendet sind", "Die Wellenbarriere muss erklaert werden");
        Assert.contains(inhalt, "## Bewertungsmetriken", "Die allgemeinen Erklaerungen der Bewertungsmetriken muessen im Report stehen");
        Assert.contains(inhalt, "Stoerungsrobustheit pro Puffer-ZE = (vermiedene zusaetzliche Stoerungsverzoegerung gegenueber dem Standard) / verplanter Gesamtpuffer", "Die Stoerungsrobustheit muss im Report erklaert werden");
        Assert.contains(inhalt, "Pufferbudgets sind absichtlich verfahrensspezifisch", "Die verfahrensspezifischen Pufferbudgets muessen erklaert werden");
        Assert.contains(inhalt, "Stoerungsrobustheit normalisiert nur den Schutz gegen zusaetzliche Ereigniswirkung", "Die Grenze der Stoerungsrobustheit muss erklaert werden");
        Assert.contains(inhalt, "reservierte Ressourcenauslastung = Summe(Bedarf * effektive Dauer) / (Kapazitaet * Makespan)", "Die reservierte Auslastung muss im Report definiert werden");
        Assert.contains(inhalt, "enthaltene Pufferzeit zaehlt als Belegung", "Die Pufferwirkung auf die Auslastung muss erklaert werden");
        Assert.contains(inhalt, "Planung nutzt prioritaetsregelbasierte wellengebundene Planung; die naechste Welle startet nach dem spaetesten Ende der aktuellen Welle", "Die Left-Shift-Planungslogik muss erklaert werden");
        Assert.contains(inhalt, "relative Projektverlaengerung durch Ereignisse = (Makespan(mit Ereignissen) - Makespan(ohne Ereignisse)) / Makespan(ohne Ereignisse)", "Die relative Projektverlaengerung muss im Report erklaert werden");
        Assert.contains(inhalt, "Interpretation: `n/a` bedeutet", "Die Sonderfaelle muessen im Report erklaert werden");
        Assert.contains(inhalt, "Vergleichsbloecke stellen immer `ohne Ereignisse` und `mit Ereignissen` desselben Verfahrens gegenueber", "Die Vergleichsrichtung muss im Report erklaert werden");
        Assert.contains(inhalt, "## Projekt-Ereignisse", "Projekt-Ereignisse muessen als eigener Abschnitt enthalten sein");
        Assert.contains(inhalt, "Reservierte Ressourcenauslastung R1: `100,00%`", "Die Ressourcenauslastung muss im Report stehen");
        Assert.contains(inhalt, "Reservierte Ressourcenauslastung R2: `0,00%`", "Auch Nullauslastung muss im Report stehen");
        Assert.contains(inhalt, "Gesamter verplanter Puffer: `3`", "Der gesamte verplante Puffer muss im Report stehen");
        Assert.contains(inhalt, "Gesamte Projekt-Ereignisverzoegerung: `1`", "Die gesamte Projekt-Ereignisverzoegerung muss im Report stehen");
        Assert.contains(inhalt, "Verbrauchter Puffer: `1`", "Der verbrauchte Puffer muss im Report stehen");
        Assert.contains(inhalt, "Puffer-Nutzungsgrad: `33,33%`", "Der Puffer-Nutzungsgrad muss im Report stehen");
        Assert.contains(inhalt, "Ereignis-Ueberhang: `0`", "Der Ereignis-Ueberhang muss im Report stehen");
        Assert.contains(inhalt, "Ereignis-Ueberhangquote: `0,00%`", "Die Ereignis-Ueberhangquote muss im Report stehen");
        Assert.contains(inhalt, "### Vergleich", "Der Vergleichsabschnitt muss enthalten sein");
        Assert.contains(inhalt, "Ereigniswirkung innerhalb des Verfahrens als auch die absoluten Makespan-Aufschlaege", "Der Vergleichsabschnitt muss seine beiden Bezugsrichtungen erklaeren");
        Assert.contains(inhalt, "Basis-Makespan-Aufschlag gegenueber Standard: `0`", "Der Basis-Aufschlag muss im Report stehen");
        Assert.contains(inhalt, "Ereignis-Makespan-Aufschlag gegenueber Standard: `0`", "Der Ereignis-Aufschlag muss im Report stehen");
        Assert.contains(inhalt, "Makespan-Differenz: `0`", "Die Makespan-Differenz muss im Report stehen");
        Assert.contains(inhalt, "Relative Projektverlaengerung durch Ereignisse: `0,00%`", "Die relative Projektverlaengerung muss im Report stehen");
        Assert.contains(inhalt, "Zusaetzliche Gesamtverzoegerung aus Ereignissen: `1`", "Die Vergleichswerte muessen im Report stehen");
        Assert.contains(inhalt, "Vermiedene zusaetzliche Stoerungsverzoegerung gegenueber Standard: `0`", "Die vermiedene Stoerungsverzoegerung muss im Report stehen");
        Assert.contains(inhalt, "Stoerungsrobustheit pro Puffer-ZE: `0,0000`", "Die Stoerungsrobustheit muss im Report stehen");
        Assert.contains(inhalt, "<svg", "Der Report muss ein eingebettetes SVG enthalten");
        Assert.contains(inhalt, "ID 1", "Die Aktivitaets-ID muss im Report auftauchen");
        Assert.contains(inhalt, "Welle 1 | ID 1", "Die Welle muss links ausgewiesen werden");
        Assert.contains(inhalt, "belegt R1=1, R2=0, R3=0", "Das Ressourcenprofil muss links im Diagramm ausgewiesen werden");
        Assert.contains(inhalt, "Aktivitaet `1` +`1` ZE", "Das Ereignis muss mit Aktivitaets-ID und Verzoegerung im Report stehen");
        Assert.contains(inhalt, "Start 0, Ende 5", "Die rechte Zeitbeschriftung muss Start und Ende zeigen");
        Assert.contains(inhalt, "width=\"56\" height=\"28\" rx=\"4\" ry=\"4\" fill=\"#4e79a7\"", "Die Basisdauer muss mit ihrer eigenen Breite dargestellt werden");
        Assert.contains(inhalt, "pattern id=\"verbrauchterPuffer\"", "Der verbrauchte Puffer muss als eigenes Muster definiert werden");
        Assert.contains(inhalt, "width=\"28\" height=\"28\" rx=\"4\" ry=\"4\" fill=\"url(#verbrauchterPuffer)\" stroke=\"#e15759\" stroke-width=\"1\"", "Der im Puffer verbrauchte Anteil muss als Muster dargestellt werden");
        Assert.contains(inhalt, "width=\"56\" height=\"28\" rx=\"4\" ry=\"4\" fill=\"#f28e2b\"", "Der verbleibende Puffer muss orange dargestellt werden");
        Assert.isFalse(inhalt.contains("Ressourcen-Ausfaelle") || inhalt.contains("Ausfall"), "Der Report darf keine Ressourcenausfaelle mehr enthalten");
        Assert.contains(reportPfad.getFileName().toString(), "gantt-run-testrun.md", "Der Dateiname muss die Lauf-ID enthalten");
    }

    private void groupsEqualStartTimesIntoWaves() throws IOException {
        Path reportOrdner = Files.createTempDirectory("ba-gantt-export-multi-");
        try {
            Datensatz erste = TestDataFactory.datensatz(1, List.of(2, 3), 2, 1, 0, 0);
            Datensatz zweite = TestDataFactory.datensatz(2, List.of(), 2, 1, 0, 0);
            Datensatz dritte = TestDataFactory.datensatz(3, List.of(), 2, 0, 1, 0);
            ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
                List.of(erste, zweite, dritte),
                1,
                1,
                1
            );

            Projektplan plan = new Projektplan("Mehrere Wellen");
            plan.addAktivitaet(TestDataFactory.planAktivitaet(erste)
                .zeit(0, 2, 2)
                .puffer(0, 0)
                .auswahl(1.0, 1)
                .build());
            plan.addAktivitaet(TestDataFactory.planAktivitaet(zweite)
                .zeit(2, 6, 4)
                .puffer(1, 2)
                .auswahl(1.0, 0)
                .build());
            plan.addAktivitaet(TestDataFactory.planAktivitaet(dritte)
                .zeit(2, 4, 2)
                .puffer(0, 0)
                .auswahl(1.0, 0)
                .build());

            List<ProjektEreignis> ereignisse = List.of(new ProjektEreignis(2, 2));
            PlanBewertung bewertung = PlanBewertung.ausPlan(plan, instanz, ereignisse);
            AlgorithmusErgebnis ergebnis = new AlgorithmusErgebnis("Standard", plan, bewertung, plan, bewertung);

            Path reportPfad = GanttDiagrammExport.exportiere(
                reportOrdner,
                "multi",
                instanz,
                ereignisse,
                List.of(ergebnis)
            );

            String inhalt = Files.readString(reportPfad);
            Assert.contains(inhalt, "Welle 1 | ID 1", "Die erste Welle muss beschriftet werden");
            Assert.contains(inhalt, "Welle 2 | ID 2", "Die zweite Welle muss beschriftet werden");
            Assert.contains(inhalt, "Welle 2 | ID 3", "Identische Startzeiten muessen dieselbe Welle erhalten");
            Assert.contains(inhalt, "belegt R1=1, R2=0, R3=0", "Das Ressourcenprofil von Aktivitaet 2 muss erhalten bleiben");
            Assert.contains(inhalt, "belegt R1=0, R2=1, R3=0", "Das Ressourcenprofil von Aktivitaet 3 muss erhalten bleiben");
            Assert.contains(inhalt, "Start 2, Ende 6", "Die rechte Beschriftung muss Start und Ende zeigen");
            Assert.contains(inhalt, "Aktivitaet `2` +`2` ZE", "Das Projekt-Ereignis der zweiten Aktivitaet muss im Report stehen");
            Assert.isFalse(inhalt.contains("Ausfall"), "Der Report darf keine Ausfallmarkierungen enthalten");
        } finally {
            loescheTempVerzeichnis(reportOrdner);
        }
    }

    private void scalesVeryLargeMakespan() throws IOException {
        Path reportOrdner = Files.createTempDirectory("ba-gantt-export-large-");
        try {
            Datensatz aktivitaet = TestDataFactory.datensatz(
                1,
                List.of(),
                Integer.MAX_VALUE,
                1,
                0,
                0
            );
            ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
                List.of(aktivitaet),
                1,
                1,
                1
            );
            Projektplan plan = new Projektplan("Sehr grosse Dauer");
            plan.addAktivitaet(TestDataFactory.planAktivitaet(aktivitaet)
                .zeit(0, Integer.MAX_VALUE, Integer.MAX_VALUE)
                .puffer(0, 0)
                .auswahl(1.0, 0)
                .build());
            PlanBewertung bewertung = PlanBewertung.ausPlan(plan, instanz, List.of());
            AlgorithmusErgebnis ergebnis = new AlgorithmusErgebnis(
                "Standard",
                plan,
                bewertung,
                plan,
                bewertung
            );

            Path reportPfad = GanttDiagrammExport.exportiere(
                reportOrdner,
                "large",
                instanz,
                List.of(),
                List.of(ergebnis)
            );

            String inhalt = Files.readString(reportPfad);
            Assert.contains(inhalt, ">2147483647</text>", "Die maximale Endzeit muss auf der Achse stehen");
            Assert.contains(inhalt, "Bei mehr als 200 Zeiteinheiten wird die Achse proportional komprimiert", "Die adaptive Skalierung muss erklaert werden");
            Assert.isTrue(inhalt.length() < 200_000, "Der Report muss trotz grossem Makespan kompakt bleiben");
        } finally {
            loescheTempVerzeichnis(reportOrdner);
        }
    }

    private void keepsShortActivitiesVisibleOnCompressedAxis() throws IOException {
        Path reportOrdner = Files.createTempDirectory("ba-gantt-export-short-");
        try {
            Datensatz langeAktivitaet = TestDataFactory.datensatz(
                1,
                List.of(2),
                Integer.MAX_VALUE - 1,
                1,
                0,
                0
            );
            Datensatz kurzeAktivitaet = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
            ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
                List.of(langeAktivitaet, kurzeAktivitaet),
                1,
                1,
                1
            );
            Projektplan plan = new Projektplan("Kurzer Abschluss");
            plan.addAktivitaet(TestDataFactory.planAktivitaet(langeAktivitaet)
                .zeit(0, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1)
                .puffer(0, 0)
                .auswahl(1.0, 1)
                .build());
            plan.addAktivitaet(TestDataFactory.planAktivitaet(kurzeAktivitaet)
                .zeit(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 1)
                .puffer(0, 0)
                .auswahl(1.0, 0)
                .build());
            PlanBewertung bewertung = PlanBewertung.ausPlan(plan, instanz, List.of());
            AlgorithmusErgebnis ergebnis = new AlgorithmusErgebnis(
                "Standard",
                plan,
                bewertung,
                plan,
                bewertung
            );

            Path reportPfad = GanttDiagrammExport.exportiere(
                reportOrdner,
                "short",
                instanz,
                List.of(),
                List.of(ergebnis)
            );

            String inhalt = Files.readString(reportPfad);
            Assert.contains(
                inhalt,
                "x=\"5849\" y=\"96\" width=\"1\" height=\"28\"",
                "Die kurze Abschlussaktivitaet muss als mindestens ein Pixel breiter Balken sichtbar bleiben"
            );
        } finally {
            loescheTempVerzeichnis(reportOrdner);
        }
    }

    private void loescheTempVerzeichnis(Path verzeichnis) throws IOException {
        if (!Files.exists(verzeichnis)) {
            return;
        }

        try (Stream<Path> pfade = Files.walk(verzeichnis)) {
            for (Path pfad : pfade.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(pfad);
            }
        }
    }
}

