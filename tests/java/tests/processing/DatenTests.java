package tests.processing;

import java.util.Arrays;
import java.util.List;

import model.Datensatz;
import processing.Daten;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Prueft die reine Aufbereitung der Abhaengigkeiten zwischen Aktivitaeten.
 *
 * Die Tests stellen sicher, dass aus Nachfolgerlisten deterministisch die
 * passenden Vorgaengerlisten entstehen.
 */
public final class DatenTests implements TestSuite {
    @Override
    public String getName() {
        return "processing/Daten";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "ergaenzt Vorgaengerlisten aus Nachfolgern", this::derivesPredecessors);
        registry.add(getName(), "dupliziert Vorgaenger nicht", this::doesNotDuplicatePredecessors);
    }

    private void derivesPredecessors() {
        Datensatz a = TestDataFactory.datensatz(1, Arrays.asList(2, 3), 4, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(4), 3, 1, 1, 0);
        Datensatz c = TestDataFactory.datensatz(3, List.of(4), 2, 0, 1, 1);
        Datensatz d = TestDataFactory.datensatz(4, List.of(), 5, 1, 0, 1);
        List<Datensatz> datensaetze = List.of(a, b, c, d);

        new Daten(datensaetze).verarbeiteVorgaengerNachfolger();

        Assert.listEquals(List.of(), a.getVorgaenger(), "Startknoten darf keine Vorgaenger bekommen");
        Assert.listEquals(List.of(1), b.getVorgaenger(), "Aktivitaet 2 braucht Vorgaenger 1");
        Assert.listEquals(List.of(1), c.getVorgaenger(), "Aktivitaet 3 braucht Vorgaenger 1");
        Assert.listEquals(Arrays.asList(2, 3), d.getVorgaenger(), "Aktivitaet 4 braucht beide Vorgaenger");
    }

    private void doesNotDuplicatePredecessors() {
        Datensatz a = TestDataFactory.datensatz(1, List.of(2), 2, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 2, 1, 0, 0);
        List<Datensatz> datensaetze = List.of(a, b);
        Daten daten = new Daten(datensaetze);

        // Mehrfaches Ausfuehren soll gefahrlos bleiben, damit Import- und
        // Aufbereitungsschritte im Testaufbau wiederholt kombinierbar sind.
        daten.verarbeiteVorgaengerNachfolger();
        daten.verarbeiteVorgaengerNachfolger();

        Assert.listEquals(List.of(1), b.getVorgaenger(), "Doppelte Verarbeitung darf keine Duplikate erzeugen");
    }
}
