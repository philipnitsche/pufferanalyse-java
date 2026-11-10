package planning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import config.PufferKonfiguration;
import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import validation.ProjektValidierung;

public final class TiefenabhaengigerPufferPlaner {
    private TiefenabhaengigerPufferPlaner() {}

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
        Map<Integer, Integer> tiefen = ProjektValidierung.berechneTiefen(i);
        int h = tiefen.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Map<Integer, Integer> puffer = new HashMap<>();
        for (Datensatz d : i.getAktivitaeten())
            puffer.put(d.getId(), c.tiefenabhaengigerPuffer(tiefen.get(d.getId()), h));
        return GemeinsamerPlaner.ergaenzePlan(
                standard, e, puffer, name, "Steigende absolute Reserve in ZE");
    }
}
