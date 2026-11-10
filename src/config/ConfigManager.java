package config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Die Konfiguration wird einmal gelesen; fehlende Werte werden explizit gemeldet. */
public final class ConfigManager {
    private static Properties properties;

    private ConfigManager() {}

    static String wert(String key) {
        if (properties == null) {
            Properties gelesen = new Properties();
            try (Reader reader =
                    Files.newBufferedReader(Path.of("config.properties"), StandardCharsets.UTF_8)) {
                gelesen.load(reader);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "config.properties konnte nicht geladen werden", e);
            }
            properties = gelesen;
        }
        String value = properties.getProperty(key);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Konfigurationswert fehlt: " + key);
        return value.trim();
    }

    public static String getImportPath() {
        return wert("importPath");
    }

    public static String getProjectEventPath() {
        return wert("projectEventPath");
    }

    public static int getR1() {
        return Integer.parseInt(wert("R1"));
    }

    public static int getR2() {
        return Integer.parseInt(wert("R2"));
    }

    public static int getR3() {
        return Integer.parseInt(wert("R3"));
    }

    public static int getR4() {
        return Integer.parseInt(wert("R4"));
    }

    public static int[] getKapazitaeten() {
        return new int[] {getR1(), getR2(), getR3(), getR4()};
    }

    public static String getConsoleLogLevel() {
        return wert("consoleLogLevel");
    }

    public static String getFileLogLevel() {
        return wert("fileLogLevel");
    }
}
