package tests.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import logging.Logger;
import model.AlgorithmusErgebnis;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import output.ErgebnisAusgabe;
import testsupport.Assert;
import testsupport.LogCapture;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert die verdichtete PROGRAM-Ausgabe gegen fachliche Rueckschritte ab.
 *
 * Der Test prueft bewusst die erklaerende Ergebnisdarstellung und nicht die
 * zugrunde liegende Planungslogik, die bereits in den Planer-Tests abgedeckt ist.
 */
public final class ErgebnisAusgabeTests implements TestSuite {
    @Override
    public String getName() {
        return "output/ErgebnisAusgabe";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "gibt Ergebnisse als Smoke-Test aus", this::printsResults);
    }

    private void printsResults() throws IOException, ReflectiveOperationException {
        // Ein minimales Szenario reicht hier aus, weil die Ausgabeerklaerung
        // im Vordergrund steht und nicht ein weiterer Volltest der Planung.
        Path reportOrdner = Files.createTempDirectory("ba-gantt-output-");
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(TestDataFactory.datensatz(1, List.of(), 2, 1, 0, 0)),
            1,
            1,
            1
        );
        Projektplan basisplan = new Projektplan("Ausgabe ohne Ereignisse");
        basisplan.addAktivitaet(TestDataFactory.planAktivitaet(
            TestDataFactory.datensatz(1, List.of(), 2, 1, 0, 0)
        )
            .zeit(0, 2, 2)
            .puffer(0, 0)
            .auswahl(0.5, 0)
            .build());
        Projektplan ereignisplan = new Projektplan("Ausgabe mit Ereignissen");
        ereignisplan.addAktivitaet(TestDataFactory.planAktivitaet(
            TestDataFactory.datensatz(1, List.of(), 2, 1, 0, 0)
        )
            .zeit(0, 4, 4)
            .puffer(0, 2)
            .auswahl(0.5, 0)
            .build());
        List<ProjektEreignis> ereignisse = List.of(new ProjektEreignis(1, 2));
        PlanBewertung basisbewertung = PlanBewertung.ausPlan(basisplan, instanz, List.of());
        PlanBewertung ereignisbewertung = PlanBewertung.ausPlan(ereignisplan, instanz, ereignisse);
        AlgorithmusErgebnis ergebnis = new AlgorithmusErgebnis(
            "Standard",
            basisplan,
            basisbewertung,
            ereignisplan,
            ereignisbewertung
        );

        Logger.getRunId();
        final Path[] reportPfad = new Path[1];
        List<String> messages = LogCapture.captureMessages(() ->
            reportPfad[0] = ErgebnisAusgabe.gibErgebnisAus(instanz, ereignisse, List.of(ergebnis), reportOrdner)
        );

        Assert.isTrue(messages.contains("========== Programmergebnis =========="), "Der Ergebnisblock muss ausgegeben werden");
        Assert.isTrue(messages.contains("Kapazitaet R1=1"), "Die Kapazitaeten muessen ausgegeben werden");
        Assert.isTrue(messages.contains("Projekt-Ereignisse:"), "Projekt-Ereignisse muessen ausgegeben werden");
        Assert.isTrue(
            messages.stream().anyMatch(message -> message.contains("Projekt-Ereignis auf Aktivitaet 1 +2 ZE")),
            "Das Projekt-Ereignis muss mit Aktivitaets-ID und Verzoegerung ausgegeben werden"
        );
        Assert.isFalse(
            messages.stream().anyMatch(message -> message.contains("Ressourcen-Ausfaelle") || message.contains("Ausfall")),
            "Die Ausgabe darf keine Ressourcenausfaelle mehr enthalten"
        );
        Assert.isTrue(messages.contains("========== Bewertungsmetriken =========="), "Die allgemeinen Erklaerungen der Metriken muessen ausgegeben werden");
        Assert.isTrue(
            messages.stream().anyMatch(message -> message.contains("Stoerungsrobustheit pro Puffer-ZE =")),
            "Die Stoerungsrobustheit muss erklaert werden"
        );
        Assert.isTrue(
            messages.contains("Pufferbudgets sind absichtlich verfahrensspezifisch und duerfen voneinander abweichen."),
            "Die verfahrensspezifischen Pufferbudgets muessen erklaert werden"
        );
        Assert.isTrue(
            messages.contains("Die Stoerungsrobustheit normalisiert nur den Schutz gegen zusaetzliche Ereigniswirkung; sie bewertet nicht die gesamte Projektdauer."),
            "Die Grenze der Stoerungsrobustheit muss erklaert werden"
        );
        Assert.isTrue(
            messages.stream().anyMatch(message -> message.startsWith("Reservierte Ressourcenauslastung =")),
            "Die reservierte Ressourcenauslastung muss definiert werden"
        );
        Assert.isTrue(
            messages.contains("Planungslogik: prioritaetsregelbasierte wellengebundene Planung; die naechste Welle startet nach dem spaetesten Ende der aktuellen Welle."),
            "Die Left-Shift-Planungslogik muss erklaert werden"
        );
        Assert.isTrue(messages.stream().anyMatch(message -> message.contains("Relative Projektverlaengerung durch Ereignisse =")), "Die relative Projektverlaengerung muss erklaert werden");
        Assert.isTrue(
            messages.stream().anyMatch(message -> message.startsWith("Vermiedene zusaetzliche Stoerungsverzoegerung =")),
            "Die differentielle Stoerungskennzahl muss konsistent erklaert werden"
        );
        Assert.isTrue(
            messages.contains("Vergleichsbloecke stellen immer Basis- und Ereignisplan desselben Verfahrens gegenueber; nur die Kennzahlen zur Standard-Referenz ziehen das Standardverfahren zusaetzlich heran."),
            "Die Vergleichsrichtung muss im Ergebnisblock erklaert werden"
        );
        Assert.isTrue(messages.contains("Standard (ohne Ereignisse):"), "Das Basisszenario muss ausgegeben werden");
        Assert.isTrue(messages.contains("- Makespan: 2"), "Der Makespan muss ausgegeben werden");
        Assert.isTrue(messages.contains("- Puffer-Nutzungsgrad: n/a"), "Nicht definierte Quoten muessen als n/a ausgegeben werden");
        Assert.isTrue(
            messages.contains("- Lesart Aktivitaetszeile: Dauer = BasisDauer + Puffer + ZusaetzlichNachPuffer; Verzoegerung ist die gesamte Stoerung, VerzoegerungImPuffer der davon im reservierten Puffer aufgefangene Anteil."),
            "Die Aktivitaetszeilen muessen im Ergebnisblock erklaert werden"
        );
        Assert.isTrue(messages.contains("Vergleich fuer Standard:"), "Der Vergleichsblock muss ausgegeben werden");
        Assert.isTrue(messages.contains("- Basis-Makespan-Aufschlag gegenueber Standard: 0"), "Der Basis-Aufschlag muss ausgegeben werden");
        Assert.isTrue(messages.contains("- Ereignis-Makespan-Aufschlag gegenueber Standard: 0"), "Der Ereignis-Aufschlag muss ausgegeben werden");
        Assert.isTrue(messages.contains("- Relative Projektverlaengerung durch Ereignisse: 100,00%"), "Die relative Projektverlaengerung muss im Vergleichsblock ausgegeben werden");
        Assert.isTrue(messages.contains("- Stoerungsrobustheit pro Puffer-ZE: n/a"), "Ohne Puffer muss die Stoerungsrobustheit als n/a ausgegeben werden");
        Assert.notNull(reportPfad[0], "Es muss ein Gantt-Report geschrieben werden");
        Assert.isTrue(Files.exists(reportPfad[0]), "Die Gantt-Datei muss existieren");
        Assert.isTrue(messages.stream().anyMatch(message -> message.contains("Gantt-Report:")), "Der Pfad des Gantt-Reports muss protokolliert werden");
    }
}


