package tests.testsupport;

import java.util.List;

import config.PufferKonfiguration;
import model.Datensatz;
import model.PlanAktivitaet;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert die Testdaten-Factory selbst ab, weil viele Fachtests ihre
 * Modellobjekte ueber diese gemeinsame Hilfsschicht erzeugen.
 */
public final class TestDataFactoryTests implements TestSuite {
    @Override
    public String getName() {
        return "testsupport/TestDataFactory";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "liefert Standard-Pufferkonfiguration mit allen Werten", this::exposesStandardBufferConfiguration);
        registry.add(getName(), "baut Planaktivitaeten ueber benannte Fachgruppen", this::buildsPlanActivitiesWithNamedGroups);
    }

    private void exposesStandardBufferConfiguration() {
        PufferKonfiguration konfiguration = TestDataFactory.standardPufferKonfiguration();

        Assert.equals(2, konfiguration.getFesterPuffer(), "Der feste Puffer muss aus der Factory kommen");
        Assert.equals(0.25, konfiguration.getProportionalerFaktor(), 0.000001, "Der proportionale Faktor muss stimmen");
    }

    private void buildsPlanActivitiesWithNamedGroups() {
        Datensatz datensatz = TestDataFactory.datensatz(7, List.of(), 3, 1, 2, 0);

        PlanAktivitaet planAktivitaet = TestDataFactory.planAktivitaet(datensatz)
            .zeit(4, 10, 6)
            .puffer(2, 5)
            .auswahl(1.75, 3)
            .build();

        Assert.equals(7, planAktivitaet.getId(), "Die Aktivitaets-ID muss aus dem Datensatz kommen");
        Assert.equals(4, planAktivitaet.getStartzeit(), "Die Startzeit muss aus der Zeitgruppe kommen");
        Assert.equals(10, planAktivitaet.getEndzeit(), "Die Endzeit muss aus der Zeitgruppe kommen");
        Assert.equals(6, planAktivitaet.getEffektiveDauer(), "Die effektive Dauer muss aus der Zeitgruppe kommen");
        Assert.equals(2, planAktivitaet.getPuffer(), "Der Puffer muss aus der Puffergruppe kommen");
        Assert.equals(5, planAktivitaet.getVerzoegerung(), "Die Verzoegerung muss aus der Puffergruppe kommen");
        Assert.equals(1.75, planAktivitaet.getScoreBeiAuswahl(), 0.000001, "Der Score muss aus der Auswahlgruppe kommen");
        Assert.equals(3, planAktivitaet.getDirekteNachfolgerBeiAuswahl(), "Die direkten Nachfolger muessen aus der Auswahlgruppe kommen");
    }
}
