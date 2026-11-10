package planning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.PufferKonfiguration;
import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

/**
 * Reserviert fuer jede Aktivitaet denselben Zeitpuffer, bereits im Basisplan ohne Ereignisse.
 */
public final class FesterPufferPlaner {
    private FesterPufferPlaner() {}

    public static Projektplan planeProjekt(
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            String planName,
            PufferKonfiguration pufferKonfiguration) {
        return planeProjekt(
                projektInstanz,
                ereignisse,
                planName,
                pufferKonfiguration,
                Standard.planeStandardProjekt(projektInstanz, List.of(), "Standardgrundplan"));
    }

    /** Ergaenzt Puffer auf den bereits festgelegten Standardwellen. */
    public static Projektplan planeProjekt(
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse,
            String planName,
            PufferKonfiguration pufferKonfiguration,
            Projektplan standardplan) {
        Map<Integer, Integer> pufferProAktivitaet = new HashMap<>();
        for (Datensatz datensatz : projektInstanz.getAktivitaeten()) {
            pufferProAktivitaet.put(datensatz.getId(), pufferKonfiguration.getFesterPuffer());
        }

        return GemeinsamerPlaner.ergaenzePlan(
                standardplan,
                ereignisse,
                pufferProAktivitaet,
                planName,
                "Fester Puffer, konstant "
                        + pufferKonfiguration.getFesterPuffer()
                        + " ZE pro Aktivitaet");
    }
}
