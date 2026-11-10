package output;

import java.util.List;

import model.AlgorithmusErgebnis;

/** Standard-Endtermine ohne und mit Ereignissen als gemeinsame Vergleichswerte. */
record StandardReferenz(int basisMakespan, int ereignisMakespan) {
    static StandardReferenz aus(List<AlgorithmusErgebnis> ergebnisse) {
        for (AlgorithmusErgebnis ergebnis : ergebnisse) {
            if ("Standard".equalsIgnoreCase(ergebnis.getAlgorithmusName())) {
                return new StandardReferenz(
                        ergebnis.getBasisbewertung().getMakespan(),
                        ergebnis.getEreignisbewertung().getMakespan());
            }
        }
        throw new IllegalArgumentException(
                "Fuer den Verfahrensvergleich fehlt das Standardergebnis");
    }

    int basisMakespanAufschlag(AlgorithmusErgebnis ergebnis) {
        return ergebnis.getBasisbewertung().getMakespan() - basisMakespan;
    }

    int ereignisMakespanAufschlag(AlgorithmusErgebnis ergebnis) {
        return ergebnis.getEreignisbewertung().getMakespan() - ereignisMakespan;
    }

    int vermiedeneZusaetzlicheStoerungsverzoegerung(AlgorithmusErgebnis ergebnis) {
        int standardDelta = ereignisMakespan - basisMakespan;
        int verfahrensDelta =
                ergebnis.getEreignisbewertung().getMakespan()
                        - ergebnis.getBasisbewertung().getMakespan();
        return standardDelta - verfahrensDelta;
    }

    double stoerungsrobustheitProPufferZe(AlgorithmusErgebnis ergebnis) {
        int gesamtPuffer = ergebnis.getEreignisbewertung().getGesamterPuffer();
        if (gesamtPuffer == 0) {
            return Double.NaN;
        }
        return (double) vermiedeneZusaetzlicheStoerungsverzoegerung(ergebnis) / gesamtPuffer;
    }
}
