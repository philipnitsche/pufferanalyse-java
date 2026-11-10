package tests.model;

import java.util.List;

import model.Datensatz;
import model.PlanAktivitaet;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Prueft, dass eine eingeplante Aktivitaet ihre aufbereitete Planansicht
 * unveraendert fuer Bewertung und Report bereitstellt.
 */
public final class PlanAktivitaetTests implements TestSuite {
    @Override
    public String getName() {
        return "model/PlanAktivitaet";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "liefert gespeicherte Planinformationen", this::exposesScheduleData);
        registry.add(getName(), "lehnt inkonsistente Planinformationen ab", this::rejectsInconsistentScheduleData);
    }

    private void exposesScheduleData() {
        Datensatz datensatz = TestDataFactory.datensatz(1, List.of(), 4, 1, 0, 1);
        PlanAktivitaet aktivitaet = new PlanAktivitaet(
            datensatz,
            new PlanAktivitaet.Zeitplanung(0, 7, 7),
            new PlanAktivitaet.PufferWirkung(2, 1),
            new PlanAktivitaet.AuswahlBewertung(1.5, 1)
        );

        Assert.equals(1, aktivitaet.getId(), "ID muss aus dem Datensatz kommen");
        Assert.equals(datensatz, aktivitaet.getDatensatz(), "Datensatz muss gespeichert werden");
        Assert.equals(0, aktivitaet.getStartzeit(), "Startzeit muss gespeichert werden");
        Assert.equals(7, aktivitaet.getEndzeit(), "Endzeit muss gespeichert werden");
        Assert.equals(7, aktivitaet.getEffektiveDauer(), "Effektive Dauer muss gespeichert werden");
        Assert.equals(4, aktivitaet.getBasisDauer(), "Basisdauer muss aus dem Datensatz kommen");
        Assert.equals(2, aktivitaet.getPuffer(), "Puffer muss gespeichert werden");
        Assert.equals(1, aktivitaet.getVerzoegerung(), "Verzoegerung muss gespeichert werden");
        Assert.equals(1, aktivitaet.getVerzoegerungImPuffer(), "Verzoegerung im Puffer muss korrekt berechnet werden");
        Assert.equals(1, aktivitaet.getVerbleibenderPuffer(), "Verbleibender Puffer muss korrekt berechnet werden");
        Assert.equals(0, aktivitaet.getZusatzverzoegerungNachPuffer(), "Zusaetzliche Verzoegerung hinter dem Puffer muss korrekt berechnet werden");
        Assert.equals(1.5, aktivitaet.getScoreBeiAuswahl(), 0.000001, "Score muss gespeichert werden");
        Assert.equals(1, aktivitaet.getDirekteNachfolgerBeiAuswahl(), "Direkte Nachfolger muessen gespeichert werden");
        Assert.equals(1, aktivitaet.getRessource1(), "Ressource 1 muss aus dem Datensatz kommen");
        Assert.equals(0, aktivitaet.getRessource2(), "Ressource 2 muss aus dem Datensatz kommen");
        Assert.equals(1, aktivitaet.getRessource3(), "Ressource 3 muss aus dem Datensatz kommen");
    }

    private void rejectsInconsistentScheduleData() {
        Datensatz datensatz = TestDataFactory.datensatz(1, List.of(), 4, 1, 0, 1);

        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanAktivitaet.Zeitplanung(3, 2, 1),
            "Endzeit"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanAktivitaet.Zeitplanung(0, 4, 5),
            "effektive Dauer"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanAktivitaet.PufferWirkung(-1, 0),
            "Puffer"
        );
        Assert.throwsException(
            IllegalArgumentException.class,
            () -> new PlanAktivitaet.AuswahlBewertung(Double.NaN, 0),
            "Auswahlscore"
        );
        Assert.throwsException(
            NullPointerException.class,
            () -> new PlanAktivitaet(
                datensatz,
                null,
                new PlanAktivitaet.PufferWirkung(0, 0),
                new PlanAktivitaet.AuswahlBewertung(1.0, 0)
            ),
            "zeitplanung"
        );
    }
}
