package tests;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import config.PufferKonfiguration;
import config.StudienKonfiguration;
import io.Dateiimport;
import model.*;
import output.StudienExport;
import planning.EreignisGenerator;
import planning.VerfahrensVergleich;
import testsupport.*;
import validation.ProjektValidierung;

public final class StudienTests implements TestSuite {
    public String getName(){return "studie";}
    public void register(TestRegistry r) {
        r.add(getName(),"Seed und sortierte IDs bestimmen dieselbe Ereignisliste",this::reproduzierbar);
        r.add(getName(),"anderer Seed erzeugt einen anderen konkreten Testfall",()->{
            var i=instanz();var c=regeln();
            Assert.isFalse(werte(EreignisGenerator.erzeuge(i,c,42)).equals(werte(EreignisGenerator.erzeuge(i,c,43))),"Zwei festgelegte Seeds unterscheiden sich");
        });
        r.add(getName(),"zwoelf eindeutige echte IDs und gueltige Zusatzdauern",()->{
            var i=instanz();var events=EreignisGenerator.erzeuge(i,regeln(),42);
            Assert.equals(12,events.size(),"Ereigniszahl");
            Assert.equals(12L,events.stream().map(ProjektEreignis::getAktivitaetId).distinct().count(),"Ohne Zuruecklegen");
            for(var e:events){Assert.isTrue(e.getAktivitaetId()>=2&&e.getAktivitaetId()<=61,"Echte IDs");Assert.isTrue(e.getVerzoegerung()>=1&&e.getVerzoegerung()<=5,"Dauerbereich");}
        });
        r.add(getName(),"unmoegliche Ereigniszahl und ungueltige Regeln werden abgelehnt",()->{
            Assert.throwsException(IllegalArgumentException.class,()->EreignisGenerator.erzeuge(instanz(),new StudienKonfiguration(1,1,61,1,5),1),"Mehr betroffene");
            Assert.throwsException(IllegalArgumentException.class,()->new StudienKonfiguration(0,1,12,1,5),"positiv");
            Assert.throwsException(IllegalArgumentException.class,()->new StudienKonfiguration(1,1,12,5,1),"Bereich");
        });
        r.add(getName(),"feste Dauergrenze und alle Aktivitaeten werden unterstuetzt",()->{
            var es=EreignisGenerator.erzeuge(instanz(),new StudienKonfiguration(1,9,60,3,3),9);
            Assert.equals(60,es.size(),"Alle echten IDs");
            Assert.isTrue(es.stream().allMatch(e->e.getVerzoegerung()==3),"Geschlossene feste Grenze");
        });
        r.add(getName(),"Serie wiederholbar und Basisplaene unveraendert gemeinsam",this::serie);
        r.add(getName(),"Zusammenfassung berechnet Median P90 und mittlere Einzelquoten",this::zusammenfassung);
    }
    private static ProjektInstanz instanz() throws java.io.IOException {
        var imp=new Dateiimport();imp.importiere("data/reference/psplib/j601_1.sm");
        return ProjektValidierung.pruefeUndErzeugeInstanz(imp.getAlleDaten(),imp.getDateiKapazitaeten());
    }
    private static StudienKonfiguration regeln(){return new StudienKonfiguration(3,20260913,12,1,5);}
    private static List<String> werte(List<ProjektEreignis> es){return es.stream().map(e->e.getAktivitaetId()+":"+e.getVerzoegerung()).toList();}
    private void reproduzierbar()throws java.io.IOException {
        var i=instanz();var a=EreignisGenerator.erzeuge(i,regeln(),42);
        var daten=new ArrayList<>(i.getAktivitaeten());Collections.reverse(daten);
        var umgedreht=ProjektValidierung.pruefeUndErzeugeInstanz(daten,i.getKapazitaeten());
        Assert.listEquals(werte(a),werte(EreignisGenerator.erzeuge(umgedreht,regeln(),42)),"Eingabereihenfolge aendert den Zufall nicht");
    }
    private void serie()throws java.io.IOException {
        var i=instanz();var v=new VerfahrensVergleich(i,new PufferKonfiguration(2,.25,1,3));
        var a=EreignisStudie.berechne(i,regeln(),v);var b=EreignisStudie.berechne(i,regeln(),v);
        for(int j=0;j<3;j++) {
            Assert.equals(a.szenarien().get(j).seed(),b.szenarien().get(j).seed(),"Gleiche Seedfolge");
            Assert.listEquals(werte(a.szenarien().get(j).ereignisse()),werte(b.szenarien().get(j).ereignisse()),"Gleiche Ereignisse");
            for(int k=0;k<4;k++) Assert.same(a.szenarien().get(0).ergebnisse().get(k).getBasisplan(),a.szenarien().get(j).ergebnisse().get(k).getBasisplan(),"Keine erneute Grundplanung");
        }
    }
    private void zusammenfassung()throws java.io.IOException {
        var i=ProjektValidierung.pruefeUndErzeugeInstanz(List.of(new Datensatz(1,List.of(),List.of(),5,1)),1);
        var v=new VerfahrensVergleich(i,new PufferKonfiguration(2,.25,1,3));
        var a=List.of(new ProjektEreignis(1,1));var b=List.of(new ProjektEreignis(1,4));
        var studie=new EreignisStudie(new StudienKonfiguration(2,1,1,1,4),List.of(
            new EreignisStudie.Szenario(1,10,a,v.berechne(a)),new EreignisStudie.Szenario(2,11,b,v.berechne(b))));
        var root=Files.createTempDirectory("studien-export-");
        try {
            Files.writeString(root.resolve("lauf.json"),"{}");
            StudienExport.exportiere(root.resolve("studie"),i,studie);
            var zeilen=Files.readAllLines(root.resolve("studie/zusammenfassung.csv"));
            String[] standard=zeilen.get(1).split(";"), fest=zeilen.get(2).split(";");
            Assert.equals("2.5",standard[4],"Mittlere Abweichung");
            Assert.equals("2.5",standard[5],"Median gerader Stichprobe");
            Assert.equals("4",standard[6],"P90 nach naechstem Rang");
            Assert.equals("0.75",fest[13],"Mittel der Aufnahmequoten 1 und 0.5, kein Quotient der Summen");
            Assert.equals(9,Files.readAllLines(root.resolve("studie/aktivitaeten.csv")).size(),"Acht Aktivitaetszeilen plus Kopf");
            Assert.contains(Files.readString(root.resolve("studie/auswertung.md")),"| Standard | 0 | 2,50 |","Lesbare Zusammenfassung nutzt dieselben Werte");
        } finally {
            for(String n:List.of("ereignisse.csv","aktivitaeten.csv","kennzahlen.csv","zusammenfassung.csv","studie.json","auswertung.md"))Files.deleteIfExists(root.resolve("studie").resolve(n));
            Files.deleteIfExists(root.resolve("studie"));Files.deleteIfExists(root.resolve("lauf.json"));Files.deleteIfExists(root);
        }
    }
}
