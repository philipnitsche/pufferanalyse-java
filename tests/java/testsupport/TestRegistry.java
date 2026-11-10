package testsupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Sammelt und fuehrt die einzelnen Testfaelle in definierter Reihenfolge aus.
 *
 * Die Registry bleibt bewusst minimal, damit die Tests keine fremde
 * Infrastruktur benoetigen und ihre Meldungen direkt lesbar bleiben.
 */
public final class TestRegistry {
    private static final java.util.logging.Logger TEST_LOGGER =
        java.util.logging.Logger.getLogger(TestRegistry.class.getName());

    static {
        TEST_LOGGER.setUseParentHandlers(false);
        Arrays.stream(TEST_LOGGER.getHandlers()).forEach(TEST_LOGGER::removeHandler);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord logRecord) {
                return logRecord.getMessage() + System.lineSeparator();
            }
        });
        TEST_LOGGER.addHandler(handler);
        TEST_LOGGER.setLevel(Level.INFO);
    }

    private final List<TestCase> testCases = new ArrayList<>();
    private final TestReporter reporter;

    public TestRegistry() {
        this(new LoggerTestReporter());
    }

    public TestRegistry(TestReporter reporter) {
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    public void add(String suite, String name, ThrowingRunnable runnable) {
        testCases.add(new TestCase(suite, name, runnable));
    }

    public int runAll() {
        int failures = 0;

        for (TestCase testCase : testCases) {
            try {
                testCase.runnable.run();
                reporter.writeLine("[PASS] " + testCase.suite + " :: " + testCase.name);
            } catch (Throwable throwable) {
                failures++;
                reporter.writeLine("[FAIL] " + testCase.suite + " :: " + testCase.name);
                reporter.writeLine(stackTraceAlsText(throwable));
            }
        }

        reporter.writeLine("");
        reporter.writeLine(
            "Executed " + testCases.size() + " tests: "
                + (testCases.size() - failures) + " passed, "
                + failures + " failed."
        );

        return failures;
    }

    public interface TestReporter {
        void writeLine(String line);
    }

    private static String stackTraceAlsText(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static final class LoggerTestReporter implements TestReporter {
        @Override
        public void writeLine(String line) {
            TEST_LOGGER.info(line);
        }
    }

    private static final class TestCase {
        private final String suite;
        private final String name;
        private final ThrowingRunnable runnable;

        private TestCase(String suite, String name, ThrowingRunnable runnable) {
            this.suite = suite;
            this.name = name;
            this.runnable = runnable;
        }
    }
}
