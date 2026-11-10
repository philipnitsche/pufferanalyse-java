package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Projektstruktur und einmal berechnete Ressourcen-Normierung. */
public class ProjektInstanz {
    private final List<Datensatz> aktivitaeten;
    private final Map<Integer, Datensatz> nachId = new HashMap<>();
    private final int[] kapazitaeten;
    private final int zielwert;

    public ProjektInstanz(List<Datensatz> aktivitaeten, int... kapazitaeten) {
        if (kapazitaeten.length == 0)
            throw new IllegalArgumentException("Keine Ressourcenkapazitaeten");
        this.aktivitaeten = new ArrayList<>(aktivitaeten);
        this.kapazitaeten = kapazitaeten.clone();
        int l = 1;
        for (int c : kapazitaeten) {
            if (c <= 0)
                throw new IllegalArgumentException(
                        "Ressourcenkapazitaeten muessen groesser als 0 sein");
            long next = (long) (l / ggt(l, c)) * c;
            if (next > Integer.MAX_VALUE)
                throw new IllegalArgumentException("kgV ueberschreitet den Zahlenbereich");
            l = (int) next;
        }
        zielwert = l;
        for (Datensatz d : aktivitaeten) {
            if (nachId.putIfAbsent(d.getId(), d) != null)
                throw new IllegalArgumentException("Doppelte Aktivitaets-ID: " + d.getId());
        }
    }

    private static int ggt(int a, int b) {
        while (b != 0) {
            int rest = a % b;
            a = b;
            b = rest;
        }
        return a;
    }

    public List<Datensatz> getAktivitaeten() {
        return new ArrayList<>(aktivitaeten);
    }

    public Datensatz getAktivitaetNachId(int id) {
        return nachId.get(id);
    }

    public int getAnzahlRessourcen() {
        return kapazitaeten.length;
    }

    public int[] getKapazitaeten() {
        return kapazitaeten.clone();
    }

    public int getKapazitaet(int k) {
        return kapazitaeten[k];
    }

    public int getKapazitaetR1() {
        return getKapazitaet(0);
    }

    public int getKapazitaetR2() {
        return getKapazitaet(1);
    }

    public int getKapazitaetR3() {
        return getKapazitaet(2);
    }

    public int getKapazitaetR4() {
        return getKapazitaet(3);
    }

    public int getNormierungsZielwert() {
        return zielwert;
    }

    public int getNormierungsFaktor(int k) {
        return zielwert / kapazitaeten[k];
    }

    public int getNormierungsFaktorR1() {
        return getNormierungsFaktor(0);
    }

    public int getNormierungsFaktorR2() {
        return getNormierungsFaktor(1);
    }

    public int getNormierungsFaktorR3() {
        return getNormierungsFaktor(2);
    }

    public int normalisiereRessource1(int b) {
        return Math.multiplyExact(b, getNormierungsFaktor(0));
    }

    public int normalisiereRessource2(int b) {
        return Math.multiplyExact(b, getNormierungsFaktor(1));
    }

    public int normalisiereRessource3(int b) {
        return Math.multiplyExact(b, getNormierungsFaktor(2));
    }

    public long berechneNormalisierteRessourcensumme(int... bedarf) {
        if (bedarf.length != kapazitaeten.length)
            throw new IllegalArgumentException("Ressourcenanzahl passt nicht");
        long summe = 0;
        for (int k = 0; k < bedarf.length; k++)
            summe = Math.addExact(summe, (long) bedarf[k] * getNormierungsFaktor(k));
        return summe;
    }
}
