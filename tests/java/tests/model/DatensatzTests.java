package tests.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.Datensatz;
import testsupport.Assert;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert die einfache Beziehungsverwaltung im Datensatzmodell ab, weil viele
 * spaetere Verarbeitungsschritte auf stabilen Vor- und Nachfolgerlisten
 * aufbauen.
 */
public final class DatensatzTests implements TestSuite {
    @Override
    public String getName() {
        return "model/Datensatz";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "verwaltet Nachfolger und Vorgaenger sicher", this::managesRelations);
    }

    private void managesRelations() {
        Datensatz datensatz = new Datensatz(
            7,
            new ArrayList<>(Arrays.asList(8)),
            new ArrayList<>(List.of(3)),
            5,
            1,
            2,
            3
        );

        Assert.equals(Integer.valueOf(8), datensatz.getNachfolgerAt(0), "Erster Nachfolger muss lesbar sein");
        Assert.isNull(datensatz.getNachfolgerAt(5), "Ungueltige Nachfolgerindizes muessen null liefern");
        Assert.equals(Integer.valueOf(3), datensatz.getVorgaengerAt(0), "Erster Vorgaenger muss lesbar sein");
        Assert.isNull(datensatz.getVorgaengerAt(-1), "Negative Vorgaengerindizes muessen null liefern");

        datensatz.addNachfolger(9);
        datensatz.addNachfolger(9);
        datensatz.removeNachfolger(8);
        datensatz.addVorgaenger(4);
        datensatz.addVorgaenger(4);
        datensatz.removeVorgaenger(3);

        Assert.listEquals(List.of(9), datensatz.getNachfolger(), "Nachfolger duerfen nicht dupliziert werden");
        Assert.listEquals(List.of(4), datensatz.getVorgaenger(), "Vorgaenger duerfen nicht dupliziert werden");
        Assert.equals(5, datensatz.getDauer(), "Dauer muss gespeichert werden");
        Assert.equals(1, datensatz.getRessource1(), "Ressource 1 muss gespeichert werden");
        Assert.equals(2, datensatz.getRessource2(), "Ressource 2 muss gespeichert werden");
        Assert.equals(3, datensatz.getRessource3(), "Ressource 3 muss gespeichert werden");
    }
}
