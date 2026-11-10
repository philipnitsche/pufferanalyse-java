package tests.model;

import java.util.List;

import model.Datensatz;
import model.PlanAktivitaet;
import model.Projektplan;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Prueft die minimalen Ordnungs- und Zugriffsfunktionen des Projektplans.
 */
public final class ProjektplanTests implements TestSuite {
    @Override
    public String getName() {
        return "model/Projektplan";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "sortiert Aktivitaeten und berechnet Makespan", this::providesScheduleData);
    }

    private void providesScheduleData() {
        Datensatz zweite = TestDataFactory.datensatz(2, List.of(), 3, 2, 1, 0);
        Datensatz erste = TestDataFactory.datensatz(1, List.of(2), 4, 1, 0, 1);
        PlanAktivitaet spaeter = TestDataFactory.planAktivitaet(zweite)
            .zeit(5, 8, 3)
            .puffer(1, 0)
            .auswahl(2.5, 0)
            .build();
        PlanAktivitaet frueher = TestDataFactory.planAktivitaet(erste)
            .zeit(0, 4, 4)
            .puffer(0, 0)
            .auswahl(1.5, 1)
            .build();
        Projektplan projektplan = new Projektplan("Beispielplan");

        projektplan.addAktivitaet(spaeter);
        projektplan.addAktivitaet(frueher);

        List<PlanAktivitaet> sortiert = projektplan.getAktivitaetenSortiert();
        Assert.equals("Beispielplan", projektplan.getName(), "Der Planname muss gespeichert werden");
        Assert.equals(1, sortiert.get(0).getId(), "Die Aktivitaeten muessen nach Startzeit sortiert sein");
        Assert.equals(2, sortiert.get(1).getId(), "Spaetere Aktivitaeten muessen hinten stehen");
        Assert.equals(8, projektplan.getMakespan(), "Der Makespan muss der maximalen Endzeit entsprechen");
        Assert.equals(2, projektplan.getAktivitaetNachId(2).getId(), "Die Aktivitaet muss per ID auffindbar sein");
    }
}
