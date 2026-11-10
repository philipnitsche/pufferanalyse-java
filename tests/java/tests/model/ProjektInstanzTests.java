package tests.model;

import java.util.List;

import model.Datensatz;
import model.ProjektInstanz;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert die Vorberechnung der Ressourcen-Normalisierung ab, weil diese
 * Faktoren spaeter direkt die Auswahlheuristik des Planers beeinflussen.
 */
public final class ProjektInstanzTests implements TestSuite {
    @Override
    public String getName() {
        return "model/ProjektInstanz";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "berechnet Zielwert und Normierungsfaktoren", this::calculatesNormalization);
        registry.add(getName(), "lehnt einen kgV-Ueberlauf ab", this::rejectsLcmOverflow);
        registry.add(getName(), "berechnet eine grosse normalisierte Summe als long", this::calculatesLargeNormalizedSum);
    }

    private void calculatesNormalization() {
        List<Datensatz> datensaetze = TestDataFactory.mitVorgaengern(List.of(
            TestDataFactory.datensatz(1, List.of(), 2, 1, 1, 1)
        ));
        ProjektInstanz instanz = new ProjektInstanz(datensaetze, 5, 8, 3);

        Assert.equals(120, instanz.getNormierungsZielwert(), "kgV von 5, 8 und 3 muss 120 sein");
        Assert.equals(24, instanz.getNormierungsFaktorR1(), "Normierungsfaktor R1 muss stimmen");
        Assert.equals(15, instanz.getNormierungsFaktorR2(), "Normierungsfaktor R2 muss stimmen");
        Assert.equals(40, instanz.getNormierungsFaktorR3(), "Normierungsfaktor R3 muss stimmen");
        Assert.equals(48, instanz.normalisiereRessource1(2), "Normalisierung fuer R1 muss stimmen");
        Assert.equals(60, instanz.normalisiereRessource2(4), "Normalisierung fuer R2 muss stimmen");
        Assert.equals(80, instanz.normalisiereRessource3(2), "Normalisierung fuer R3 muss stimmen");
        Assert.equals(188L, instanz.berechneNormalisierteRessourcensumme(2, 4, 2), "Die normierte Summe muss additiv sein");

        List<Datensatz> kopie = instanz.getAktivitaeten();
        kopie.clear();
        Assert.equals(1, instanz.getAktivitaeten().size(), "Rueckgabe der Aktivitaeten muss defensiv kopiert sein");
    }

    private void rejectsLcmOverflow() {
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new ProjektInstanz(List.of(), 46_337, 46_349, 1),
            "kgV"
        );
    }

    private void calculatesLargeNormalizedSum() {
        ProjektInstanz instanz = new ProjektInstanz(
            List.of(),
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        );

        Assert.equals(
            3L * Integer.MAX_VALUE,
            instanz.berechneNormalisierteRessourcensumme(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            ),
            "Die normalisierte Summe muss den int-Bereich sicher ueberschreiten koennen"
        );
    }
}
