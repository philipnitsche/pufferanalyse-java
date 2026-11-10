package tests.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.ProjektEreignisImport;
import model.ProjektEreignis;
import testsupport.Assert;
import testsupport.TestPaths;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Sichert das CSV-Parsing fuer aktivitaetsbezogene Projekt-Ereignisse ab.
 */
public final class ProjektEreignisImportTests implements TestSuite {
    @Override
    public String getName() {
        return "io/ProjektEreignisImport";
    }

    @Override
    public void register(TestRegistry registry) {
        registry.add(getName(), "liest gueltige Projekt-Ereignisse", this::readsValidData);
        registry.add(getName(), "lehnt eine abweichende Kopfzeile ab", this::rejectsUnexpectedHeader);
        registry.add(getName(), "lehnt zusaetzliche CSV-Spalten ab", this::rejectsAdditionalColumns);
        registry.add(getName(), "liefert fuer leere CSV-Dateien keine Projekt-Ereignisse", this::returnsNoEventsForEmptyFile);
    }

    private void readsValidData() throws IOException {
        ProjektEreignisImport importierer = new ProjektEreignisImport();
        List<ProjektEreignis> ereignisse = importierer.importiere(
            TestPaths.resource("io", "project-events-valid.csv").toString()
        );

        Assert.equals(2, ereignisse.size(), "Zwei gueltige Projekt-Ereignisse muessen importiert werden");
        Assert.equals(1, ereignisse.get(0).getAktivitaetId(), "Die Aktivitaets-ID muss gelesen werden");
        Assert.equals(2, ereignisse.get(0).getVerzoegerung(), "Die Verzoegerung muss gelesen werden");
        Assert.equals(3, ereignisse.get(1).getAktivitaetId(), "Die zweite Aktivitaets-ID muss gelesen werden");
        Assert.equals(1, ereignisse.get(1).getVerzoegerung(), "Die zweite Verzoegerung muss gelesen werden");
    }

    private void rejectsAdditionalColumns() throws IOException {
        Path dateiMitZusatzspalte = Files.createTempFile("ba-projekt-ereignisse-extra-column-", ".csv");
        try {
            Files.writeString(
                dateiMitZusatzspalte,
                "AktivitaetID;Verzoegerung\n1;2;UnerlaubteBeschreibung\n"
            );

            ProjektEreignisImport importierer = new ProjektEreignisImport();
            Assert.throwsException(
                IllegalArgumentException.class,
                () -> importierer.importiere(dateiMitZusatzspalte.toString()),
                "genau AktivitaetID und Verzoegerung"
            );
        } finally {
            Files.deleteIfExists(dateiMitZusatzspalte);
        }
    }

    private void rejectsUnexpectedHeader() throws IOException {
        Path dateiMitFalscherKopfzeile = Files.createTempFile("ba-projekt-ereignisse-invalid-header-", ".csv");
        try {
            Files.writeString(
                dateiMitFalscherKopfzeile,
                "AktivitaetID;Verzoegerung;Beschreibung\n1;2\n"
            );

            ProjektEreignisImport importierer = new ProjektEreignisImport();
            Assert.throwsException(
                IllegalArgumentException.class,
                () -> importierer.importiere(dateiMitFalscherKopfzeile.toString()),
                "AktivitaetID;Verzoegerung"
            );
        } finally {
            Files.deleteIfExists(dateiMitFalscherKopfzeile);
        }
    }

    private void returnsNoEventsForEmptyFile() throws IOException {
        Path leereDatei = Files.createTempFile("ba-projekt-ereignisse-empty-", ".csv");
        try {
            ProjektEreignisImport importierer = new ProjektEreignisImport();
            List<ProjektEreignis> ereignisse = importierer.importiere(leereDatei.toString());

            Assert.equals(0, ereignisse.size(), "Eine leere CSV-Datei darf keine Projekt-Ereignisse erzeugen");
        } finally {
            Files.deleteIfExists(leereDatei);
        }
    }
}
