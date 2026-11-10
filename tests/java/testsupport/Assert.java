package testsupport;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

/**
 * Minimaler Assertion-Satz fuer die projektspezifische Testinfrastruktur.
 */
public final class Assert {
    private static final String EXPECTED_PREFIX = " Expected: ";
    private static final String ACTUAL_PREFIX = ", actual: ";

    private Assert() {
    }

    public static void equals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            fail(message + EXPECTED_PREFIX + expected + ACTUAL_PREFIX + actual);
        }
    }

    public static void equals(int expected, int actual, String message) {
        if (expected != actual) {
            fail(message + EXPECTED_PREFIX + expected + ACTUAL_PREFIX + actual);
        }
    }

    public static void equals(double expected, double actual, double tolerance, String message) {
        if (Double.isNaN(actual) || Math.abs(expected - actual) > tolerance) {
            fail(message + EXPECTED_PREFIX + expected + ACTUAL_PREFIX + actual + ", tolerance: " + tolerance);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            fail(message);
        }
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            fail(message);
        }
    }

    public static void isNull(Object value, String message) {
        if (value != null) {
            fail(message + " Expected null but was " + value);
        }
    }

    public static void listEquals(List<?> expected, List<?> actual, String message) {
        if (!Objects.equals(expected, actual)) {
            fail(message + EXPECTED_PREFIX + expected + ACTUAL_PREFIX + actual);
        }
    }

    public static void contains(String haystack, String needle, String message) {
        if (haystack == null || !haystack.contains(needle)) {
            fail(message + " Expected to contain: " + needle + ACTUAL_PREFIX + haystack);
        }
    }

    public static void same(Object expected, Object actual, String message) {
        if (expected != actual) {
            fail(message + " Expected same instance.");
        }
    }

    public static <T extends Throwable> T throwsException(
        Class<T> expectedType,
        ThrowingRunnable runnable,
        String expectedMessagePart
    ) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            Throwable actualThrowable = unwrap(throwable);
            if (!expectedType.isInstance(actualThrowable)) {
                fail(
                    "Expected exception " + expectedType.getName()
                        + " but got " + actualThrowable.getClass().getName()
                );
            }

            if (expectedMessagePart != null) {
                String message = actualThrowable.getMessage();
                if (message == null || !message.contains(expectedMessagePart)) {
                    fail(
                        "Expected exception message to contain '" + expectedMessagePart
                            + "' but was '" + message + "'"
                    );
                }
            }

            return expectedType.cast(actualThrowable);
        }

        fail("Expected exception " + expectedType.getName() + " but nothing was thrown");
        return null;
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    private static Throwable unwrap(Throwable throwable) {
        // Reflektive Testaufrufe verpacken den eigentlichen Fehler oft in einer
        // InvocationTargetException. Fuer klare Fehlermeldungen wird diese Huelle entfernt.
        if (throwable instanceof InvocationTargetException invocationTargetException
            && invocationTargetException.getCause() != null) {
            return invocationTargetException.getCause();
        }
        return throwable;
    }
}
