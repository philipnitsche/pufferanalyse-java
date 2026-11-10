package tests.testsupport;

import java.util.ArrayList;
import java.util.List;

import testsupport.Assert;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Prueft den leichtgewichtigen Test-Runner-Baustein ohne echte Konsolenausgabe.
 */
public final class TestRegistryTests implements TestSuite {
    @Override
    public String getName() {
        return "testsupport/TestRegistry";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "meldet Statuszeilen, Stacktrace und Zusammenfassung", this::reportsStatusLinesAndSummary);
    }

    private void reportsStatusLinesAndSummary() {
        List<String> ausgabe = new ArrayList<>();
        TestRegistry registry = new TestRegistry(ausgabe::add);

        registry.add("Suite", "erfolgreich", () -> Assert.isTrue(true, "Der Kontrolltest soll erfolgreich sein"));
        registry.add("Suite", "fehlerhaft", () -> {
            throw new AssertionError("Beabsichtigter Testfehler");
        });

        int fehler = registry.runAll();
        String text = String.join("\n", ausgabe);

        Assert.equals(1, fehler, "Ein fehlschlagender Test muss als Fehler gezaehlt werden");
        Assert.contains(text, "[PASS] Suite :: erfolgreich", "Erfolgreiche Tests muessen gemeldet werden");
        Assert.contains(text, "[FAIL] Suite :: fehlerhaft", "Fehlschlagende Tests muessen gemeldet werden");
        Assert.contains(text, "Beabsichtigter Testfehler", "Der Stacktrace muss die Ursache enthalten");
        Assert.contains(text, "Executed 2 tests: 1 passed, 1 failed.", "Die Zusammenfassung muss stimmen");
    }
}
