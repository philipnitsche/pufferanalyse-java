package model;

import java.util.ArrayList;
import java.util.List;
import config.StudienKonfiguration;
import planning.EreignisGenerator;
import planning.VerfahrensVergleich;

/** Die Zufallsserie ist vom gespeicherten Detailbeispiel getrennt. */
public record EreignisStudie(StudienKonfiguration konfiguration, List<Szenario> szenarien) {
    public EreignisStudie {
        szenarien = List.copyOf(szenarien);
    }

    public record Szenario(
            int nummer,
            long seed,
            List<ProjektEreignis> ereignisse,
            List<AlgorithmusErgebnis> ergebnisse) {
        public Szenario {
            ereignisse = List.copyOf(ereignisse);
            ergebnisse = List.copyOf(ergebnisse);
        }
    }

    public static EreignisStudie berechne(
            ProjektInstanz instanz, StudienKonfiguration c, VerfahrensVergleich vergleich) {
        List<Szenario> result = new ArrayList<>();
        java.util.Random seeds = new java.util.Random(c.startSeed());
        for (int i = 0; i < c.anzahl(); i++) {
            long seed = seeds.nextLong();
            List<ProjektEreignis> events = EreignisGenerator.erzeuge(instanz, c, seed);
            result.add(new Szenario(i + 1, seed, events, vergleich.berechne(events)));
        }
        return new EreignisStudie(c, result);
    }
}
