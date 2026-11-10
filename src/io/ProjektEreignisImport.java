package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import logging.Logger;
import model.ProjektEreignis;

/** Liest Ereignisse im CSV-Format AktivitaetID;Verzoegerung. */
public class ProjektEreignisImport {
    private static final String ERWARTETE_KOPFZEILE = "AktivitaetID;Verzoegerung";

    /** Eine leere Datei liefert eine leere Ereignisliste. */
    public List<ProjektEreignis> importiere(String dateiname) throws IOException {
        List<ProjektEreignis> ereignisse = new ArrayList<>();

        try (BufferedReader reader =
                new BufferedReader(
                        new FileReader(dateiname, java.nio.charset.StandardCharsets.UTF_8))) {
            String kopfzeile = reader.readLine();
            if (kopfzeile == null) {
                return ereignisse;
            }
            if (!ERWARTETE_KOPFZEILE.equals(kopfzeile.replace("\uFEFF", "").trim())) {
                throw new IllegalArgumentException(
                        "Die Kopfzeile der Projekt-Ereignisdatei muss exakt '"
                                + ERWARTETE_KOPFZEILE
                                + "' entsprechen");
            }

            String zeile;
            int zeilennummer = 1;

            while ((zeile = reader.readLine()) != null) {
                zeilennummer++;
                ProjektEreignis ereignis = verarbeiteZeile(zeile, zeilennummer);
                ereignisse.add(ereignis);
            }
        }

        return ereignisse;
    }

    private ProjektEreignis verarbeiteZeile(String zeile, int zeilennummer) {
        String[] teile = zeile.split(";", -1);
        if (teile.length != 2) {
            throw new IllegalArgumentException(
                    "Projekt-Ereignis-Zeile "
                            + zeilennummer
                            + " muss genau AktivitaetID und Verzoegerung enthalten");
        }
        int aktivitaetId = Integer.parseInt(teile[0].trim());
        int verzoegerung = Integer.parseInt(teile[1].trim());

        Logger.debug(
                "Projekt-Ereignis-Zeile "
                        + zeilennummer
                        + " erfolgreich gelesen: Aktivitaet "
                        + aktivitaetId
                        + ", Verzoegerung "
                        + verzoegerung);

        return new ProjektEreignis(aktivitaetId, verzoegerung);
    }
}
