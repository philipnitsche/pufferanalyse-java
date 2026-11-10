package model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Kennzahlen eines Plans; Auslastung misst reservierte Zeit, nicht produktive Arbeit. */
public final class PlanBewertung {
    private final String planName;

    private final int makespan;
    private final double[] auslastungen;
    private final int gesamterPuffer;
    private final int gesamteVerzoegerung;
    private final int verbrauchterPuffer;
    private final int ereignisUeberhang;
    private final double pufferNutzungsgrad;
    private final double ereignisUeberhangquote;

    public record Auslastung(double... werte) {
        public Auslastung {
            werte = werte.clone();
            for (int k = 0; k < werte.length; k++)
                pruefeNichtNegativeEndlicheQuote(werte[k], "Ressource " + (k + 1));
        }

        @Override
        public double[] werte() {
            return werte.clone();
        }
    }

    public record PufferKennzahlen(
            int gesamterPuffer, int verbrauchterPuffer, double nutzungsgrad) {
        public PufferKennzahlen {
            if (gesamterPuffer < 0 || verbrauchterPuffer < 0) {
                throw new IllegalArgumentException("Pufferkennzahlen duerfen nicht negativ sein");
            }
            pruefeOptionaleNichtNegativeQuote(nutzungsgrad, "Puffer-Nutzungsgrad");
        }
    }

    public record EreignisKennzahlen(
            int gesamteVerzoegerung, int ueberhang, double ueberhangquote) {
        public EreignisKennzahlen {
            if (gesamteVerzoegerung < 0 || ueberhang < 0) {
                throw new IllegalArgumentException("Ereigniskennzahlen duerfen nicht negativ sein");
            }
            pruefeOptionaleNichtNegativeQuote(ueberhangquote, "Ereignis-Ueberhangquote");
        }
    }

    public PlanBewertung(
            String planName,
            int makespan,
            Auslastung auslastung,
            PufferKennzahlen pufferKennzahlen,
            EreignisKennzahlen ereignisKennzahlen) {
        Auslastung sichereAuslastung = Objects.requireNonNull(auslastung, "auslastung");
        PufferKennzahlen sicherePufferKennzahlen =
                Objects.requireNonNull(pufferKennzahlen, "pufferKennzahlen");
        EreignisKennzahlen sichereEreignisKennzahlen =
                Objects.requireNonNull(ereignisKennzahlen, "ereignisKennzahlen");

        this.planName = Objects.requireNonNull(planName, "planName");
        this.makespan = makespan;
        this.auslastungen = sichereAuslastung.werte();
        this.gesamterPuffer = sicherePufferKennzahlen.gesamterPuffer();
        this.gesamteVerzoegerung = sichereEreignisKennzahlen.gesamteVerzoegerung();
        this.verbrauchterPuffer = sicherePufferKennzahlen.verbrauchterPuffer();
        this.ereignisUeberhang = sichereEreignisKennzahlen.ueberhang();
        this.pufferNutzungsgrad = sicherePufferKennzahlen.nutzungsgrad();
        this.ereignisUeberhangquote = sichereEreignisKennzahlen.ueberhangquote();
    }

    /** Berechnet Kennzahlen und prueft, ob Plan und Ereignisliste zusammenpassen. */
    public static PlanBewertung ausPlan(
            Projektplan projektplan,
            ProjektInstanz projektInstanz,
            List<ProjektEreignis> ereignisse) {
        int makespan = projektplan.getMakespan();

        int gesamterPuffer = 0;
        int gesamteVerzoegerung = 0;
        int verbrauchterPuffer = 0;
        int ereignisUeberhang = 0;
        double[] belegteZeit = new double[projektInstanz.getAnzahlRessourcen()];
        Set<Integer> aktivitaetenMitEreignis = new HashSet<>();
        java.util.Map<Integer, Integer> ereigniswerte = new java.util.HashMap<>();

        for (ProjektEreignis ereignis : ereignisse) {
            if (!aktivitaetenMitEreignis.add(ereignis.getAktivitaetId())) {
                throw new IllegalArgumentException(
                        "Mehr als ein Projekt-Ereignis fuer Aktivitaet "
                                + ereignis.getAktivitaetId()
                                + " ist nicht zulaessig");
            }
            ereigniswerte.put(ereignis.getAktivitaetId(), ereignis.getVerzoegerung());
            gesamteVerzoegerung =
                    addiereKennzahl(
                            gesamteVerzoegerung,
                            ereignis.getVerzoegerung(),
                            "gesamte Ereignisverzoegerung");
        }

        // Ressourcen bleiben waehrend der gesamten reservierten Dauer belegt.
        for (PlanAktivitaet planAktivitaet : projektplan.getAktivitaetenSortiert()) {
            if (planAktivitaet.getVerzoegerung()
                    != ereigniswerte.getOrDefault(planAktivitaet.getId(), 0))
                throw new IllegalArgumentException("Ereignisliste und Plan stimmen nicht ueberein");
            if ((long) planAktivitaet.getBasisDauer()
                            + Math.max(planAktivitaet.getPuffer(), planAktivitaet.getVerzoegerung())
                    != planAktivitaet.getEffektiveDauer())
                throw new IllegalArgumentException(
                        "Plan enthaelt eine widerspruechliche effektive Dauer");
            gesamterPuffer =
                    addiereKennzahl(gesamterPuffer, planAktivitaet.getPuffer(), "gesamter Puffer");
            verbrauchterPuffer =
                    addiereKennzahl(
                            verbrauchterPuffer,
                            planAktivitaet.getVerzoegerungImPuffer(),
                            "verbrauchter Puffer");
            ereignisUeberhang =
                    addiereKennzahl(
                            ereignisUeberhang,
                            planAktivitaet.getZusatzverzoegerungNachPuffer(),
                            "Ereignis-Ueberhang");
            for (int k = 0; k < belegteZeit.length; k++)
                belegteZeit[k] +=
                        (double) planAktivitaet.getRessource(k)
                                * planAktivitaet.getEffektiveDauer();
        }

        if ((long) verbrauchterPuffer + ereignisUeberhang != gesamteVerzoegerung)
            throw new IllegalArgumentException(
                    "Ereignisliste ist nicht vollstaendig im Plan enthalten");
        double[] auslastung = new double[belegteZeit.length];
        for (int k = 0; k < auslastung.length; k++)
            auslastung[k] =
                    makespan == 0
                            ? 0
                            : belegteZeit[k]
                                    / ((double) projektInstanz.getKapazitaet(k) * makespan);
        // Bei Nenner 0 bleibt die Quote undefiniert (NaN, in Ausgaben n/a).
        double pufferNutzungsgrad =
                gesamterPuffer == 0 ? Double.NaN : (double) verbrauchterPuffer / gesamterPuffer;
        double ereignisUeberhangquote =
                gesamteVerzoegerung == 0
                        ? Double.NaN
                        : (double) ereignisUeberhang / gesamteVerzoegerung;

        return new PlanBewertung(
                projektplan.getName(),
                makespan,
                new Auslastung(auslastung),
                new PufferKennzahlen(gesamterPuffer, verbrauchterPuffer, pufferNutzungsgrad),
                new EreignisKennzahlen(
                        gesamteVerzoegerung, ereignisUeberhang, ereignisUeberhangquote));
    }

    public String getPlanName() {
        return planName;
    }

    public int getAnzahlRessourcen() {
        return auslastungen.length;
    }

    public double getAuslastung(int k) {
        return auslastungen[k];
    }

    public double getAuslastungR4() {
        return auslastungen[3];
    }

    /** Groesste reservierte Endzeit des Plans. */
    public int getMakespan() {
        return makespan;
    }

    public double getAuslastungR1() {
        return auslastungen[0];
    }

    public double getAuslastungR2() {
        return auslastungen[1];
    }

    public double getAuslastungR3() {
        return auslastungen[2];
    }

    public int getGesamteVerzoegerung() {
        return gesamteVerzoegerung;
    }

    public int getGesamterPuffer() {
        return gesamterPuffer;
    }

    public int getVerbrauchterPuffer() {
        return verbrauchterPuffer;
    }

    /** Summe der lokalen Ueberhaenge; nicht die Verschiebung des Projektendtermins. */
    public int getEreignisUeberhang() {
        return ereignisUeberhang;
    }

    /** Verbrauch geteilt durch geplante Reserve; NaN ohne Reserve. */
    public double getPufferNutzungsgrad() {
        return pufferNutzungsgrad;
    }

    /** Ueberhang geteilt durch Ereigniszusatzdauer; NaN ohne Ereignisse. */
    public double getEreignisUeberhangquote() {
        return ereignisUeberhangquote;
    }

    private static void pruefeNichtNegativeEndlicheQuote(double wert, String name) {
        if (!Double.isFinite(wert) || wert < 0) {
            throw new IllegalArgumentException(
                    name + " muss eine endliche nichtnegative Quote sein");
        }
    }

    private static void pruefeOptionaleNichtNegativeQuote(double wert, String name) {
        if (!Double.isNaN(wert) && (!Double.isFinite(wert) || wert < 0)) {
            throw new IllegalArgumentException(
                    name + " muss eine nichtnegative Quote oder n/a sein");
        }
    }

    private static int addiereKennzahl(int bisherigerWert, int summand, String name) {
        try {
            return Math.addExact(bisherigerWert, summand);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Die Kennzahl '" + name + "' ueberschreitet den unterstuetzten Zahlenbereich",
                    e);
        }
    }
}
