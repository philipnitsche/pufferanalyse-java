package testsupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import config.ConfigManager;
import config.PufferKonfiguration;
import io.Dateiimport;
import model.Datensatz;
import model.PlanAktivitaet;
import model.ProjektInstanz;
import processing.Daten;
import validation.ProjektValidierung;

/**
 * Erzeugt kompakte Testdaten fuer Modell-, Planungs- und Integrationstests.
 */
public final class TestDataFactory {
    private TestDataFactory() {
    }

    public static Datensatz datensatz(
        int id,
        List<Integer> nachfolger,
        int dauer,
        int ressource1,
        int ressource2,
        int ressource3
    ) {
        return new Datensatz(
            id,
            new ArrayList<>(nachfolger),
            new ArrayList<>(),
            dauer,
            ressource1,
            ressource2,
            ressource3
        );
    }

    public static List<Datensatz> mitVorgaengern(List<Datensatz> datensaetze) {
        // Testfaelle definieren meist nur Nachfolger. Fuer dieselbe Datenlage
        // wie im Produktivpfad werden die Rueckkanten hier zentral nachgezogen.
        new Daten(datensaetze).verarbeiteVorgaengerNachfolger();
        return datensaetze;
    }

    public static ProjektInstanz projektInstanzMitVorgaengern(List<Datensatz> datensaetze, int r1, int r2, int r3) {
        return new ProjektInstanz(mitVorgaengern(datensaetze), r1, r2, r3);
    }

    public static PufferKonfiguration standardPufferKonfiguration() {
        return new PufferKonfiguration(2, 0.25, 1, 3);
    }

    public static PlanAktivitaetBuilder planAktivitaet(Datensatz datensatz) {
        return new PlanAktivitaetBuilder(datensatz);
    }

    /**
     * Baut Planaktivitaeten in Tests ueber benannte Fachgruppen statt ueber
     * lange Positionsparameter.
     */
    public static final class PlanAktivitaetBuilder {
        private final Datensatz datensatz;
        private PlanAktivitaet.Zeitplanung zeitplanung;
        private PlanAktivitaet.PufferWirkung pufferWirkung = new PlanAktivitaet.PufferWirkung(0, 0);
        private PlanAktivitaet.AuswahlBewertung auswahlBewertung = new PlanAktivitaet.AuswahlBewertung(0.0, 0);

        private PlanAktivitaetBuilder(Datensatz datensatz) {
            this.datensatz = datensatz;
        }

        public PlanAktivitaetBuilder zeit(int start, int ende, int effektiveDauer) {
            this.zeitplanung = new PlanAktivitaet.Zeitplanung(start, ende, effektiveDauer);
            return this;
        }

        public PlanAktivitaetBuilder puffer(int puffer, int verzoegerung) {
            this.pufferWirkung = new PlanAktivitaet.PufferWirkung(puffer, verzoegerung);
            return this;
        }

        public PlanAktivitaetBuilder auswahl(double score, int direkteNachfolger) {
            this.auswahlBewertung = new PlanAktivitaet.AuswahlBewertung(score, direkteNachfolger);
            return this;
        }

        public PlanAktivitaet build() {
            if (zeitplanung == null) {
                throw new IllegalStateException("Die Zeitplanung muss fuer Test-Planaktivitaeten gesetzt werden");
            }
            return new PlanAktivitaet(datensatz, zeitplanung, pufferWirkung, auswahlBewertung);
        }
    }

    public static List<Datensatz> ladeKonfigurierteDatensaetze() throws IOException {
        // Nutzt dieselbe Konfiguration wie das Hauptprogramm und eignet sich
        // deshalb fuer Tests mit echten Projektdateien.
        Dateiimport importierer = new Dateiimport();
        importierer.importiere(ConfigManager.getImportPath());
        return mitVorgaengern(importierer.getAlleDaten());
    }

    public static ProjektInstanz ladeKonfigurierteProjektInstanz() throws IOException {
        return ProjektValidierung.pruefeUndErzeugeInstanz(
            ladeKonfigurierteDatensaetze(),
            ConfigManager.getKapazitaeten()
        );
    }
}
