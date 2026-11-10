package tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import config.PufferKonfiguration;
import io.Dateiimport;
import io.ProjektEreignisImport;
import model.AlgorithmusErgebnis;
import model.Datensatz;
import model.PlanAktivitaet;
import model.Projektplan;
import planning.Standard;
import validation.ProjektValidierung;
import testsupport.Assert;
import testsupport.TestDataFactory;
import testsupport.TestPaths;
import testsupport.TestRegistry;
import testsupport.TestSuite;

/** Referenzdaten sind der vor der Java-Aenderung gespeicherte Python-Lauf. */
public final class ReferenzfallTests implements TestSuite {
    public String getName() { return "integration/Referenzfall"; }
    public void register(TestRegistry r) {
        r.add(getName(),"alle 480 Referenzintervalle und vier Ressourcen",this::vergleicheReferenz);
        r.add(getName(),"vierte Ressource verhindert unzulaessige Parallelitaet",this::vierteRessource);
        r.add(getName(),"mathematisch gleiche Scores verwenden den Tie-Break",this::exakterGleichstand);
        r.add(getName(),"unbekannte Kanten und doppelte IDs werden vor Aufbereitung abgelehnt",this::fehlerhafteKanten);
        r.add(getName(),"Tiefenberechnung ohne Rekursion auf langer Kette",this::langeKette);
        r.add(getName(),"Wellenidentitaet und Schranken fuer beliebige lokale Reserven",this::wellenIdentitaet);
        r.add(getName(),"Endterminstabilitaet ist nicht gleich lokaler Pufferverbrauch",this::parallelitaetsReserve);
        r.add(getName(),"nicht erneuerbare PSPLIB-Ressourcen werden abgelehnt",() -> {
            Path p = Files.createTempFile("nicht-erneuerbar-", ".sm");
            try {
                String text = Files.readString(Path.of("data/reference/psplib/j601_1.sm"));
                Files.writeString(p, text.replace("- nonrenewable              :  0", "- nonrenewable              :  1"));
                Assert.throwsException(IllegalArgumentException.class, () -> new Dateiimport().importiere(p.toString()), "Nur erneuerbare");
            } finally { Files.deleteIfExists(p); }
        });
    }

    @SuppressWarnings("unchecked")
    private void vergleicheReferenz() throws java.io.IOException, ReflectiveOperationException {
        Dateiimport importer = new Dateiimport();
        importer.importiere("data/reference/psplib/j601_1.sm");
        int[] cap = importer.getDateiKapazitaeten();
        Assert.listEquals(List.of(13,11,12,13),java.util.Arrays.stream(cap).boxed().toList(),"Originalkapazitaeten");
        var instanz = ProjektValidierung.pruefeUndErzeugeInstanz(importer.getAlleDaten(),cap);
        Assert.equals(60,instanz.getAktivitaeten().size(),"Dummies ausgeschlossen");
        var events = new ProjektEreignisImport().importiere("data/reference/psplib/j601_1-ereignisse-vorschlag.csv");
        var method = Class.forName("Main").getMethod("berechneVergleich",model.ProjektInstanz.class,List.class,PufferKonfiguration.class);
        var ergebnisse = (List<AlgorithmusErgebnis>)method.invoke(null,instanz,events,new PufferKonfiguration(2,.25,1,3));
        Map<String,Projektplan> plaene = new HashMap<>();
        String[] namen = {"Standard","Fest","Proportional","Tiefenabhängig"};
        for (int k = 0; k < ergebnisse.size(); k++) {
            plaene.put(namen[k]+"/base",ergebnisse.get(k).getBasisplan());
            plaene.put(namen[k]+"/event",ergebnisse.get(k).getEreignisplan());
        }
        List<String> referenz = Files.readAllLines(TestPaths.resource("reference","j601_1-aktivitaeten.csv"));
        Assert.equals(481,referenz.size(),"480 Zeilen plus Kopf");
        for (String zeile : referenz.subList(1,referenz.size())) {
            String[] v = zeile.split(",");
            Projektplan plan = plaene.get(v[0]+"/"+v[1]);
            PlanAktivitaet a = plan.getAktivitaetNachId(Integer.parseInt(v[2]));
            int[] erwartet = java.util.Arrays.stream(v).skip(3).mapToInt(Integer::parseInt).toArray();
            int welle = new TreeSet<>(plan.getAktivitaetenSortiert().stream().map(PlanAktivitaet::getStartzeit).toList()).headSet(a.getStartzeit()).size()+1;
            int[] ist = {welle,a.getStartzeit(),a.getEndzeit(),a.getBasisDauer(),a.getPuffer(),a.getVerzoegerung(),a.getVerzoegerungImPuffer(),a.getZusatzverzoegerungNachPuffer()};
            Assert.isTrue(java.util.Arrays.equals(erwartet,ist),"Referenzabweichung: "+zeile);
        }
        for (Projektplan plan : plaene.values()) {
            List<PlanAktivitaet> aktiv = plan.getAktivitaetenSortiert();
            TreeSet<Integer> grenzen = new TreeSet<>();
            for (PlanAktivitaet a : aktiv) {
                grenzen.add(a.getStartzeit()); grenzen.add(a.getEndzeit());
                for (int vor : a.getDatensatz().getVorgaenger())
                    Assert.isTrue(plan.getAktivitaetNachId(vor).getEndzeit()<=a.getStartzeit(),"Vorgaengerbedingung");
            }
            for (int t : grenzen) for (int k = 0; k < 4; k++) {
                int summe = 0;
                for (PlanAktivitaet a : aktiv) if (a.getStartzeit()<=t && t<a.getEndzeit()) summe+=a.getRessource(k);
                Assert.isTrue(summe<=cap[k],"Ressource R"+(k+1)+" bei t="+t);
            }
        }
        int[] basis={114,140,149,144}, event={127,142,151,145}, budget={0,120,108,129};
        for(int k=0;k<4;k++) {
            var e=ergebnisse.get(k);
            Assert.equals(basis[k],e.getBasisbewertung().getMakespan(),"Basis-Makespan");
            Assert.equals(event[k],e.getEreignisbewertung().getMakespan(),"Ereignis-Makespan");
            Assert.equals(budget[k],e.getBasisbewertung().getGesamterPuffer(),"Pufferbudget");
            Assert.equals(33,e.getEreignisbewertung().getVerbrauchterPuffer()+e.getEreignisbewertung().getEreignisUeberhang(),"Erhaltung der Zusatzdauer");
        }
    }

    private void vierteRessource() {
        var i=ProjektValidierung.pruefeUndErzeugeInstanz(List.of(
            new Datensatz(1,List.of(),List.of(),2,0,0,0,1),
            new Datensatz(2,List.of(),List.of(),2,0,0,0,1)),1,1,1,1);
        Projektplan p=Standard.planeStandardProjekt(i,List.of(),"R4");
        Assert.equals(4,p.getMakespan(),"R4 allein zwingt zwei Wellen");
    }
    private void wellenIdentitaet() {
        var i=ProjektValidierung.pruefeUndErzeugeInstanz(List.of(
            new Datensatz(1,List.of(3),List.of(),5,1),
            new Datensatz(2,List.of(3),List.of(),2,1),
            new Datensatz(3,List.of(),List.of(),3,1)),2);
        Projektplan standard=Standard.planeStandardProjekt(i,List.of(),"Grundplan");
        for(int p1=0;p1<=6;p1++) for(int p2=0;p2<=6;p2++)
            for(int e1=0;e1<=6;e1++) for(int e2=0;e2<=6;e2++) {
                Map<Integer,Integer> p=Map.of(1,p1,2,p2,3,2);
                List<model.ProjektEreignis> events=new java.util.ArrayList<>();
                if(e1>0)events.add(new model.ProjektEreignis(1,e1));
                if(e2>0)events.add(new model.ProjektEreignis(2,e2));
                Projektplan basis=planning.GemeinsamerPlaner.ergaenzePlan(standard,List.of(),p,"B","");
                Projektplan ereignis=planning.GemeinsamerPlaner.ergaenzePlan(basis,events,Map.of(),"E","");
                int b1=Math.max(5+p1,2+p2), e01=Math.max(5+e1,2+e2);
                Assert.equals(Math.max(b1,e01)+5,ereignis.getMakespan(),"Wellenmaximum aus Basis und Standardereignis");
                int delta=ereignis.getMakespan()-basis.getMakespan();
                Assert.isTrue(delta>=0 && delta<=e01-5,"Abweichung bleibt zwischen null und Standardabweichung");
                Assert.equals(standard.getAktivitaetenInPlanreihenfolge().stream().map(PlanAktivitaet::getId).toList(),
                    ereignis.getAktivitaetenInPlanreihenfolge().stream().map(PlanAktivitaet::getId).toList(),"Auswahlfolge bleibt fest");
            }
    }
    private void parallelitaetsReserve() {
        var i=ProjektValidierung.pruefeUndErzeugeInstanz(List.of(
            new Datensatz(1,List.of(),List.of(),10,1),
            new Datensatz(2,List.of(),List.of(),9,1)),2);
        Projektplan standard=Standard.planeStandardProjekt(i,List.of(),"Grundplan");
        var events=List.of(new model.ProjektEreignis(1,2));
        Projektplan basis=planning.GemeinsamerPlaner.ergaenzePlan(standard,List.of(),Map.of(2,3),"B","");
        Projektplan ereignis=planning.GemeinsamerPlaner.ergaenzePlan(basis,events,Map.of(),"E","");
        Assert.equals(12,basis.getMakespan(),"Reserve der parallelen Aktivitaet bestimmt den angekuendigten Termin");
        Assert.equals(12,ereignis.getMakespan(),"Keine Endterminabweichung trotz lokalem Ueberhang");
        var b=model.PlanBewertung.ausPlan(ereignis,i,events);
        Assert.equals(0,b.getVerbrauchterPuffer(),"Keine lokale Pufferaufnahme");
        Assert.equals(2,b.getEreignisUeberhang(),"Ueberhang wird vom Wellenmaximum ueberdeckt");
    }
    private void exakterGleichstand() {
        var i=TestDataFactory.projektInstanzMitVorgaengern(List.of(
            TestDataFactory.datensatz(1,List.of(),5,1,1,1),
            TestDataFactory.datensatz(2,List.of(3),5,3,0,0),
            TestDataFactory.datensatz(3,List.of(),1,0,0,0)),3,3,3);
        Projektplan p=Standard.planeStandardProjekt(i,List.of(),"Gleichstand");
        Assert.equals(0,p.getAktivitaetNachId(2).getStartzeit(),"Exakt 3/5: mehr Nachfolger entscheidet");
        Assert.equals(5,p.getAktivitaetNachId(1).getStartzeit(),"Kein Gleitkomma-Vorrang fuer 1/5+1/5+1/5");
    }
    private void fehlerhafteKanten() {
        Assert.throwsException(IllegalArgumentException.class,()->ProjektValidierung.pruefeUndErzeugeInstanz(
            List.of(new Datensatz(1,List.of(99),List.of(),1,0,0,0,0)),1,1,1,1),"unbekannten Nachfolger 99");
        Assert.throwsException(IllegalArgumentException.class,()->ProjektValidierung.pruefeUndErzeugeInstanz(
            List.of(new Datensatz(1,List.of(),List.of(),1,0,0,0,0),new Datensatz(1,List.of(),List.of(),1,0,0,0,0)),1,1,1,1),"Doppelte");
    }
    private void langeKette() {
        List<Datensatz> daten=new java.util.ArrayList<>();
        for(int k=1;k<=2500;k++) daten.add(new Datensatz(k,k<2500?List.of(k+1):List.of(),List.of(),1,0,0,0,0));
        var i=ProjektValidierung.pruefeUndErzeugeInstanz(daten,1,1,1,1);
        Assert.equals(Integer.valueOf(2499),ProjektValidierung.berechneTiefen(i).get(2500),"Iterative Tiefe auf langer Kette");
    }
}


