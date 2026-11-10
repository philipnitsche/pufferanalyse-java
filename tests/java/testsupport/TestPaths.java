package testsupport;

import java.nio.file.Path;

/**
 * Kapselt die fuer Tests benoetigten Projekt- und Ressourcenpfade zentral.
 */
public final class TestPaths {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    private static final Path RESOURCES_ROOT = PROJECT_ROOT.resolve("tests").resolve("resources");

    private TestPaths() {
    }

    public static Path projectRoot() {
        // Liefert den normalisierten Projektwurzelpfad des aktuellen Testlaufs.
        return PROJECT_ROOT;
    }

    public static Path resource(String... parts) {
        // Baut einen Pfad unterhalb von tests/resources schrittweise auf, damit
        // Aufrufer nur noch die fachlichen Teilsegmente angeben muessen.
        Path path = RESOURCES_ROOT;
        for (String part : parts) {
            // Der Aufbau aus Segmenten haelt die Aufrufe kurz und vermeidet
            // plattformspezifische Trennzeichen in den Testfaellen.
            path = path.resolve(part);
        }
        return path;
    }
}
