package model;

import java.util.ArrayList;
import java.util.List;

/** Basisdaten einer Aktivitaet; Ressourcen werden als Vektor gespeichert. */
public class Datensatz {
    private final int id;
    private final int dauer;
    private final int[] ressourcen;
    private List<Integer> nachfolger;
    private List<Integer> vorgaenger;
    private Integer originalNachfolgerAnzahl;

    public Datensatz(
            int id,
            List<Integer> nachfolger,
            List<Integer> vorgaenger,
            int dauer,
            int... ressourcen) {
        this.id = id;
        this.dauer = dauer;
        this.ressourcen = ressourcen.clone();
        this.nachfolger = new ArrayList<>(nachfolger);
        this.vorgaenger = new ArrayList<>(vorgaenger);
    }

    public int getId() {
        return id;
    }

    public int getDauer() {
        return dauer;
    }

    public int getAnzahlRessourcen() {
        return ressourcen.length;
    }

    public int getRessource(int index) {
        return ressourcen[index];
    }

    public int[] getRessourcen() {
        return ressourcen.clone();
    }

    public int getRessource1() {
        return getRessource(0);
    }

    public int getRessource2() {
        return getRessource(1);
    }

    public int getRessource3() {
        return getRessource(2);
    }

    public int getRessource4() {
        return getRessource(3);
    }

    public List<Integer> getNachfolger() {
        return List.copyOf(nachfolger);
    }

    public List<Integer> getVorgaenger() {
        return List.copyOf(vorgaenger);
    }

    public Integer getNachfolgerAt(int index) {
        return index < 0 || index >= nachfolger.size() ? null : nachfolger.get(index);
    }

    public Integer getVorgaengerAt(int index) {
        return index < 0 || index >= vorgaenger.size() ? null : vorgaenger.get(index);
    }

    public void setNachfolger(List<Integer> ids) {
        nachfolger = new ArrayList<>(ids);
    }

    public void setVorgaenger(List<Integer> ids) {
        vorgaenger = new ArrayList<>(ids);
    }

    public void addNachfolger(Integer id) {
        if (!nachfolger.contains(id)) nachfolger.add(id);
    }

    public void removeNachfolger(Integer id) {
        nachfolger.remove(id);
    }

    public void addVorgaenger(Integer id) {
        if (!vorgaenger.contains(id)) vorgaenger.add(id);
    }

    public void removeVorgaenger(Integer id) {
        vorgaenger.remove(id);
    }

    /** PSPLIB-Tie-Break zaehlt auch eine urspruengliche Kante zum End-Dummy. */
    public void setOriginalNachfolgerAnzahl(int anzahl) {
        if (anzahl < nachfolger.size())
            throw new IllegalArgumentException("Ungueltige Original-Nachfolgerzahl");
        originalNachfolgerAnzahl = anzahl;
    }

    public int getDirekteNachfolgerFuerPrioritaet() {
        return originalNachfolgerAnzahl == null ? nachfolger.size() : originalNachfolgerAnzahl;
    }
}
