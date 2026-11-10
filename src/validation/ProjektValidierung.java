package validation;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Datensatz;
import model.ProjektEreignis;
import model.ProjektInstanz;
import processing.Daten;

public final class ProjektValidierung {
    private ProjektValidierung() {}

    public static ProjektInstanz pruefeUndErzeugeInstanz(
            List<Datensatz> daten, int... kapazitaeten) {
        if (daten.isEmpty())
            throw new IllegalArgumentException("Es wurden keine Aktivitaeten importiert");
        ProjektInstanz instanz = new ProjektInstanz(daten, kapazitaeten);
        for (Datensatz d : daten) {
            if (d.getDauer() <= 0)
                throw new IllegalArgumentException(
                        "Aktivitaet " + d.getId() + " hat keine gueltige Dauer");
            if (d.getAnzahlRessourcen() != kapazitaeten.length)
                throw new IllegalArgumentException(
                        "Ressourcenanzahl passt nicht bei Aktivitaet " + d.getId());
            for (int k = 0; k < kapazitaeten.length; k++) {
                if (d.getRessource(k) < 0)
                    throw new IllegalArgumentException(
                            "Aktivitaet " + d.getId() + " enthaelt negative Ressourcenwerte");
                if (d.getRessource(k) > kapazitaeten[k])
                    throw new IllegalArgumentException(
                            "Aktivitaet "
                                    + d.getId()
                                    + " benoetigt mehr Ressourcen als insgesamt verfuegbar sind");
            }
        }
        new Daten(daten).verarbeiteVorgaengerNachfolger();
        berechneTiefen(instanz);
        return instanz;
    }

    /** Berechnet Tiefen ohne Rekursion; jede Aktivitaet und Kante wird einmal verarbeitet. */
    public static Map<Integer, Integer> berechneTiefen(ProjektInstanz instanz) {
        Map<Integer, Integer> eingang = new HashMap<>();
        Map<Integer, Integer> tiefen = new HashMap<>();
        ArrayDeque<Datensatz> bereit = new ArrayDeque<>();
        for (Datensatz d : instanz.getAktivitaeten()) {
            eingang.put(d.getId(), d.getVorgaenger().size());
            tiefen.put(d.getId(), 0);
            if (d.getVorgaenger().isEmpty()) bereit.add(d);
        }
        int besucht = 0;
        while (!bereit.isEmpty()) {
            Datensatz d = bereit.remove();
            besucht++;
            for (int id : d.getNachfolger()) {
                Datensatz ziel = instanz.getAktivitaetNachId(id);
                if (ziel == null)
                    throw new IllegalArgumentException("Unbekannter Nachfolger " + id);
                tiefen.merge(id, tiefen.get(d.getId()) + 1, Math::max);
                if (eingang.merge(id, -1, Integer::sum) == 0) bereit.add(ziel);
            }
        }
        if (besucht != eingang.size())
            throw new IllegalArgumentException(
                    "Der Aktivitaetsgraph enthaelt mindestens einen Zyklus");
        return tiefen;
    }

    public static void pruefeEreignisse(List<ProjektEreignis> ereignisse, ProjektInstanz instanz) {
        Set<Integer> ids = new HashSet<>();
        for (ProjektEreignis e : ereignisse) {
            if (instanz.getAktivitaetNachId(e.getAktivitaetId()) == null)
                throw new IllegalArgumentException(
                        "Projekt-Ereignis verweist auf unbekannte Aktivitaet "
                                + e.getAktivitaetId());
            if (e.getVerzoegerung() <= 0)
                throw new IllegalArgumentException(
                        "Projekt-Ereignis hat keine positive Verzoegerung");
            if (!ids.add(e.getAktivitaetId()))
                throw new IllegalArgumentException(
                        "Mehr als ein Projekt-Ereignis fuer Aktivitaet "
                                + e.getAktivitaetId()
                                + " ist nicht zulaessig");
        }
    }
}
