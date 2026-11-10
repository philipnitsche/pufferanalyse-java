package tests.planning;

import java.util.List;
import java.util.Map;

import model.Datensatz;
import model.PlanAktivitaet;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import planning.GemeinsamerPlaner;
import planning.Standard;
import planning.FesterPufferPlaner;
import planning.ProportionalerPufferPlaner;
import planning.TiefenabhaengigerPufferPlaner;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Beschreibt die gemeinsame Kernlogik aller Planer:
 * Pufferverrechnung, Wellenbildung und Ressourcenpruefung.
 */
public final class GemeinsamerPlanerTests implements TestSuite {
    private static final String TESTVERFAHREN = "Testverfahren";

    @Override
    public String getName() {
        return "planning/GemeinsamerPlaner";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "verwendet oeffentliche Planung mit Puffer und Ereignis", this::usesPublicPlannerWithBuffersAndDelays);
        registry.add(getName(), "verrechnet Ereignisse zuerst mit vorhandenem Puffer", this::consumesAvailableBufferBeforeExtendingEnd);
        registry.add(getName(), "startet nur ressourcenpassende Aktivitaeten pro Zeitschritt", this::startsOnlyResourceFittingActivities);
        registry.add(getName(), "startet eine Folgewelle erst nach Abschluss der gesamten Vorwelle", this::waitsForEntireWave);
        registry.add(getName(), "verschiebt bei einem Ereignis die gesamte Folgewelle", this::delaysFollowingWaveWhenEventExtendsAnyActivity);
        registry.add(getName(), "laesst Ereignisse die Prioritaetsfolge nicht veraendern", this::keepsPriorityIndependentFromEvents);
        registry.add(getName(), "behaelt Standardwellen bei allen Puffern und Ereignissen bei", this::keepsStandardWavesForAllVariants);
        registry.add(getName(), "ergaenzt Ereignisse ohne Puffer doppelt zu addieren", this::addsEventsToExistingBufferedPlan);
        registry.add(getName(), "lehnt mehrere Ereignisse je Aktivitaet defensiv ab", this::rejectsMultipleEventsForSameActivity);
        registry.add(getName(), "lehnt eine ueberlaufende effektive Dauer ab", this::rejectsOverflowingEffectiveDuration);
        registry.add(getName(), "lehnt eine ueberlaufende Endzeit ab", this::rejectsOverflowingEndTime);
    }

    private void usesPublicPlannerWithBuffersAndDelays() {
        // Aktivitaet 1 hat Basisdauer 2, Puffer 2 und Verzoegerung 3.
        // Deshalb gilt:
        // - 2 Zeiteinheiten Verzoegerung werden im Puffer absorbiert
        // - 1 Zeiteinheit verlaengert zusaetzlich hinter dem Puffer
        // - effektive Dauer = 2 + max(2, 3) = 5
        Datensatz erste = TestDataFactory.datensatz(1, List.of(2), 2, 1, 0, 0);
        Datensatz zweite = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(erste, zweite), 1, 1, 1);
        Projektplan plan = GemeinsamerPlaner.planeProjekt(
            instanz,
            List.of(new ProjektEreignis(1, 3)),
            Map.of(1, 2, 2, 0),
            "Gemeinsam",
            TESTVERFAHREN
        );

        PlanAktivitaet erstePlanAktivitaet = plan.getAktivitaetNachId(1);
        PlanAktivitaet zweitePlanAktivitaet = plan.getAktivitaetNachId(2);

        Assert.equals(2, erstePlanAktivitaet.getPuffer(), "Der uebergebene Puffer muss verwendet werden");
        Assert.equals(3, erstePlanAktivitaet.getVerzoegerung(), "Das zugeordnete Ereignis muss verwendet werden");
        Assert.equals(5, erstePlanAktivitaet.getEffektiveDauer(), "Ein Ereignis muss zuerst vorhandenen Puffer verbrauchen");
        Assert.equals(2, erstePlanAktivitaet.getVerzoegerungImPuffer(), "Der Puffer muss zunaechst von der Verzoegerung aufgezehrt werden");
        Assert.equals(1, erstePlanAktivitaet.getZusatzverzoegerungNachPuffer(), "Nur der Rest darf das Planende zusaetzlich verlaengern");
        Assert.equals(0.5, erstePlanAktivitaet.getScoreBeiAuswahl(), 0.000001, "Der Entscheidungsquotient muss auf der Basisdauer bleiben und darf nicht durch Puffer oder Ereignisse sinken");
        Assert.equals(5, zweitePlanAktivitaet.getStartzeit(), "Nachfolger duerfen erst nach der effektiven Endzeit starten");
        Assert.equals(6, plan.getMakespan(), "Der Makespan muss aus der oeffentlichen Planung stimmen");
    }

    private void consumesAvailableBufferBeforeExtendingEnd() {
        // Verzoegerung 1 liegt komplett innerhalb des Puffers 3.
        // Die Endzeit bleibt deshalb bei Basisdauer + Puffer.
        Datensatz erste = TestDataFactory.datensatz(1, List.of(2), 2, 1, 0, 0);
        Datensatz zweite = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(erste, zweite), 1, 1, 1);

        Projektplan plan = GemeinsamerPlaner.planeProjekt(
            instanz,
            List.of(new ProjektEreignis(1, 1)),
            Map.of(1, 3, 2, 0),
            "Pufferverbrauch",
            TESTVERFAHREN
        );

        PlanAktivitaet erstePlanAktivitaet = plan.getAktivitaetNachId(1);

        Assert.equals(5, erstePlanAktivitaet.getEffektiveDauer(), "Bei kleinerem Ereignis muss der geplante Puffer die Endzeit begrenzen");
        Assert.equals(1, erstePlanAktivitaet.getVerzoegerungImPuffer(), "Die Stoerung muss innerhalb des Puffers verbraucht werden");
        Assert.equals(2, erstePlanAktivitaet.getVerbleibenderPuffer(), "Nicht verbrauchter Puffer muss erhalten bleiben");
        Assert.equals(0, erstePlanAktivitaet.getZusatzverzoegerungNachPuffer(), "Es darf keine zusaetzliche Verzoegerung hinter dem Puffer entstehen");
        Assert.equals(5, plan.getAktivitaetNachId(2).getStartzeit(), "Nachfolger muessen bis zum Ende des reservierten Puffers warten");
    }

    private void startsOnlyResourceFittingActivities() {
        // Beide Aktivitaeten sind fachlich startbar, aber bei Kapazitaet R1=1
        // passt in derselben Welle immer nur eine von beiden.
        Datensatz kurz = TestDataFactory.datensatz(1, List.of(), 1, 1, 0, 0);
        Datensatz lang = TestDataFactory.datensatz(2, List.of(), 3, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(lang, kurz), 1, 1, 1);

        Projektplan plan = GemeinsamerPlaner.planeProjekt(
            instanz,
            List.of(),
            Map.of(1, 0, 2, 0),
            "Ressourcen",
            TESTVERFAHREN
        );

        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Die kuerzere Aktivitaet mit gleichem Ressourcenverbrauch muss zuerst starten");
        Assert.equals(1, plan.getAktivitaetNachId(2).getStartzeit(), "Die zweite Aktivitaet darf erst nach Freigabe der Ressource starten");
    }

    private void waitsForEntireWave() {
        // 1 und 2 starten gemeinsam. Obwohl 1 bereits bei Zeit 1 endet, darf
        // Nachfolger 3 erst nach dem Ende der gesamten Welle bei Zeit 5 starten.
        Datensatz a = TestDataFactory.datensatz(1, List.of(3), 1, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 5, 0, 1, 0);
        Datensatz c = TestDataFactory.datensatz(3, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b, c), 1, 1, 1);

        Projektplan plan = GemeinsamerPlaner.planeProjekt(
            instanz,
            List.of(),
            Map.of(),
            "Wellenlogik",
            TESTVERFAHREN
        );

        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Die erste Aktivitaet muss in der ersten Welle starten");
        Assert.equals(0, plan.getAktivitaetNachId(2).getStartzeit(), "Die zweite Aktivitaet muss parallel in derselben Welle starten");
        Assert.equals(5, plan.getAktivitaetNachId(3).getStartzeit(), "Die Nachfolgeaktivitaet muss auf den Abschluss der gesamten Vorwelle warten");
        Assert.equals(6, plan.getMakespan(), "Die Wellenbarriere muss im Makespan enthalten sein");
    }

    private void delaysFollowingWaveWhenEventExtendsAnyActivity() {
        // Aktivitaet 2 ist kein Vorgaenger von 3. Ihr Ereignis verlaengert aber
        // die erste Welle und verschiebt deshalb auch den Start von 3.
        Datensatz a = TestDataFactory.datensatz(1, List.of(3), 1, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 2, 0, 1, 0);
        Datensatz c = TestDataFactory.datensatz(3, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b, c), 1, 1, 1);

        Projektplan plan = GemeinsamerPlaner.planeProjekt(
            instanz,
            List.of(new ProjektEreignis(2, 5)),
            Map.of(),
            "Ereignisbedingte Wellenverschiebung",
            TESTVERFAHREN
        );

        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Aktivitaet 1 muss in der ersten Welle starten");
        Assert.equals(0, plan.getAktivitaetNachId(2).getStartzeit(), "Aktivitaet 2 muss in derselben Welle starten");
        Assert.equals(7, plan.getAktivitaetNachId(3).getStartzeit(), "Das Ereignis muss die gesamte Folgewelle verschieben");
        Assert.equals(8, plan.getMakespan(), "Die Ereignisverzoegerung muss sich ueber die Wellenbarriere fortsetzen");
    }

    private void keepsPriorityIndependentFromEvents() {
        Datensatz erste = TestDataFactory.datensatz(1, List.of(), 2, 2, 0, 0);
        Datensatz zweite = TestDataFactory.datensatz(2, List.of(), 2, 2, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(erste, zweite), 2, 1, 1);

        Projektplan plan = GemeinsamerPlaner.planeProjekt(
            instanz,
            List.of(new ProjektEreignis(1, 3)),
            Map.of(),
            "Statische Prioritaet",
            TESTVERFAHREN
        );

        Assert.equals(0, plan.getAktivitaetNachId(1).getStartzeit(), "Das Ereignis darf die ID-Tie-Break-Regel nicht veraendern");
        Assert.equals(5, plan.getAktivitaetNachId(2).getStartzeit(), "Die zweite Aktivitaet darf erst nach der priorisierten Aktivitaet starten");
    }

    private void keepsStandardWavesForAllVariants() {
        // Nach Welle 1 konkurrieren 3 und 4 mit gleichem Score und gleicher Dauer.
        // Unterschiedliche Tiefenpuffer duerfen die Entscheidung 3 vor 4 nicht umkehren.
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(
            TestDataFactory.datensatz(1, List.of(), 1, 1, 0, 0),
            TestDataFactory.datensatz(2, List.of(4), 1, 0, 0, 0),
            TestDataFactory.datensatz(3, List.of(), 4, 1, 0, 0),
            TestDataFactory.datensatz(4, List.of(), 4, 1, 0, 0)
        ), 1, 1, 1);
        var konfiguration = TestDataFactory.standardPufferKonfiguration();
        Projektplan standard = Standard.planeStandardProjekt(instanz, List.of(), "Referenz");
        List<ProjektEreignis> ereignisse = List.of(new ProjektEreignis(2, 10), new ProjektEreignis(3, 8));
        for (List<ProjektEreignis> szenario : List.of(List.<ProjektEreignis>of(), ereignisse)) {
            List<Projektplan> plaene = List.of(
                Standard.planeStandardProjekt(instanz, szenario, "Standard"),
                FesterPufferPlaner.planeProjekt(instanz, szenario, "Fest", konfiguration),
                ProportionalerPufferPlaner.planeProjekt(instanz, szenario, "Proportional", konfiguration),
                TiefenabhaengigerPufferPlaner.planeProjekt(instanz, szenario, "Tiefenabhängig", konfiguration),
                TiefenabhaengigerPufferPlaner.planeProjekt(instanz, szenario, "TiefenabhängigReferenz", konfiguration, standard)
            );
            for (Projektplan plan : plaene) {
                // Vergleich aller Paare prueft sowohl Reihenfolge als auch Parallelitaet.
                for (PlanAktivitaet a : standard.getAktivitaetenSortiert()) {
                    for (PlanAktivitaet b : standard.getAktivitaetenSortiert()) {
                        Assert.equals(
                            Integer.compare(a.getStartzeit(), b.getStartzeit()),
                            Integer.compare(plan.getAktivitaetNachId(a.getId()).getStartzeit(),
                                plan.getAktivitaetNachId(b.getId()).getStartzeit()),
                            "Reihenfolge und Wellen muessen erhalten bleiben: " + plan.getName()
                        );
                    }
                }
            }
        }
        Assert.equals(1, standard.getAktivitaetNachId(3).getStartzeit(), "Die Referenz darf nicht mutiert werden");
    }

    private void addsEventsToExistingBufferedPlan() {
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(
            TestDataFactory.datensatz(1, List.of(2), 2, 1, 0, 0),
            TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0)
        ), 1, 1, 1);
        Projektplan basis = FesterPufferPlaner.planeProjekt(
            instanz, List.of(), "Fest", TestDataFactory.standardPufferKonfiguration()
        );
        Projektplan ereignis = GemeinsamerPlaner.ergaenzePlan(
            basis, List.of(new ProjektEreignis(1, 5)), Map.of(), "Ereignis", TESTVERFAHREN
        );
        Assert.equals(4, basis.getAktivitaetNachId(1).getEndzeit(), "Der Basisplan bleibt unveraendert");
        Assert.equals(2, ereignis.getAktivitaetNachId(1).getPuffer(), "Der bestehende Puffer bleibt erhalten");
        Assert.equals(7, ereignis.getAktivitaetNachId(1).getEndzeit(), "Basisdauer + max(Puffer, Ereignis)");
        Assert.equals(7, ereignis.getAktivitaetNachId(2).getStartzeit(), "Nur der Zeitversatz wird weitergegeben");
        Assert.equals(10, ereignis.getMakespan(), "Der Nachfolger behaelt seinen eigenen Puffer");
    }

    private void rejectsMultipleEventsForSameActivity() {
        Datensatz aktivitaet = TestDataFactory.datensatz(1, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(aktivitaet),
            1,
            1,
            1
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> GemeinsamerPlaner.planeProjekt(
                instanz,
                List.of(
                    new ProjektEreignis(1, 1),
                    new ProjektEreignis(1, 2)
                ),
                Map.of(),
                "Duplikatpruefung",
                TESTVERFAHREN
            ),
            "Mehr als ein Projekt-Ereignis fuer Aktivitaet 1"
        );
    }

    private void rejectsOverflowingEffectiveDuration() {
        Datensatz aktivitaet = TestDataFactory.datensatz(1, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(aktivitaet),
            1,
            1,
            1
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> GemeinsamerPlaner.planeProjekt(
                instanz,
                List.of(new ProjektEreignis(1, Integer.MAX_VALUE)),
                Map.of(),
                "Dauerueberlaufpruefung",
                TESTVERFAHREN
            ),
            "effektive Aktivitaetsdauer"
        );
    }

    private void rejectsOverflowingEndTime() {
        Datensatz erste = TestDataFactory.datensatz(1, List.of(2), Integer.MAX_VALUE, 1, 0, 0);
        Datensatz zweite = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
        ProjektInstanz instanz = TestDataFactory.projektInstanzMitVorgaengern(
            List.of(erste, zweite),
            1,
            1,
            1
        );

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> GemeinsamerPlaner.planeProjekt(
                instanz,
                List.of(),
                Map.of(),
                "Endzeitueberlaufpruefung",
                TESTVERFAHREN
            ),
            "Endzeit der Aktivitaet 2"
        );
    }
}
