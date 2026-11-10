package testsupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Hilfswerkzeug zum Mitschneiden von Logausgaben in Tests.
 */
public final class LogCapture {
    private LogCapture() {
    }

    public static List<String> captureMessages(ThrowingRunnable runnable)
        throws IOException, ReflectiveOperationException {
        List<String> messages = new ArrayList<>();
        for (LogRecord logRecord : captureRecords(runnable)) {
            messages.add(logRecord.getMessage());
        }
        return messages;
    }

    public static List<LogRecord> captureRecords(ThrowingRunnable runnable)
        throws IOException, ReflectiveOperationException {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(logging.Logger.class.getName());
        Handler[] originalHandlers = logger.getHandlers();
        Level originalLevel = logger.getLevel();
        RecordingHandler recordingHandler = new RecordingHandler();

        // Fuer die Testdauer werden alle vorhandenen Handler ersetzt, damit
        // nur die waehrend des Aufrufs erzeugten Records gesammelt werden.
        for (Handler handler : originalHandlers) {
            logger.removeHandler(handler);
        }

        logger.addHandler(recordingHandler);
        logger.setLevel(Level.ALL);

        try {
            runnable.run();
            recordingHandler.flush();
            return recordingHandler.getRecords();
        } finally {
            // Der urspruengliche Loggerzustand wird immer wiederhergestellt,
            // damit spaetere Tests keine versteckten Seiteneffekte uebernehmen.
            logger.removeHandler(recordingHandler);
            for (Handler handler : originalHandlers) {
                logger.addHandler(handler);
            }
            logger.setLevel(originalLevel);
        }
    }

    private static final class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        private RecordingHandler() {
            setLevel(Level.ALL);
        }

        @Override
        public void publish(LogRecord logRecord) {
            records.add(logRecord);
        }

        @Override
        public void flush() {
            // Der Handler puffert nur in einer Liste; es gibt keinen externen Stream zu flushen.
        }

        @Override
        public void close() {
            // Der Handler besitzt keine Betriebssystemressourcen, die geschlossen werden muessten.
        }

        private List<LogRecord> getRecords() {
            return new ArrayList<>(records);
        }
    }
}
