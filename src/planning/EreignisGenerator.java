package planning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import config.StudienKonfiguration;
import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;

/** Erst IDs ohne Zuruecklegen ziehen, dann Zusatzdauern vergeben. */
public final class EreignisGenerator {
    public static final String VERSION = "java.util.Random-partial-Fisher-Yates-v1";

    private EreignisGenerator() {}

    public static List<ProjektEreignis> erzeuge(
            ProjektInstanz instanz, StudienKonfiguration c, long seed) {
        List<Integer> ids =
                new ArrayList<>(
                        instanz.getAktivitaeten().stream().map(Datensatz::getId).sorted().toList());
        if (c.betroffeneAktivitaeten() > ids.size())
            throw new IllegalArgumentException(
                    "Mehr betroffene Aktivitaeten als im Projekt vorhanden");
        Random zufall = new Random(seed);
        for (int i = 0; i < c.betroffeneAktivitaeten(); i++) {
            int j = i + zufall.nextInt(ids.size() - i);
            int id = ids.get(i);
            ids.set(i, ids.get(j));
            ids.set(j, id);
        }
        int breite = c.maximaleZusatzdauer() - c.minimaleZusatzdauer() + 1;
        List<ProjektEreignis> result = new ArrayList<>();
        for (int i = 0; i < c.betroffeneAktivitaeten(); i++)
            result.add(
                    new ProjektEreignis(
                            ids.get(i), c.minimaleZusatzdauer() + zufall.nextInt(breite)));
        result.sort(Comparator.comparingInt(ProjektEreignis::getAktivitaetId));
        return List.copyOf(result);
    }
}
