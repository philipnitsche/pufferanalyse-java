package tests.model;

import java.util.Arrays;
import java.util.List;

import model.Datensatz;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Deckt die Verdichtung eines Plans in Kennzahlen ab, damit spaetere
 * Verfahrensvergleiche auf stabilen Metriken beruhen.
 */
public final class PlanBewertungTests implements TestSuite {
    @Override
    public String getName() {
        return "model/PlanBewertung";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "verdichtet Kennzahlen aus einem Plan", this::aggregatesMetrics);
        registry.add(getName(), "lehnt widerspruechliche Ereignislisten ab", () -> {
            var d = TestDataFactory.datensatz(1, List.of(), 2, 1, 0, 0);
            var i = new ProjektInstanz(List.of(d), 1, 1, 1);
            var p = new Projektplan("Widerspruch");
            p.addAktivitaet(TestDataFactory.planAktivitaet(d).zeit(0,2,2).puffer(0,0).build());
            Assert.throwsException(IllegalArgumentException.class, () -> PlanBewertung.ausPlan(p,i,List.of(new ProjektEreignis(1,1))), "stimmen nicht ueberein");
        });
        registry.add(getName(), "liefert Nullauslastung fuer leere Plaene", this::usesZeroLoadForEmptyPlans);
        registry.add(getName(), "lehnt ungueltige Kennzahlen ab", this::rejectsInvalidMetrics);
        registry.add(getName(), "berechnet grosse Auslastungsnenner ohne Ueberlauf", this::handlesLargeUtilizationDenominator);
        registry.add(getName(), "lehnt mehrere Ereignisse je Aktivitaet defensiv ab", this::rejectsMultipleEventsForSameActivity);
        registry.add(getName(), "lehnt eine ueberlaufende Ereignissumme ab", this::rejectsOverflowingEventTotal);
    }

    private void aggregatesMetrics() {
        // Der Test kombiniert bewusst Puffer und Ereignisse, damit die
        // Kennzahlen fuer Pufferverbrauch und Ueberhang gemeinsam verifiziert
        // werden koennen.
        Datensatz a = TestDataFactory.datensatz(1, List.of(2), 4, 1, 0, 1);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 3, 2, 1, 0);
        ProjektInstanz instanz = new ProjektInstanz(TestDataFactory.mitVorgaengern(Arrays.asList(a, b)), 4, 2, 2);

        Projektplan projektplan = new Projektplan("Basisplan");
        projektplan.addAktivitaet(TestDataFactory.planAktivitaet(a)
            .zeit(0, 5, 5)
            .puffer(1, 1)
            .auswahl(1.0, 1)
            .build());
        projektplan.addAktivitaet(TestDataFactory.planAktivitaet(b)
            .zeit(5, 10, 5)
            .puffer(2, 2)
            .auswahl(2.0, 0)
            .build());

        List<ProjektEreignis> ereignisse = Arrays.asList(
            new ProjektEreignis(1, 1),
            new ProjektEreignis(2, 2)
        );
        PlanBewertung bewertung = PlanBewertung.ausPlan(projektplan, instanz, ereignisse);

        Assert.equals("Basisplan", bewertung.getPlanName(), "Der Planname muss uebernommen werden");
        Assert.equals(10, bewertung.getMakespan(), "Der Makespan muss aus dem Projektplan kommen");
        Assert.equals((5.0 + 10.0) / (4.0 * 10.0), bewertung.getAuslastungR1(), 0.000001, "Auslastung R1 muss stimmen");
        Assert.equals(5.0 / (2.0 * 10.0), bewertung.getAuslastungR2(), 0.000001, "Auslastung R2 muss stimmen");
        Assert.equals(5.0 / (2.0 * 10.0), bewertung.getAuslastungR3(), 0.000001, "Auslastung R3 muss stimmen");
        Assert.equals(3, bewertung.getGesamterPuffer(), "Puffer muessen summiert werden");
        Assert.equals(3, bewertung.getGesamteVerzoegerung(), "Ereignisse muessen summiert werden");
        Assert.equals(3, bewertung.getVerbrauchterPuffer(), "Der verbrauchte Puffer muss aus den Planaktivitaeten summiert werden");
        Assert.equals(0, bewertung.getEreignisUeberhang(), "Der Ereignis-Ueberhang muss aus den Planaktivitaeten summiert werden");
        Assert.equals(1.0, bewertung.getPufferNutzungsgrad(), 0.000001, "Der Puffer-Nutzungsgrad muss stimmen");
        Assert.equals(0.0, bewertung.getEreignisUeberhangquote(), 0.000001, "Die Ereignis-Ueberhangquote muss stimmen");
    }

    private void usesZeroLoadForEmptyPlans() {
        // Ein leerer Plan ist wichtig als Randfall, damit Bewertungslogik nicht
        // mit Divisionen durch 0 oder kuenstlichen Restwerten reagiert.
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(TestDataFactory.datensatz(1, List.of(), 2, 1, 1, 0)),
            3,
            2,
            1
        );
        Projektplan projektplan = new Projektplan("Leer");

        PlanBewertung bewertung = PlanBewertung.ausPlan(projektplan, instanz, List.of());

        Assert.equals(0, bewertung.getMakespan(), "Ein leerer Plan muss Makespan 0 haben");
        Assert.equals(0.0, bewertung.getAuslastungR1(), 0.0, "R1-Auslastung muss null sein");
        Assert.equals(0.0, bewertung.getAuslastungR2(), 0.0, "R2-Auslastung muss null sein");
        Assert.equals(0.0, bewertung.getAuslastungR3(), 0.0, "R3-Auslastung muss null sein");
        Assert.equals(0, bewertung.getGesamterPuffer(), "Ein leerer Plan darf keinen Puffer haben");
        Assert.equals(0, bewertung.getGesamteVerzoegerung(), "Ohne Ereignisse darf keine Verzoegerung entstehen");
        Assert.equals(0, bewertung.getVerbrauchterPuffer(), "Ein leerer Plan darf keinen verbrauchten Puffer haben");
        Assert.equals(0, bewertung.getEreignisUeberhang(), "Ein leerer Plan darf keinen Ereignis-Ueberhang haben");
        Assert.isTrue(Double.isNaN(bewertung.getPufferNutzungsgrad()), "Ohne Puffer muss der Nutzungsgrad nicht definiert sein");
        Assert.isTrue(Double.isNaN(bewertung.getEreignisUeberhangquote()), "Ohne Ereignisse muss die Ueberhangquote nicht definiert sein");
    }

    private void rejectsInvalidMetrics() {
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanBewertung.Auslastung(-0.1, 0.0, 0.0),
            "Ressource 1"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanBewertung.Auslastung(Double.POSITIVE_INFINITY, 0.0, 0.0),
            "Ressource 1"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanBewertung.PufferKennzahlen(1, -1, 0.0),
            "Pufferkennzahlen"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanBewertung.EreignisKennzahlen(1, 0, Double.NEGATIVE_INFINITY),
            "Ereignis-Ueberhangquote"
        );
    }

    private void handlesLargeUtilizationDenominator() {
        Datensatz aktivitaet = TestDataFactory.datensatz(
            1,
            List.of(),
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            0,
            0
        );
        ProjektInstanz instanz = new ProjektInstanz(
            List.of(aktivitaet),
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        );
        Projektplan projektplan = new Projektplan("Grosse Werte");
        projektplan.addAktivitaet(TestDataFactory.planAktivitaet(aktivitaet)
            .zeit(0, Integer.MAX_VALUE, Integer.MAX_VALUE)
            .puffer(0, 0)
            .auswahl(1.0, 0)
            .build());

        PlanBewertung bewertung = PlanBewertung.ausPlan(projektplan, instanz, List.of());

        Assert.equals(1.0, bewertung.getAuslastungR1(), 0.000001, "Der grosse Auslastungsnenner darf nicht als int ueberlaufen");
    }

    private void rejectsMultipleEventsForSameActivity() {
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(TestDataFactory.datensatz(1, List.of(), 1, 1, 0, 0)),
            1,
            1,
            1
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> PlanBewertung.ausPlan(
                new Projektplan("Duplikat"),
                instanz,
                List.of(
                    new ProjektEreignis(1, 1),
                    new ProjektEreignis(1, 2)
                )
            ),
            "Mehr als ein Projekt-Ereignis"
        );
    }

    private void rejectsOverflowingEventTotal() {
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(
                TestDataFactory.datensatz(1, List.of(), 1, 1, 0, 0),
                TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0)
            ),
            2,
            1,
            1
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> PlanBewertung.ausPlan(
                new Projektplan("Ueberlauf"),
                instanz,
                List.of(
                    new ProjektEreignis(1, Integer.MAX_VALUE),
                    new ProjektEreignis(2, 1)
                )
            ),
            "gesamte Ereignisverzoegerung"
        );
    }
}

