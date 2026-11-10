package tests.validation;

import java.util.Arrays;
import java.util.List;

import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;
import validation.ProjektValidierung;

/**
 * Deckt die fachliche Vorvalidierung fuer Projekte und Ereignisse ab.
 *
 * Die Tests fokussieren auf die Fehlergrenzen, damit die Validierung
 * die Planung spaeter nur noch mit konsistenten Daten versorgt.
 */
public final class ProjektValidierungTests implements TestSuite {
    @Override
    public String getName() {
        return "validation/ProjektValidierung";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "erzeugt ProjektInstanz fuer gueltige Daten", this::createsProjectInstance);
        registry.add(getName(), "lehnt leere Datensaetze ab", this::rejectsEmptyProjects);
        registry.add(getName(), "lehnt ungueltige Ressourcen ab", this::rejectsInvalidResources);
        registry.add(getName(), "lehnt doppelte Aktivitaets-IDs ab", this::rejectsDuplicateActivityIds);
        registry.add(getName(), "lehnt ungueltige Aktivitaetsdauer ab", this::rejectsInvalidDuration);
        registry.add(getName(), "lehnt Ressourcenbedarf ueber Kapazitaet ab", this::rejectsDemandAboveCapacity);
        registry.add(getName(), "lehnt unbekannte Nachfolger ab", this::rejectsUnknownSuccessors);
        registry.add(getName(), "erkennt Zyklen", this::rejectsCycles);
        registry.add(getName(), "lehnt unbekannte Ereignisaktivitaeten ab", this::eventValidationRejectsUnknownActivities);
        registry.add(getName(), "lehnt mehrere Ereignisse je Aktivitaet ab", this::eventValidationRejectsMultipleEvents);
        registry.add(getName(), "akzeptiert Ereignisse verschiedener Aktivitaeten", this::eventValidationAllowsEventsForDifferentActivities);
    }

    private void createsProjectInstance() {
        List<Datensatz> datensaetze = TestDataFactory.mitVorgaengern(Arrays.asList(
            TestDataFactory.datensatz(1, Arrays.asList(2, 3), 4, 1, 1, 0),
            TestDataFactory.datensatz(2, List.of(4), 3, 2, 1, 1),
            TestDataFactory.datensatz(3, List.of(4), 2, 1, 0, 1),
            TestDataFactory.datensatz(4, List.of(), 5, 1, 1, 1)
        ));

        ProjektInstanz instanz = ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 5, 6, 4);

        Assert.equals(4, instanz.getAktivitaeten().size(), "Alle Aktivitaeten muessen in der Instanz enthalten sein");
        Assert.notNull(instanz.getAktivitaetNachId(3), "Aktivitaet 3 muss ueber die ID-Map erreichbar sein");
        Assert.equals(5, instanz.getKapazitaetR1(), "Kapazitaet R1 muss uebernommen werden");
    }

    private void rejectsEmptyProjects() {
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(List.of(), 1, 1, 1),
            "keine Aktivitaeten"
        );
    }

    private void rejectsInvalidResources() {
        List<Datensatz> datensaetze = TestDataFactory.mitVorgaengern(List.of(
            TestDataFactory.datensatz(1, List.of(), 3, 1, 1, 1)
        ));

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 0, 1, 1),
            "Ressourcenkapazitaeten"
        );

        List<Datensatz> negativeRessourcen = TestDataFactory.mitVorgaengern(List.of(
            TestDataFactory.datensatz(1, List.of(), 3, -1, 1, 1)
        ));
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(negativeRessourcen, 2, 2, 2),
            "negative Ressourcenwerte"
        );
    }

    private void rejectsDuplicateActivityIds() {
        List<Datensatz> datensaetze = Arrays.asList(
            TestDataFactory.datensatz(1, List.of(), 3, 1, 1, 1),
            TestDataFactory.datensatz(1, List.of(), 2, 1, 1, 1)
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 2, 2, 2),
            "Doppelte Aktivitaets-ID"
        );
    }

    private void rejectsInvalidDuration() {
        List<Datensatz> datensaetze = List.of(
            TestDataFactory.datensatz(1, List.of(), 0, 1, 1, 1)
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 2, 2, 2),
            "gueltige Dauer"
        );
    }

    private void rejectsDemandAboveCapacity() {
        List<Datensatz> datensaetze = List.of(
            TestDataFactory.datensatz(1, List.of(), 3, 3, 1, 1)
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 2, 2, 2),
            "mehr Ressourcen"
        );
    }

    private void rejectsUnknownSuccessors() {
        List<Datensatz> datensaetze = List.of(
            TestDataFactory.datensatz(1, List.of(99), 3, 1, 1, 1)
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 2, 2, 2),
            "unbekannten Nachfolger"
        );
    }

    private void rejectsCycles() {
        List<Datensatz> datensaetze = TestDataFactory.mitVorgaengern(Arrays.asList(
            TestDataFactory.datensatz(1, List.of(2), 3, 1, 1, 1),
            TestDataFactory.datensatz(2, List.of(1), 2, 1, 1, 1)
        ));

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 2, 2, 2),
            "Zyklus"
        );
    }

    private void eventValidationRejectsUnknownActivities() {
        ProjektInstanz instanz = createValidProjectInstance();

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeEreignisse(
                List.of(new ProjektEreignis(99, 2)),
                instanz
            ),
            "unbekannte Aktivitaet"
        );
    }

    private void eventValidationRejectsMultipleEvents() {
        ProjektInstanz instanz = createValidProjectInstance();

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> ProjektValidierung.pruefeEreignisse(
                List.of(
                    new ProjektEreignis(1, 1),
                    new ProjektEreignis(1, 2)
                ),
                instanz
            ),
            "Mehr als ein Projekt-Ereignis fuer Aktivitaet 1"
        );
    }

    private void eventValidationAllowsEventsForDifferentActivities() {
        ProjektInstanz instanz = createValidProjectInstance();
        List<ProjektEreignis> ereignisse = List.of(
            new ProjektEreignis(1, 1),
            new ProjektEreignis(2, 2)
        );

        ProjektValidierung.pruefeEreignisse(ereignisse, instanz);

        Assert.equals(2, ereignisse.size(), "Ereignisse verschiedener Aktivitaeten muessen erlaubt bleiben");
    }

    private ProjektInstanz createValidProjectInstance() {
        List<Datensatz> datensaetze = TestDataFactory.mitVorgaengern(Arrays.asList(
            TestDataFactory.datensatz(1, List.of(2), 2, 1, 1, 0),
            TestDataFactory.datensatz(2, List.of(), 3, 1, 0, 1)
        ));
        return ProjektValidierung.pruefeUndErzeugeInstanz(datensaetze, 2, 2, 2);
    }
}

