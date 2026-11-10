package planning;

import java.util.List;

import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

/**
 * Plant ohne Puffer mit der gemeinsamen Wellen- und Ressourcenregel.
 * Prioritaet: hoechster Score, dann mehr direkte Nachfolger,
 * kuerzere Basisdauer und kleinere ID. Ereignisse aendern diese Reihenfolge nicht.
 */
public final class Standard {
    private Standard() {}

    public static Projektplan planeStandardProjekt(
            ProjektInstanz projektInstanz, List<ProjektEreignis> ereignisse, String planName) {
        return GemeinsamerPlaner.planeProjekt(
                projektInstanz,
                ereignisse,
                java.util.Map.of(),
                planName,
                "Standard ohne zusaetzlichen Puffer");
    }

    public static double berechneScore(ProjektInstanz projektInstanz, Datensatz datensatz) {
        return GemeinsamerPlaner.berechneScore(projektInstanz, datensatz);
    }
}
