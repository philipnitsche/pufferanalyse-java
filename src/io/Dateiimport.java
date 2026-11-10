package io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Datensatz;

/** Liest ausschliesslich originale PSPLIB-Single-Mode-Projektdateien. */
public class Dateiimport {
    private List<Datensatz> daten = new ArrayList<>();
    private int[] dateiKapazitaeten;

    public void importiere(String dateiname) throws IOException {
        daten.clear();
        dateiKapazitaeten = null;
        Path pfad = Path.of(dateiname);
        if (!dateiname.toLowerCase(Locale.ROOT).endsWith(".sm"))
            throw new IllegalArgumentException(
                    pfad + ": Projektdatei muss eine PSPLIB-Datei mit der Endung .sm sein");
        PsplibImport.Ergebnis ergebnis = PsplibImport.lese(pfad);
        daten = new ArrayList<>(ergebnis.aktivitaeten());
        dateiKapazitaeten = ergebnis.kapazitaeten().clone();
    }

    public List<Datensatz> getAlleDaten() {
        return new ArrayList<>(daten);
    }

    public Datensatz getDatenNachId(int id) {
        return daten.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    public int[] getDateiKapazitaeten() {
        return dateiKapazitaeten == null ? null : dateiKapazitaeten.clone();
    }
}
