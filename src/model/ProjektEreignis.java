package model;

/** Positive Zusatzdauer in Zeiteinheiten fuer genau eine Aktivitaet. */
public final class ProjektEreignis {
    private final int aktivitaetId;

    private final int verzoegerung;

    public ProjektEreignis(int aktivitaetId, int verzoegerung) {
        if (aktivitaetId <= 0) {
            throw new IllegalArgumentException(
                    "Die Aktivitaets-ID des Ereignisses muss positiv sein");
        }
        if (verzoegerung <= 0) {
            throw new IllegalArgumentException("Die Ereignisverzoegerung muss positiv sein");
        }
        this.aktivitaetId = aktivitaetId;
        this.verzoegerung = verzoegerung;
    }

    public int getAktivitaetId() {
        return aktivitaetId;
    }

    public int getVerzoegerung() {
        return verzoegerung;
    }
}
