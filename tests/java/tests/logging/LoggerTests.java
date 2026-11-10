package tests.logging;

import java.io.IOException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import logging.Logger;
import testsupport.Assert;
import testsupport.LogCapture;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Deckt die wichtigsten Sichtbarkeits- und Hilfsfunktionen des Projektloggers ab.
 */
public final class LoggerTests implements TestSuite {
    @Override
    public String getName() {
        return "logging/Logger";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "wendet Alias-Loglevel auf Handler an", this::appliesAliasLevelsToHandlers);
        registry.add(getName(), "verwendet Fallback-Level fuer unbekannte Werte", this::usesFallbackLevelsForUnknownValues);
        registry.add(getName(), "liefert die erwarteten Log-Nachrichten", this::emitsExpectedMessages);
        registry.add(getName(), "stellt LogCapture nach Exceptions wieder her", this::restoresLogCaptureAfterException);
    }

    private void appliesAliasLevelsToHandlers() {
        // Dieser Test prueft bewusst die echte Handler-Konfiguration statt nur
        // Rueckgabewerte, weil genau dort spaeter entschieden wird, was in
        // Konsole und Datei ueberhaupt sichtbar bleibt.
        java.util.logging.Logger basisLogger = java.util.logging.Logger.getLogger(Logger.class.getName());
        Handler consoleHandler = findeHandler(basisLogger.getHandlers(), ConsoleHandler.class);
        Handler fileHandler = findeHandler(basisLogger.getHandlers(), FileHandler.class);

        Assert.notNull(consoleHandler, "Ein ConsoleHandler muss vorhanden sein");
        Assert.notNull(fileHandler, "Ein FileHandler muss vorhanden sein");

        Logger.configureLevels("DEBUG", "WARN");

        Assert.equals(Level.FINE, consoleHandler.getLevel(), "DEBUG muss auf FINE abgebildet werden");
        Assert.equals(Level.WARNING, fileHandler.getLevel(), "WARN muss auf WARNING abgebildet werden");

        Logger.configureLevels("OFF", "OFF");
    }

    private void usesFallbackLevelsForUnknownValues() {
        java.util.logging.Logger basisLogger = java.util.logging.Logger.getLogger(Logger.class.getName());
        Handler consoleHandler = findeHandler(basisLogger.getHandlers(), ConsoleHandler.class);
        Handler fileHandler = findeHandler(basisLogger.getHandlers(), FileHandler.class);

        Logger.configureLevels("UNBEKANNT", " ");

        Assert.equals("PROGRAM", consoleHandler.getLevel().getName(), "Unbekannte Konsolen-Level muessen auf PROGRAM fallen");
        Assert.equals(Level.ALL, fileHandler.getLevel(), "Leere Datei-Level muessen auf ALL fallen");

        Logger.configureLevels("OFF", "OFF");
    }

    private void emitsExpectedMessages() throws IOException, ReflectiveOperationException {
        List<String> messages = LogCapture.captureMessages(() -> {
            Logger.info("info");
            Logger.program("program");
            Logger.warning("warning");
            Logger.error("error");
            Logger.debug("debug");
            Logger.config("config");
            Logger.section("Abschnitt");
            Logger.programSection("Programmabschnitt");
        });

        Assert.isTrue(messages.contains("info"), "INFO-Nachrichten muessen protokolliert werden");
        Assert.isTrue(messages.contains("program"), "PROGRAM-Nachrichten muessen protokolliert werden");
        Assert.isTrue(messages.contains("warning"), "WARNING-Nachrichten muessen protokolliert werden");
        Assert.isTrue(messages.contains("error"), "SEVERE-Nachrichten muessen protokolliert werden");
        Assert.isTrue(messages.contains("debug"), "DEBUG-Nachrichten muessen protokolliert werden");
        Assert.isTrue(messages.contains("config"), "CONFIG-Nachrichten muessen protokolliert werden");
        Assert.isTrue(messages.contains("========== Abschnitt =========="), "Sektionen muessen formatiert protokolliert werden");
        Assert.isTrue(messages.contains("========== Programmabschnitt =========="), "Programmsektionen muessen formatiert protokolliert werden");
    }

    private void restoresLogCaptureAfterException() {
        java.util.logging.Logger basisLogger = java.util.logging.Logger.getLogger(Logger.class.getName());
        Handler[] originalHandlers = basisLogger.getHandlers();
        Level originalLevel = basisLogger.getLevel();

        Assert.throwsException(
            RuntimeException.class,
            () -> LogCapture.captureMessages(() -> {
                throw new RuntimeException("Testfehler");
            }),
            "Testfehler"
        );

        Handler[] restoredHandlers = basisLogger.getHandlers();
        Assert.equals(originalHandlers.length, restoredHandlers.length, "LogCapture muss alle Handler wiederherstellen");
        if (originalHandlers.length > 0) {
            Assert.same(originalHandlers[0], restoredHandlers[0], "Der urspruengliche Handler muss erhalten bleiben");
        }
        Assert.equals(originalLevel, basisLogger.getLevel(), "LogCapture muss das urspruengliche Logger-Level wiederherstellen");
    }

    private Handler findeHandler(Handler[] handlers, Class<? extends Handler> typ) {
        for (Handler handler : handlers) {
            if (typ.isInstance(handler)) {
                return handler;
            }
        }
        return null;
    }
}
