package processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import model.Datensatz;

/** Prueft Nachfolger und baut daraus die Vorgaengerlisten bei jedem Aufruf neu auf. */
public class Daten {
    private final Map<Integer, Datensatz> nachId = new HashMap<>();

    public Daten(Collection<Datensatz> daten) {
        for (Datensatz d : daten)
            if (nachId.putIfAbsent(d.getId(), d) != null)
                throw new IllegalArgumentException("Doppelte Aktivitaets-ID: " + d.getId());
    }

    public void verarbeiteVorgaengerNachfolger() {
        for (Datensatz d : nachId.values()) {
            if (new HashSet<>(d.getNachfolger()).size() != d.getNachfolger().size())
                throw new IllegalArgumentException(
                        "Doppelte Nachfolgerkante bei Aktivitaet " + d.getId());
            for (int id : d.getNachfolger())
                if (!nachId.containsKey(id))
                    throw new IllegalArgumentException(
                            "Aktivitaet "
                                    + d.getId()
                                    + " verweist auf unbekannten Nachfolger "
                                    + id);
        }
        for (Datensatz d : nachId.values()) d.setVorgaenger(new ArrayList<>());
        for (Datensatz d : nachId.values())
            for (int id : d.getNachfolger()) nachId.get(id).addVorgaenger(d.getId());
    }
}
