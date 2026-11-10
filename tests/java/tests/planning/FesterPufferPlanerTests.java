package tests.planning;

import java.util.List;

import config.PufferKonfiguration;
import model.Datensatz;
import model.ProjektInstanz;
import model.Projektplan;
import planning.FesterPufferPlaner;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Zeigt, dass das feste Verfahren nur eine konstante Pufferkarte vorbereitet
 * und die gemeinsame Planungslogik anschliessend unveraendert laeuft.
 */
public final class FesterPufferPlanerTests implements TestSuite {
    @Override
    public String getName() {
        return "planning/FesterPufferPlaner";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "vergibt denselben Puffer an alle Aktivitaeten", this::appliesConstantBuffer);
    }

    private void appliesConstantBuffer() {
        // Beide Aktivitaeten muessen denselben Puffer erhalten, obwohl sie
        // unterschiedliche Rollen im Netzplan haben.
        Datensatz a = TestDataFactory.datensatz(1, List.of(2), 2, 1, 0, 0);
        Datensatz b = TestDataFactory.datensatz(2, List.of(), 1, 1, 0, 0);
        ProjektInstanz projektInstanz = TestDataFactory.projektInstanzMitVorgaengern(List.of(a, b), 1, 1, 1);
        PufferKonfiguration konfiguration = new PufferKonfiguration(2, 0.25, 1, 3);

        Projektplan plan = FesterPufferPlaner.planeProjekt(projektInstanz, List.of(), "Fest", konfiguration);

        Assert.equals(2, plan.getAktivitaetNachId(1).getPuffer(), "Alle Aktivitaeten muessen denselben festen Puffer erhalten");
        Assert.equals(2, plan.getAktivitaetNachId(2).getPuffer(), "Auch Nachfolger muessen denselben festen Puffer erhalten");
        Assert.equals(7, plan.getMakespan(), "Der Makespan muss die konstanten Puffer enthalten");
    }
}
