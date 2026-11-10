package testsupport;

/**
 * Gemeinsames Minimalinterface fuer alle Test-Suiten.
 *
 * Jede Suite liefert einen Namen und registriert ihre Faelle selbst, damit
 * die Teststruktur ohne externe Framework-Konventionen auskommt.
 */
public interface TestSuite {
    String getName();

    void register(TestRegistry registry);
}
