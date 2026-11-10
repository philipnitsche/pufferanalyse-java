package model;

/** Basis- und Ereignisplan eines Verfahrens mit ihren Kennzahlen. */
public class AlgorithmusErgebnis {
    private final String algorithmusName;

    private final Projektplan basisplan;
    private final PlanBewertung basisbewertung;

    private final Projektplan ereignisplan;
    private final PlanBewertung ereignisbewertung;

    public AlgorithmusErgebnis(
            String algorithmusName,
            Projektplan basisplan,
            PlanBewertung basisbewertung,
            Projektplan ereignisplan,
            PlanBewertung ereignisbewertung) {
        this.algorithmusName = algorithmusName;
        this.basisplan = basisplan;
        this.basisbewertung = basisbewertung;
        this.ereignisplan = ereignisplan;
        this.ereignisbewertung = ereignisbewertung;
    }

    public String getAlgorithmusName() {
        return algorithmusName;
    }

    public Projektplan getBasisplan() {
        return basisplan;
    }

    public PlanBewertung getBasisbewertung() {
        return basisbewertung;
    }

    public Projektplan getEreignisplan() {
        return ereignisplan;
    }

    public PlanBewertung getEreignisbewertung() {
        return ereignisbewertung;
    }
}
