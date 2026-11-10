package tests.planning;

import java.util.List;

import model.Datensatz;
import model.PlanAktivitaet;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import planning.Standard;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Dokumentiert die Kernregeln des Standardverfahrens ueber fokussierte
 * Szenarien: Score, Vorrangregeln, Ressourcenpruefung und Blockadeerkennung.
 */
public final class StandardTests implements TestSuite {
    @Override
    public String getName() {
        return "planning/Standard";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "berechnet Score ueber normierte Rohstoffanteile und Basisdauer", this::usesNormalizedScore);
        registry.add(getName(), "startet parallele Aktivitaeten und respektiert Vorgaenger", this::plansParallelActivities);
        registry.add(getName(), "beruecksichtigt Verzoegerungen in der effektiven Dauer", this::considersDelays);
        registry.add(getName(), "bevorzugt bei Gleichstand mehr direkte Nachfolger", this::prefersMoreDirectSuccessors);
        registry.add(getName(), "bevorzugt danach kuerzere Dauer", this::prefersShorterDuration);
        registry.add(getName(), "bevorzugt zuletzt die kleinere ID", this::prefersSmallerId);
        registry.add(getName(), "beruecksichtigt Ereignisse verschiedener Aktivitaeten getrennt", this::considersEventsForDifferentActivities);
        registry.add(getName(), "meldet zyklische Blockaden als Fehler", this::rejectsDeadlocks);
    }

    private void usesNormalizedScore() {
        Datensatz datensatz = TestDataFactory.datensatz(2, List.of(), 6, 2, 4, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(datensatz), 5, 8, 3);

        // Erwartung:
        // normierte Summe = (2*24) + (4*15) + (0*40) = 108
        // Score = 108 / 6 = 18
        double score = Standard.berechneScore(instanz, datensatz);

        Assert.equals(18.0, score, 0.000001, "Der Score muss aus den normierten Rohstoffanteilen geteilt durch die Basisdauer berechnet werden");
    }

    private void plansParallelActivities() {
        // Graph: 1 -> 3 und 2 -> 3.
        // 1 und 2 duerfen deshalb gemeinsam starten, 3 erst nach beiden.
        Datensatz a = TestDataFactory.datensatz(1, List.of(3), 3, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(3), 2, 1, 0, 0);
        Datensatz c = TestDataFactory.datensatz(3, List.of(), 4, 1, 0, 0);
        a.addNachfolger(3);
        b.addNachfolger(3);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b, c), 2, 1, 1);

        Projektplan plan = Standard.planeStandardProjekt(projektInstanz, List.of(), "Parallel");

        Assert.equals(7, plan.getMakespan(), "Der Nachfolger darf erst nach beiden Vorgaengern enden");
        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Aktivitaet 1 muss sofort starten");
        Assert.equals(0, plan.getAktivitaetNachId(2).getStartzeit(), "Aktivitaet 2 muss parallel starten");
        Assert.equals(3, plan.getAktivitaetNachId(3).getStartzeit(), "Der Nachfolger darf erst nach Abschluss aller Vorgaenger starten");
    }

    private void considersDelays() {
        // Ohne Puffer gilt im Standardverfahren:
        // effektive Dauer = Basisdauer + gesamte Verzoegerung.
        Datensatz a = TestDataFactory.datensatz(1, List.of(2), 2, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b), 1, 1, 1);

        Projektplan plan = Standard.planeStandardProjekt(
            projektInstanz,
            List.of(new ProjektEreignis(1, 3)),
            "Verzoegerung"
        );

        PlanAktivitaet erste = plan.getAktivitaetNachId(1);
        PlanAktivitaet zweite = plan.getAktivitaetNachId(2);

        Assert.equals(5, erste.getEffektiveDauer(), "Die Verzoegerung muss auf die Dauer aufgeschlagen werden");
        Assert.equals(3, erste.getVerzoegerung(), "Die Planaktivitaet muss die zugeordnete Verzoegerung speichern");
        Assert.equals(5, zweite.getStartzeit(), "Der Nachfolger muss auf die verlaengerte Endzeit warten");
        Assert.equals(6, plan.getMakespan(), "Der Makespan muss die Verzoegerung enthalten");
    }

    private void prefersMoreDirectSuccessors() {
        // Aktivitaet 1 und 2 sind beim Score gleichwertig, aber 1 hat zwei
        // direkte Nachfolger und muss deshalb zuerst gewaehlt werden.
        Datensatz a = TestDataFactory.datensatz(1, List.of(3, 4), 2, 2, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(5), 1, 1, 0, 0);
        Datensatz c = TestDataFactory.datensatz(3, List.of(), 1, 0, 0, 0);
        Datensatz d = TestDataFactory.datensatz(4, List.of(), 1, 0, 0, 0);
        Datensatz e = TestDataFactory.datensatz(5, List.of(), 1, 0, 0, 0);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b, c, d, e), 2, 1, 1);

        Projektplan plan = Standard.planeStandardProjekt(projektInstanz, List.of(), "DirekteNachfolger");

        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Die Aktivitaet mit mehr direkten Nachfolgern muss zuerst starten");
        Assert.equals(2, plan.getAktivitaetNachId(2).getStartzeit(), "Die alternative Aktivitaet darf erst nach Freiwerden der Ressourcen starten");
    }

    private void prefersShorterDuration() {
        // Bei gleichem Score und gleicher Zahl direkter Nachfolger
        // greift die kuerzere Dauer als dritter Tie-Break.
        Datensatz a = TestDataFactory.datensatz(1, List.of(), 3, 3, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b), 3, 1, 1);

        Projektplan plan = Standard.planeStandardProjekt(projektInstanz, List.of(), "Dauer");

        Assert.equals(0, plan.getAktivitaetNachId(2).getStartzeit(), "Bei gleichem Score muss die kuerzere Dauer gewinnen");
        Assert.equals(1, plan.getAktivitaetNachId(1).getStartzeit(), "Die laengere Aktivitaet muss auf freie Ressourcen warten");
    }

    private void prefersSmallerId() {
        // Erst wenn auch Score, direkte Nachfolger und Dauer gleich sind,
        // wird deterministisch ueber die kleinere ID entschieden.
        Datensatz a = TestDataFactory.datensatz(1, List.of(), 2, 2, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 2, 2, 0, 0);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b), 2, 1, 1);

        Projektplan plan = Standard.planeStandardProjekt(projektInstanz, List.of(), "ID");

        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Bei vollstaendigem Gleichstand muss die kleinere ID bevorzugt werden");
        Assert.equals(2, plan.getAktivitaetNachId(2).getStartzeit(), "Die zweite Aktivitaet muss danach folgen");
    }

    private void considersEventsForDifferentActivities() {
        Datensatz eins = TestDataFactory.datensatz(1, List.of(), 4, 1, 0, 0);
        Datensatz zwei = TestDataFactory.datensatz(2, List.of(), 3, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(eins, zwei), 2, 1, 1);
        List<ProjektEreignis> ereignisse = List.of(
            new ProjektEreignis(1, 2),
            new ProjektEreignis(2, 1)
        );

        Projektplan plan = Standard.planeStandardProjekt(instanz, ereignisse, "Mit Ereignis");

        Assert.equals(6, plan.getAktivitaetNachId(1).getEffektiveDauer(), "Das erste Ereignis muss nur Aktivitaet 1 verlaengern");
        Assert.equals(4, plan.getAktivitaetNachId(2).getEffektiveDauer(), "Das zweite Ereignis muss nur Aktivitaet 2 verlaengern");
        Assert.equals(6, plan.getMakespan(), "Ereignisse verschiedener Aktivitaeten muessen getrennt in den Plan eingehen");
    }

    private void rejectsDeadlocks() {
        // Zyklus: 1 wartet auf 2 und 2 wartet auf 1.
        // Damit kann keine erste Welle gebildet werden.
        Datensatz a = TestDataFactory.datensatz(1, List.of(2), 1, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(1), 1, 1, 0, 0);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b), 1, 1, 1);

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> Standard.planeStandardProjekt(projektInstanz, List.of(), "Blockade"),
            "Planung blockiert"
        );
    }
}
