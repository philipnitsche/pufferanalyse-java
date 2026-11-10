package planning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import config.PufferKonfiguration;
import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

public final class ProportionalerPufferPlaner {
    private ProportionalerPufferPlaner() {}

    public static Projektplan planeProjekt(
            ProjektInstanz i, List<ProjektEreignis> e, String name, PufferKonfiguration c) {
        return planeProjekt(
                i, e, name, c, Standard.planeStandardProjekt(i, List.of(), "Standardgrundplan"));
    }

    public static Projektplan planeProjekt(
            ProjektInstanz i,
            List<ProjektEreignis> e,
            String name,
            PufferKonfiguration c,
            Projektplan standard) {

        Map<Integer, Integer> puffer = new HashMap<>();
        for (Datensatz d : i.getAktivitaeten())
            puffer.put(d.getId(), c.proportionalerPuffer(d.getDauer()));
        return GemeinsamerPlaner.ergaenzePlan(
                standard, e, puffer, name, "Aufgerundeter Basisdaueranteil ohne Deckelung");
    }
}
