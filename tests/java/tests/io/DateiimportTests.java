package tests.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import io.Dateiimport;
import testsupport.Assert;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/** Prueft den ausschliesslichen PSPLIB-Projektimport. */
public final class DateiimportTests implements TestSuite {
    private static final String PROJEKT = "data/reference/psplib/j601_1.sm";
    public String getName() { return "io/Dateiimport"; }
    public void register(TestRegistry r) {
        r.add(getName(), "liest 60 echte PSPLIB-Aktivitaeten und vier Ressourcen", this::liestPsplib);
        r.add(getName(), "ersetzt Importdaten und entfernt alte Daten nach Fehler", this::ersetztImportdaten);
        r.add(getName(), "lehnt andere Projektformate ab", this::lehntAndereFormateAb);
        r.add(getName(), "lehnt leere PSPLIB-Dateien ab", this::lehntLeereDateiAb);
    }
    private void liestPsplib() throws IOException {
        Dateiimport imp = new Dateiimport(); imp.importiere(PROJEKT);
        Assert.equals(60, imp.getAlleDaten().size(), "60 echte Aktivitaeten");
        Assert.listEquals(List.of(13,11,12,13), java.util.Arrays.stream(imp.getDateiKapazitaeten()).boxed().toList(), "Vier Kapazitaeten");
        var a = imp.getDatenNachId(2);
        Assert.equals(8, a.getDauer(), "Basisdauer");
        Assert.listEquals(List.of(5,10,15), a.getNachfolger(), "Nachfolger");
        Assert.listEquals(List.of(10,0,0,0), java.util.Arrays.stream(a.getRessourcen()).boxed().toList(), "Vier Bedarfe");
        Assert.isNull(imp.getDatenNachId(1), "Start-Dummy entfernt");
        Assert.isNull(imp.getDatenNachId(62), "End-Dummy entfernt");
        Assert.notNull(imp.getDatenNachId(61), "Letzte echte ID erhalten");
    }
    private void ersetztImportdaten() throws IOException {
        Dateiimport imp = new Dateiimport();
        imp.importiere("data/reference/psplib/j301_1.sm");
        Assert.equals(30, imp.getAlleDaten().size(), "Erstes Projekt");
        imp.importiere(PROJEKT);
        Assert.equals(60, imp.getAlleDaten().size(), "Neuer Import ersetzt alten");
        Assert.throwsException(IllegalArgumentException.class, () -> imp.importiere("projekt.csv"), ".sm");
        Assert.equals(0, imp.getAlleDaten().size(), "Nach Fehler keine alten Aktivitaeten");
        Assert.isNull(imp.getDateiKapazitaeten(), "Nach Fehler keine alten Kapazitaeten");
    }
    private void lehntAndereFormateAb() throws IOException {
        for (String endung : List.of(".csv", ".txt")) {
            Path p = Files.createTempFile("projekt-format-", endung);
            try {
                // Auch gueltiger PSPLIB-Inhalt oeffnet keinen zweiten Dateiformatpfad.
                Files.copy(Path.of(PROJEKT), p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Assert.throwsException(IllegalArgumentException.class, () -> new Dateiimport().importiere(p.toString()), ".sm");
            } finally { Files.deleteIfExists(p); }
        }
    }
    private void lehntLeereDateiAb() throws IOException {
        Path p = Files.createTempFile("projekt-leer-", ".sm");
        try {
            Assert.throwsException(IllegalArgumentException.class, () -> new Dateiimport().importiere(p.toString()), "Unvollstaendige PSPLIB-Struktur");
        } finally { Files.deleteIfExists(p); }
    }
}
