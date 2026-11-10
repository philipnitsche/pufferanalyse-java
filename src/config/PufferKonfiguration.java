package config;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Verbindliche Regeln: feste Reserve, Daueranteil, tiefenabhaengige, nicht fallende ZE-Reserve. */
public final class PufferKonfiguration {
    private final int fest;
    private final BigDecimal proportional;
    private final int startReserve;
    private final int endReserve;

    public PufferKonfiguration(int fest, double proportional, int startReserve, int endReserve) {
        if (fest < 0)
            throw new IllegalArgumentException("Der feste Puffer darf nicht negativ sein");
        if (!Double.isFinite(proportional) || proportional < 0)
            throw new IllegalArgumentException(
                    "Der proportionale Faktor muss endlich und nichtnegativ sein");
        if (startReserve < 0 || endReserve < startReserve)
            throw new IllegalArgumentException(
                    "Die tiefenabhängige Reserve muss nichtnegativ und nicht fallend sein");
        this.fest = fest;
        this.proportional = BigDecimal.valueOf(proportional);
        this.startReserve = startReserve;
        this.endReserve = endReserve;
    }

    public int getFesterPuffer() {
        return fest;
    }

    public double getProportionalerFaktor() {
        return proportional.doubleValue();
    }

    public int getTiefenabhaengigeStartReserve() {
        return startReserve;
    }

    public int getTiefenabhaengigeEndReserve() {
        return endReserve;
    }

    public int proportionalerPuffer(int dauer) {
        try {
            return proportional
                    .multiply(BigDecimal.valueOf(dauer))
                    .setScale(0, RoundingMode.CEILING)
                    .intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Proportionaler Puffer ueberschreitet den Zahlenbereich", e);
        }
    }

    public int tiefenabhaengigerPuffer(int tiefe, int maximaleTiefe) {
        if (tiefe < 0 || maximaleTiefe < tiefe)
            throw new IllegalArgumentException("Ungueltige Graph-Tiefe");
        if (maximaleTiefe == 0) return startReserve;
        long zaehler = (long) (endReserve - startReserve) * tiefe;
        return Math.toIntExact(startReserve + (zaehler + maximaleTiefe - 1) / maximaleTiefe);
    }
}
