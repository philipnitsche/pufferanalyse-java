import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import config.ConfigManager;
import config.PufferConfigManager;
import config.PufferKonfiguration;
import config.StudienKonfiguration;
import io.Dateiimport;
import io.ProjektEreignisImport;
import logging.Logger;
import model.*;
import output.ErgebnisAusgabe;
import output.ErgebnisExport;
import output.StudienExport;
import planning.*;
import validation.ProjektValidierung;

/** Reproduzierbarer Vergleich zur Terminstabilitaet auf einer festen Wellenstruktur. */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            Logger.configureLevels(
                    ConfigManager.getConsoleLogLevel(), ConfigManager.getFileLogLevel());
            Dateiimport importer = new Dateiimport();
            importer.importiere(ConfigManager.getImportPath());
            int[] kapazitaeten = ConfigManager.getKapazitaeten();
            if (!Arrays.equals(importer.getDateiKapazitaeten(), kapazitaeten))
                throw new IllegalArgumentException(
                        "Konfigurierte Kapazitaeten stimmen nicht mit der PSPLIB-Datei ueberein");
            ProjektInstanz instanz =
                    ProjektValidierung.pruefeUndErzeugeInstanz(
                            importer.getAlleDaten(), kapazitaeten);
            List<ProjektEreignis> ereignisse =
                    new ProjektEreignisImport().importiere(ConfigManager.getProjectEventPath());
            PufferKonfiguration puffer = PufferConfigManager.getKonfiguration();
            StudienKonfiguration studienRegeln = StudienKonfiguration.ausKonfiguration();
            VerfahrensVergleich vergleich = new VerfahrensVergleich(instanz, puffer);
            List<AlgorithmusErgebnis> ergebnisse = vergleich.berechne(ereignisse);
            ErgebnisAusgabe.gibErgebnisAus(instanz, ereignisse, ergebnisse);
            Path ziel = Path.of("reports", "lauf-" + Logger.getRunId());
            ErgebnisExport.exportiere(
                    ziel,
                    instanz,
                    ereignisse,
                    ergebnisse,
                    puffer,
                    Path.of(ConfigManager.getImportPath()),
                    Path.of(ConfigManager.getProjectEventPath()));
            EreignisStudie studie = EreignisStudie.berechne(instanz, studienRegeln, vergleich);
            StudienExport.exportiere(ziel.resolve("studie"), instanz, studie);
            Files.writeString(
                    Path.of("reports", "letzter-lauf.txt"),
                    ziel.toString() + System.lineSeparator());
            Logger.program("Rechendaten und Laufnachweis: " + ziel.toAbsolutePath());
            Logger.program(
                    "Zufallsserie: "
                            + studienRegeln.anzahl()
                            + " Szenarien; getrennt vom Detailbeispiel");
            Logger.program(
                    "Serienauswertung: " + ziel.resolve("studie/auswertung.md").toAbsolutePath());
        } catch (IOException | IllegalArgumentException e) {
            Logger.error(e.getMessage());
            System.exit(1);
        }
    }

    public static List<AlgorithmusErgebnis> berechneVergleich(
            ProjektInstanz instanz, List<ProjektEreignis> ereignisse, PufferKonfiguration c) {
        return new VerfahrensVergleich(instanz, c).berechne(ereignisse);
    }
}
