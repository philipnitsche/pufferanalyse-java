package tests;

import logging.Logger;
import tests.config.ConfigManagerTests;
import tests.config.PufferConfigManagerTests;
import tests.config.PufferKonfigurationTests;
import tests.io.DateiimportTests;
import tests.io.ProjektEreignisImportTests;
import tests.logging.LoggerTests;
import tests.model.AlgorithmusErgebnisTests;
import tests.model.DatensatzTests;
import tests.model.PlanAktivitaetTests;
import tests.model.PlanBewertungTests;
import tests.model.ProjektEreignisTests;
import tests.model.ProjektInstanzTests;
import tests.model.ProjektplanTests;
import tests.output.ErgebnisAusgabeTests;
import tests.output.GanttDiagrammExportTests;
import tests.planning.TiefenabhaengigerPufferPlanerTests;
import tests.planning.FesterPufferPlanerTests;
import tests.planning.GemeinsamerPlanerTests;
import tests.planning.ProportionalerPufferPlanerTests;
import tests.planning.StandardTests;
import tests.processing.DatenTests;
import tests.testsupport.TestDataFactoryTests;
import tests.testsupport.TestRegistryTests;
import tests.validation.ProjektValidierungTests;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/**
 * Startet die komplette leichtgewichtige Test-Suite ohne externes Framework.
 *
 * Der Runner bleibt absichtlich klein, damit die Tests in jeder Umgebung
 * mit denselben Regeln und ohne Zusatzabhaengigkeiten laufen.
 */
public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) {
        Logger.configureLevels("OFF", "OFF");

        TestRegistry registry = new TestRegistry();
        TestSuite[] suites = new TestSuite[] {
            new ConfigManagerTests(),
            new PufferConfigManagerTests(),
            new PufferKonfigurationTests(),
            new DateiimportTests(),
            new ProjektEreignisImportTests(),
            new LoggerTests(),
            new DatensatzTests(),
            new ProjektInstanzTests(),
            new PlanAktivitaetTests(),
            new ProjektplanTests(),
            new PlanBewertungTests(),
            new AlgorithmusErgebnisTests(),
            new ProjektEreignisTests(),
            new ErgebnisAusgabeTests(),
            new GanttDiagrammExportTests(),
            new StandardTests(),
            new GemeinsamerPlanerTests(),
            new FesterPufferPlanerTests(),
            new ProportionalerPufferPlanerTests(),
            new TiefenabhaengigerPufferPlanerTests(),
            new DatenTests(),
            new TestDataFactoryTests(),
            new TestRegistryTests(),
            new ProjektValidierungTests(),
            new ReferenzfallTests(),
            new StudienTests(),
            new MainTests()
        };

        for (TestSuite suite : suites) {
            suite.register(registry);
        }

        int failures = registry.runAll();
        if (failures > 0) {
            System.exit(1);
        }
    }
}

