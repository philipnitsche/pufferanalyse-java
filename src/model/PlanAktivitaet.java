package model;

import java.util.Objects;

/**
 * Reserviertes Intervall einer Aktivitaet mit Puffer und Ereigniswirkung.
 * Dauer = Basisdauer + max(Puffer, Ereignis); ungenutzte Reserve bleibt belegt.
 */
public final class PlanAktivitaet {
    private final Datensatz datensatz;

    private final int startzeit;
    private final int endzeit;

    private final int effektiveDauer;

    private final int puffer;

    private final int verzoegerung;

    private final double scoreBeiAuswahl;
    private final int direkteNachfolgerBeiAuswahl;

    public record Zeitplanung(int startzeit, int endzeit, int effektiveDauer) {
        public Zeitplanung {
            if (startzeit < 0) {
                throw new IllegalArgumentException("Die Startzeit darf nicht negativ sein");
            }
            if (endzeit < startzeit) {
                throw new IllegalArgumentException(
                        "Die Endzeit darf nicht vor der Startzeit liegen");
            }
            if (effektiveDauer < 0) {
                throw new IllegalArgumentException("Die effektive Dauer darf nicht negativ sein");
            }
            if (effektiveDauer != endzeit - startzeit) {
                throw new IllegalArgumentException(
                        "Die effektive Dauer muss zur Start- und Endzeit passen");
            }
        }
    }

    public record PufferWirkung(int puffer, int verzoegerung) {
        public PufferWirkung {
            if (puffer < 0) {
                throw new IllegalArgumentException("Der Puffer darf nicht negativ sein");
            }
            if (verzoegerung < 0) {
                throw new IllegalArgumentException("Die Verzoegerung darf nicht negativ sein");
            }
        }
    }

    public record AuswahlBewertung(double scoreBeiAuswahl, int direkteNachfolgerBeiAuswahl) {
        public AuswahlBewertung {
            if (!Double.isFinite(scoreBeiAuswahl)) {
                throw new IllegalArgumentException("Der Auswahlscore muss endlich sein");
            }
            if (direkteNachfolgerBeiAuswahl < 0) {
                throw new IllegalArgumentException(
                        "Die Zahl der direkten Nachfolger darf nicht negativ sein");
            }
        }
    }

    /** Zeit- und Bewertungswerte sind fest; der Datensatz wird ohne Kopie referenziert. */
    public PlanAktivitaet(
            Datensatz datensatz,
            Zeitplanung zeitplanung,
            PufferWirkung pufferWirkung,
            AuswahlBewertung auswahlBewertung) {
        Zeitplanung sichereZeitplanung = Objects.requireNonNull(zeitplanung, "zeitplanung");
        PufferWirkung sicherePufferWirkung = Objects.requireNonNull(pufferWirkung, "pufferWirkung");
        AuswahlBewertung sichereAuswahlBewertung =
                Objects.requireNonNull(auswahlBewertung, "auswahlBewertung");

        this.datensatz = Objects.requireNonNull(datensatz, "datensatz");
        this.startzeit = sichereZeitplanung.startzeit();
        this.endzeit = sichereZeitplanung.endzeit();
        this.effektiveDauer = sichereZeitplanung.effektiveDauer();
        this.puffer = sicherePufferWirkung.puffer();
        this.verzoegerung = sicherePufferWirkung.verzoegerung();
        this.scoreBeiAuswahl = sichereAuswahlBewertung.scoreBeiAuswahl();
        this.direkteNachfolgerBeiAuswahl = sichereAuswahlBewertung.direkteNachfolgerBeiAuswahl();
    }

    public int getId() {
        return datensatz.getId();
    }

    public Datensatz getDatensatz() {
        return datensatz;
    }

    public int getStartzeit() {
        return startzeit;
    }

    public int getEndzeit() {
        return endzeit;
    }

    public int getEffektiveDauer() {
        return effektiveDauer;
    }

    public int getBasisDauer() {
        return datensatz.getDauer();
    }

    public int getPuffer() {
        return puffer;
    }

    /** Zusatzdauer des einzigen zugeordneten Ereignisses; ohne Ereignis 0. */
    public int getVerzoegerung() {
        return verzoegerung;
    }

    /** Lokal aufgefangene Zusatzdauer; keine Aussage ueber den Projektendtermin. */
    public int getVerzoegerungImPuffer() {
        return Math.min(puffer, verzoegerung);
    }

    public int getVerbleibenderPuffer() {
        return Math.max(0, puffer - verzoegerung);
    }

    /** Zusatzdauer ueber die lokale Reserve hinaus. */
    public int getZusatzverzoegerungNachPuffer() {
        return Math.max(0, verzoegerung - puffer);
    }

    public double getScoreBeiAuswahl() {
        return scoreBeiAuswahl;
    }

    /** Nachfolgerzahl fuer Gleichstaende bei der Auswahl, einschliesslich End-Dummy-Kante. */
    public int getDirekteNachfolgerBeiAuswahl() {
        return direkteNachfolgerBeiAuswahl;
    }

    public int getAnzahlRessourcen() {
        return datensatz.getAnzahlRessourcen();
    }

    public int getRessource(int k) {
        return datensatz.getRessource(k);
    }

    public int getRessource4() {
        return getRessource(3);
    }

    public int getRessource1() {
        return datensatz.getRessource1();
    }

    public int getRessource2() {
        return datensatz.getRessource2();
    }

    public int getRessource3() {
        return datensatz.getRessource3();
    }
}
