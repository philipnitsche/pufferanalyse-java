package logging;

import java.util.logging.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/** Schreibt Meldungen in Konsole und Logdatei; beide Ausgaben sind getrennt filterbar. */
public final class Logger {
    private static final java.util.logging.Logger JUL_LOGGER =
            java.util.logging.Logger.getLogger(Logger.class.getName());

    // PROGRAM liegt ueber INFO, damit sich Ergebnisse ohne Ablaufmeldungen anzeigen lassen.
    private static final String PROGRAM_LEVEL_NAME = "PROGRAM";
    private static final Level PROGRAM_LEVEL =
            new Level(PROGRAM_LEVEL_NAME, 850) {
                private static final long serialVersionUID = 1L;
            };

    private static final String LOG_DIR = "logs";

    // Gemeinsame Kennung fuer Logdatei und Ergebnisse eines Laufs.
    private static final String RUN_ID =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))
                    + "-"
                    + java.util.UUID.randomUUID().toString();

    private static final String LOG_FILE = "run-" + RUN_ID + ".log";

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_MAGENTA = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";

    private static ConsoleHandler consoleHandler;
    private static FileHandler fileHandler;

    private Logger() {}

    static {
        // Verhindert doppelte Ausgaben ueber den uebergeordneten Logger.
        JUL_LOGGER.setUseParentHandlers(false);

        Arrays.stream(JUL_LOGGER.getHandlers()).forEach(JUL_LOGGER::removeHandler);

        try {
            Formatter colorFormatter =
                    new SimpleFormatter() {
                        @Override
                        public String format(LogRecord logRecord) {
                            String color;

                            switch (logRecord.getLevel().getName()) {
                                case "INFO":
                                    color = ANSI_BLUE;
                                    break;
                                case PROGRAM_LEVEL_NAME:
                                    color = ANSI_CYAN;
                                    break;
                                case "WARNING":
                                    color = ANSI_YELLOW;
                                    break;
                                case "SEVERE":
                                    color = ANSI_RED;
                                    break;
                                case "FINE":
                                    color = ANSI_GREEN;
                                    break;
                                case "CONFIG":
                                    color = ANSI_MAGENTA;
                                    break;
                                default:
                                    color = ANSI_RESET;
                            }

                            return color
                                    + String.format(
                                            "[%s] %s%n",
                                            logRecord.getLevel().getName(), logRecord.getMessage())
                                    + ANSI_RESET;
                        }
                    };

            consoleHandler = new ConsoleHandler();

            consoleHandler.setFormatter(colorFormatter);

            // Initial alle Level erlauben; configureLevels setzt danach den Filter.
            consoleHandler.setLevel(Level.ALL);

            JUL_LOGGER.addHandler(consoleHandler);

            // Erst die Ausgaben filtern, nicht bereits der gemeinsame Logger.
            JUL_LOGGER.setLevel(Level.ALL);

        } catch (Exception e) {
            meldeInitialisierungsfehler(
                    "Fehler beim Initialisieren der Konsolenausgabe des Loggers", e);
        }

        try {
            // Fehler beim Datei-Logging duerfen die Konsolenausgabe nicht verhindern.
            Files.createDirectories(Path.of(LOG_DIR));
            fileHandler = new FileHandler(Path.of(LOG_DIR, LOG_FILE).toString(), false);

            fileHandler.setFormatter(new SimpleFormatter());

            // Initial alle Level erlauben; configureLevels setzt danach den Filter.
            fileHandler.setLevel(Level.ALL);

            JUL_LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            meldeInitialisierungsfehler("Datei-Logging konnte nicht initialisiert werden", e);
        }
    }

    /** Technische Ablaufmeldungen auf INFO-Level. */
    public static void info(String message) {
        JUL_LOGGER.info(message);
    }

    /** Fachliche Ergebnisse auf PROGRAM-Level. */
    public static void program(String message) {
        JUL_LOGGER.log(PROGRAM_LEVEL, message);
    }

    public static void warning(String message) {
        JUL_LOGGER.warning(message);
    }

    public static void error(String message) {
        JUL_LOGGER.severe(message);
    }

    public static void debug(String message) {
        JUL_LOGGER.fine(message);
    }

    public static void config(String message) {
        JUL_LOGGER.config(message);
    }

    /** Abschnittsueberschrift auf INFO-Level. */
    public static void section(String title) {
        JUL_LOGGER.log(Level.INFO, () -> formatiereAbschnitt(title));
    }

    /** Abschnittsueberschrift auf PROGRAM-Level. */
    public static void programSection(String title) {
        JUL_LOGGER.log(PROGRAM_LEVEL, () -> formatiereAbschnitt(title));
    }

    /** Setzt getrennte Filter fuer Konsole und Logdatei. */
    public static void configureLevels(String consoleLevelName, String fileLevelName) {
        Level consoleLevel = parseLevel(consoleLevelName, PROGRAM_LEVEL);
        Level fileLevel = parseLevel(fileLevelName, Level.ALL);

        if (consoleHandler != null) {
            consoleHandler.setLevel(consoleLevel);
        }

        if (fileHandler != null) {
            fileHandler.setLevel(fileLevel);
        }

        Logger.config(
                "Logger-Level gesetzt: Konsole="
                        + consoleLevel.getName()
                        + ", Datei="
                        + fileLevel.getName());
    }

    public static String getRunId() {
        return RUN_ID;
    }

    /** Logdateipfad relativ zum Arbeitsverzeichnis. */
    public static Path getLogFilePath() {
        return Path.of(LOG_DIR, LOG_FILE);
    }

    /** Erkennt auch Aliasnamen wie ERROR; unbekannte oder leere Werte nutzen den Ersatzwert. */
    private static Level parseLevel(String levelName, Level fallback) {
        if (levelName == null || levelName.isBlank()) {
            return fallback;
        }

        switch (levelName.trim().toUpperCase(Locale.ROOT)) {
            case "SEVERE", "ERROR":
                return Level.SEVERE;
            case "WARNING", "WARN":
                return Level.WARNING;
            case PROGRAM_LEVEL_NAME, "RESULT":
                return PROGRAM_LEVEL;
            case "INFO":
                return Level.INFO;
            case "CONFIG":
                return Level.CONFIG;
            case "FINE", "DEBUG":
                return Level.FINE;
            case "ALL":
                return Level.ALL;
            case "OFF":
                return Level.OFF;
            default:
                return fallback;
        }
    }

    private static String formatiereAbschnitt(String title) {
        return "========== " + title + " ==========";
    }

    private static void meldeInitialisierungsfehler(String nachricht, Exception exception) {
        java.util.logging.Logger.getLogger(Logger.class.getName() + ".bootstrap")
                .log(Level.SEVERE, "{0}: {1}", new Object[] {nachricht, exception.getMessage()});
    }
}
