package config;

/** Einfache Randregeln der reproduzierbaren Ereignisserie. */
public record StudienKonfiguration(
        int anzahl,
        long startSeed,
        int betroffeneAktivitaeten,
        int minimaleZusatzdauer,
        int maximaleZusatzdauer) {
    public StudienKonfiguration {
        if (anzahl <= 0 || betroffeneAktivitaeten <= 0)
            throw new IllegalArgumentException(
                    "Szenariozahl und Ereigniszahl muessen positiv sein");
        if (minimaleZusatzdauer <= 0
                || maximaleZusatzdauer < minimaleZusatzdauer
                || (long) maximaleZusatzdauer - minimaleZusatzdauer + 1 > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Ungueltiger Bereich der Zusatzdauern");
    }

    public static StudienKonfiguration ausKonfiguration() {
        return new StudienKonfiguration(
                Integer.parseInt(ConfigManager.wert("studyScenarioCount")),
                Long.parseLong(ConfigManager.wert("studyStartSeed")),
                Integer.parseInt(ConfigManager.wert("studyEventCount")),
                Integer.parseInt(ConfigManager.wert("studyMinDelay")),
                Integer.parseInt(ConfigManager.wert("studyMaxDelay")));
    }
}
