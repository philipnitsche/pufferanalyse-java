package planning;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import logging.Logger;
import model.Datensatz;
import model.PlanAktivitaet;
import model.ProjektEreignis;
import model.ProjektInstanz;
import model.Projektplan;

/** Einmalige Standardauswahl; alle Varianten uebernehmen deren Wellen. */
public final class GemeinsamerPlaner {
    private GemeinsamerPlaner() {}

    /** Erwartet eine zuvor durch ProjektValidierung gepruefte Instanz. */
    public static Projektplan planeProjekt(
            ProjektInstanz instanz,
            List<ProjektEreignis> ereignisse,
            Map<Integer, Integer> puffer,
            String name,
            String hinweis) {
        Projektplan standard = berechneStandardplan(instanz, name);
        if (ereignisse.isEmpty() && puffer.isEmpty()) return standard;
        return ergaenzePlan(standard, ereignisse, puffer, name, hinweis);
    }

    private static Projektplan berechneStandardplan(ProjektInstanz instanz, String name) {
        Logger.info("Verfahren: Standardreihenfolge ohne Puffer und Ereignisse");
        List<Datensatz> prioritaet = instanz.getAktivitaeten();
        Map<Integer, Long> zaehler = new HashMap<>();
        for (Datensatz d : prioritaet)
            zaehler.put(d.getId(), instanz.berechneNormalisierteRessourcensumme(d.getRessourcen()));
        // Exakter Bruchvergleich: n_a/d_a gegen n_b/d_b, ohne double-Gleichstandsfehler.
        Comparator<Datensatz> vergleich =
                (a, b) ->
                        BigInteger.valueOf(zaehler.get(b.getId()))
                                .multiply(BigInteger.valueOf(a.getDauer()))
                                .compareTo(
                                        BigInteger.valueOf(zaehler.get(a.getId()))
                                                .multiply(BigInteger.valueOf(b.getDauer())));
        prioritaet.sort(
                vergleich
                        .thenComparing(
                                Comparator.comparingInt(
                                                Datensatz::getDirekteNachfolgerFuerPrioritaet)
                                        .reversed())
                        .thenComparingInt(Datensatz::getDauer)
                        .thenComparingInt(Datensatz::getId));
        Projektplan plan = new Projektplan(name);
        Set<Integer> fertig = new HashSet<>();
        int start = 0;
        while (fertig.size() < prioritaet.size()) {
            int[] frei = instanz.getKapazitaeten();
            List<Integer> welle = new ArrayList<>();
            int ende = start;
            // Eine einmalige Prioritaetsordnung reicht: Ressourcen werden nur knapper,
            // und Vorgaenger werden erst nach Abschluss der gesamten Welle freigegeben.
            for (Datensatz d : prioritaet) {
                if (fertig.contains(d.getId())
                        || !fertig.containsAll(d.getVorgaenger())
                        || !passt(d, frei)) continue;
                int f = endzeit(start, d.getDauer(), d.getId());
                plan.addAktivitaet(
                        aktivitaet(
                                d,
                                start,
                                f,
                                d.getDauer(),
                                0,
                                0,
                                berechneScore(instanz, d),
                                d.getDirekteNachfolgerFuerPrioritaet()));
                welle.add(d.getId());
                for (int k = 0; k < frei.length; k++) frei[k] -= d.getRessource(k);
                ende = Math.max(ende, f);
                Logger.info("Auswahl: ID=" + d.getId() + ", Start=" + start + ", Ende=" + f);
            }
            if (welle.isEmpty())
                throw new IllegalArgumentException("Planung blockiert: keine startbare Aktivitaet");
            fertig.addAll(welle);
            start = ende;
        }
        return plan;
    }

    private static boolean passt(Datensatz d, int[] frei) {
        for (int k = 0; k < frei.length; k++) if (d.getRessource(k) > frei[k]) return false;
        return true;
    }

    public static double berechneScore(ProjektInstanz instanz, Datensatz d) {
        return (double) instanz.berechneNormalisierteRessourcensumme(d.getRessourcen())
                / d.getDauer();
    }

    /**
     * Uebernimmt die Wellen eines von diesem Planer erzeugten Referenzplans.
     * Aktivitaeten einer Welle muessen in der Planreihenfolge zusammenstehen;
     * die Wellen muessen nach ihrer Startzeit geordnet sein.
     */
    public static Projektplan ergaenzePlan(
            Projektplan referenz,
            List<ProjektEreignis> ereignisse,
            Map<Integer, Integer> puffer,
            String name,
            String hinweis) {
        Logger.info("Planung: " + name + " | " + hinweis);
        Map<Integer, Integer> events = new HashMap<>();
        for (ProjektEreignis e : ereignisse) {
            if (referenz.getAktivitaetNachId(e.getAktivitaetId()) == null)
                throw new IllegalArgumentException(
                        "Unbekannte Ereignisaktivitaet " + e.getAktivitaetId());
            if (events.putIfAbsent(e.getAktivitaetId(), e.getVerzoegerung()) != null)
                throw new IllegalArgumentException(
                        "Mehr als ein Projekt-Ereignis fuer Aktivitaet "
                                + e.getAktivitaetId()
                                + " ist nicht zulaessig");
        }
        for (var eintrag : puffer.entrySet())
            if (referenz.getAktivitaetNachId(eintrag.getKey()) == null || eintrag.getValue() < 0)
                throw new IllegalArgumentException(
                        "Ungueltige Pufferzuordnung " + eintrag.getKey());
        Projektplan result = new Projektplan(name);
        int referenzStart = -1, start = 0, ende = 0;
        for (PlanAktivitaet a : referenz.getAktivitaetenInPlanreihenfolge()) {
            if (a.getStartzeit() != referenzStart) {
                referenzStart = a.getStartzeit();
                start = ende;
            }
            int p = puffer.getOrDefault(a.getId(), a.getPuffer());
            int e = events.getOrDefault(a.getId(), 0);
            int dauer;
            try {
                dauer = Math.addExact(a.getBasisDauer(), Math.max(p, e));
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException(
                        "Die effektive Aktivitaetsdauer ueberschreitet den Zahlenbereich", ex);
            }
            int f = endzeit(start, dauer, a.getId());
            result.addAktivitaet(
                    aktivitaet(
                            a.getDatensatz(),
                            start,
                            f,
                            dauer,
                            p,
                            e,
                            a.getScoreBeiAuswahl(),
                            a.getDirekteNachfolgerBeiAuswahl()));
            ende = Math.max(ende, f);
        }
        return result;
    }

    private static PlanAktivitaet aktivitaet(
            Datensatz d, int s, int f, int dauer, int p, int e, double q, int nachfolger) {
        return new PlanAktivitaet(
                d,
                new PlanAktivitaet.Zeitplanung(s, f, dauer),
                new PlanAktivitaet.PufferWirkung(p, e),
                new PlanAktivitaet.AuswahlBewertung(q, nachfolger));
    }

    private static int endzeit(int s, int d, int id) {
        try {
            return Math.addExact(s, d);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Die Endzeit der Aktivitaet " + id + " ueberschreitet den Zahlenbereich", e);
        }
    }
}
