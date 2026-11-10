package planning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import config.PufferKonfiguration;
import model.AlgorithmusErgebnis;
import model.PlanBewertung;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;
import validation.ProjektValidierung;

/** Grundstruktur, Reserven und Basisbewertungen werden einmal erzeugt. */
public final class VerfahrensVergleich {
    private final ProjektInstanz instanz;
    private final List<Projektplan> basisplaene;
    private final List<PlanBewertung> basisbewertungen;
    private static final List<String> NAMEN =
            List.of(
                    "Standard",
                    "Fester Puffer",
                    "Proportionaler Puffer",
                    "Tiefenabhängiger Puffer");

    public VerfahrensVergleich(ProjektInstanz instanz, PufferKonfiguration c) {
        this.instanz = instanz;
        Projektplan standard =
                Standard.planeStandardProjekt(instanz, List.of(), "Standard ohne Ereignisse");
        basisplaene =
                List.of(
                        standard,
                        FesterPufferPlaner.planeProjekt(
                                instanz, List.of(), "Fester Puffer ohne Ereignisse", c, standard),
                        ProportionalerPufferPlaner.planeProjekt(
                                instanz,
                                List.of(),
                                "Proportionaler Puffer ohne Ereignisse",
                                c,
                                standard),
                        TiefenabhaengigerPufferPlaner.planeProjekt(
                                instanz,
                                List.of(),
                                "Tiefenabhängiger Puffer ohne Ereignisse",
                                c,
                                standard));
        basisbewertungen =
                basisplaene.stream()
                        .map(p -> PlanBewertung.ausPlan(p, instanz, List.of()))
                        .toList();
    }

    public List<AlgorithmusErgebnis> berechne(List<ProjektEreignis> ereignisse) {
        ProjektValidierung.pruefeEreignisse(ereignisse, instanz);
        List<AlgorithmusErgebnis> result = new ArrayList<>();
        for (int k = 0; k < basisplaene.size(); k++) {
            Projektplan basis = basisplaene.get(k);
            Projektplan event =
                    GemeinsamerPlaner.ergaenzePlan(
                            basis,
                            ereignisse,
                            Map.of(),
                            NAMEN.get(k) + " mit Ereignissen",
                            "Unveraenderte Wellen");
            result.add(
                    new AlgorithmusErgebnis(
                            NAMEN.get(k),
                            basis,
                            basisbewertungen.get(k),
                            event,
                            PlanBewertung.ausPlan(event, instanz, ereignisse)));
        }
        return List.copyOf(result);
    }
}
