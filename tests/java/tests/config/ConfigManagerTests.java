package tests.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import config.ConfigManager;
import testsupport.Assert;
import testsupport.TestPaths;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert die Basiskonfiguration des Programms gegen unbeabsichtigte
 * Schluesselaenderungen ab.
 */
public final class ConfigManagerTests implements TestSuite {
    @Override
    public String getName() {
        return "config/ConfigManager";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "liest Werte aus config.properties", this::readsProperties);
    }

    private void readsProperties() throws IOException {
        Properties properties = loadProjectProperties();

        Assert.equals(properties.getProperty("importPath"), ConfigManager.getImportPath(), "Importpfad muss geladen werden");
        Assert.equals(properties.getProperty("projectEventPath"), ConfigManager.getProjectEventPath(), "Projekt-Ereignispfad muss geladen werden");
        Assert.equals(Integer.parseInt(properties.getProperty("R1")), ConfigManager.getR1(), "R1 muss geladen werden");
        Assert.equals(Integer.parseInt(properties.getProperty("R2")), ConfigManager.getR2(), "R2 muss geladen werden");
        Assert.equals(Integer.parseInt(properties.getProperty("R3")), ConfigManager.getR3(), "R3 muss geladen werden");
        Assert.equals(13, ConfigManager.getR4(), "R4 muss geladen werden");
        Assert.equals(properties.getProperty("consoleLogLevel"), ConfigManager.getConsoleLogLevel(), "Console-Level muss geladen werden");
        Assert.equals(properties.getProperty("fileLogLevel"), ConfigManager.getFileLogLevel(), "File-Level muss geladen werden");
    }

    private Properties loadProjectProperties() throws IOException {
        Properties properties = new Properties();
        // Der Test liest dieselbe Datei direkt ein, damit die Rueckgabewerte des
        // ConfigManagers gegen die echte Projektkonfiguration verglichen werden.
        try (InputStream inputStream = Files.newInputStream(TestPaths.projectRoot().resolve("config.properties"))) {
            properties.load(inputStream);
        }
        return properties;
    }
}
