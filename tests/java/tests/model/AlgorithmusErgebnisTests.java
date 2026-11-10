package tests.model;

import model.AlgorithmusErgebnis;
import model.PlanBewertung;
import model.Projektplan;
import testsupport.Assert;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Prueft die reine Kapselung der beiden Plansichten eines Verfahrens.
 */
public final class AlgorithmusErgebnisTests implements TestSuite {
    @Override
    public String getName() {
        return "model/AlgorithmusErgebnis";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "kapselt beide Plansichten unveraendert", this::exposesViews);
    }

    private void exposesViews() {
        Projektplan basisplan = new Projektplan("Basis");
        Projektplan ereignisplan = new Projektplan("Ereignis");
        PlanBewertung basisbewertung = new PlanBewertung(
            "Basis",
            5,
            new PlanBewertung.Auslastung(0.5, 0.4, 0.3),
            new PlanBewertung.PufferKennzahlen(1, 0, Double.NaN),
            new PlanBewertung.EreignisKennzahlen(0, 0, Double.NaN)
        );
        PlanBewertung ereignisbewertung = new PlanBewertung(
            "Ereignis",
            7,
            new PlanBewertung.Auslastung(0.6, 0.5, 0.4),
            new PlanBewertung.PufferKennzahlen(2, 1, 0.5),
            new PlanBewertung.EreignisKennzahlen(3, 2, 2.0 / 3.0)
        );
        AlgorithmusErgebnis ergebnis = new AlgorithmusErgebnis(
            "Standard",
            basisplan,
            basisbewertung,
            ereignisplan,
            ereignisbewertung
        );

        Assert.equals("Standard", ergebnis.getAlgorithmusName(), "Der Algorithmusname muss gespeichert werden");
        Assert.same(basisplan, ergebnis.getBasisplan(), "Der Basisplan muss zugreifbar sein");
        Assert.same(basisbewertung, ergebnis.getBasisbewertung(), "Die Basisbewertung muss zugreifbar sein");
        Assert.same(ereignisplan, ergebnis.getEreignisplan(), "Der Ereignisplan muss zugreifbar sein");
        Assert.same(ereignisbewertung, ergebnis.getEreignisbewertung(), "Die Ereignisbewertung muss zugreifbar sein");
    }
}
