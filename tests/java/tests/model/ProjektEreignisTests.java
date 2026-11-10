package tests.model;

import model.ProjektEreignis;
import testsupport.Assert;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert die einzige im Modell zugelassene Ereignisart ab.
 */
public final class ProjektEreignisTests implements TestSuite {
    @Override
    public String getName() {
        return "model/ProjektEreignis";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "liefert Aktivitaets-ID und Verzoegerung", this::exposesValues);
        registry.add(getName(), "besitzt nur den fachlichen Zweiparameter-Konstruktor", this::exposesOnlyDomainConstructor);
        registry.add(getName(), "lehnt ungueltige Ereignisse ab", this::rejectsInvalidValues);
    }

    private void exposesValues() {
        ProjektEreignis ereignis = new ProjektEreignis(3, 2);

        Assert.equals(3, ereignis.getAktivitaetId(), "Die Zielaktivitaet muss gespeichert werden");
        Assert.equals(2, ereignis.getVerzoegerung(), "Die Verzoegerung muss gespeichert werden");
    }

    private void exposesOnlyDomainConstructor() {
        var konstruktoren = ProjektEreignis.class.getConstructors();

        Assert.equals(1, konstruktoren.length, "ProjektEreignis darf nur einen oeffentlichen Konstruktor besitzen");
        Assert.equals(2, konstruktoren[0].getParameterCount(), "Der Konstruktor muss genau zwei Parameter besitzen");
        Assert.equals(int.class, konstruktoren[0].getParameterTypes()[0], "Der erste Parameter muss die Aktivitaets-ID aufnehmen");
        Assert.equals(int.class, konstruktoren[0].getParameterTypes()[1], "Der zweite Parameter muss die Verzoegerung aufnehmen");
        Assert.isFalse(
            java.util.Arrays.stream(ProjektEreignis.class.getMethods())
                .anyMatch(methode -> methode.getName().equals("getBeschreibung")),
            "ProjektEreignis darf keine Beschreibung mehr exponieren"
        );
    }

    private void rejectsInvalidValues() {
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new ProjektEreignis(0, 1),
            "Aktivitaets-ID"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new ProjektEreignis(1, 0),
            "positiv"
        );
    }
}
