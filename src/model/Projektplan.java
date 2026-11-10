package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Benannter Plan mit terminierten Aktivitaeten. */
public class Projektplan {
    private final String name;

    private final List<PlanAktivitaet> aktivitaeten;

    private final Map<Integer, PlanAktivitaet> aktivitaetNachId;

    public Projektplan(String name) {
        this.name = name;
        this.aktivitaeten = new ArrayList<>();
        this.aktivitaetNachId = new HashMap<>();
    }

    public void addAktivitaet(PlanAktivitaet planAktivitaet) {
        if (aktivitaetNachId.containsKey(planAktivitaet.getId()))
            throw new IllegalArgumentException(
                    "Aktivitaet bereits geplant: " + planAktivitaet.getId());
        aktivitaeten.add(planAktivitaet);
        aktivitaetNachId.put(planAktivitaet.getId(), planAktivitaet);
    }

    /** Unveraenderliche Listenkopie in Einfuegungs- und Planungsreihenfolge. */
    public List<PlanAktivitaet> getAktivitaetenInPlanreihenfolge() {
        return List.copyOf(aktivitaeten);
    }

    public String getName() {
        return name;
    }

    /** Listenkopie, sortiert nach Startzeit und ID. */
    public List<PlanAktivitaet> getAktivitaetenSortiert() {
        List<PlanAktivitaet> kopie = new ArrayList<>(aktivitaeten);
        kopie.sort(
                Comparator.comparingInt(PlanAktivitaet::getStartzeit)
                        .thenComparingInt(PlanAktivitaet::getId));
        return kopie;
    }

    /** Liefert null, wenn die ID nicht im Plan enthalten ist. */
    public PlanAktivitaet getAktivitaetNachId(int id) {
        return aktivitaetNachId.get(id);
    }

    /** Groesste reservierte Endzeit; bei leerem Plan 0. */
    public int getMakespan() {
        int maximum = 0;
        for (PlanAktivitaet planAktivitaet : aktivitaeten) {
            if (planAktivitaet.getEndzeit() > maximum) {
                maximum = planAktivitaet.getEndzeit();
            }
        }
        return maximum;
    }
}
