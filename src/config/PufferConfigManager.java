package config;

/** Liest die Pufferwerte aus der gemeinsamen Konfiguration. */
public final class PufferConfigManager {
    private PufferConfigManager() {}

    public static PufferKonfiguration getKonfiguration() {
        return new PufferKonfiguration(
                Integer.parseInt(ConfigManager.wert("fixedBuffer")),
                Double.parseDouble(ConfigManager.wert("proportionalFactor")),
                Integer.parseInt(ConfigManager.wert("depthStartReserve")),
                Integer.parseInt(ConfigManager.wert("depthEndReserve")));
    }
}
