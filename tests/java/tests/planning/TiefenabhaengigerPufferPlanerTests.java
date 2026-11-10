package tests.planning;
import java.util.List;
import config.PufferKonfiguration;
import planning.TiefenabhaengigerPufferPlaner;
import testsupport.*;
public final class TiefenabhaengigerPufferPlanerTests implements TestSuite {
    public String getName(){return "planning/TiefenabhaengigerPufferPlaner";}
    public void register(TestRegistry r){
        r.add(getName(),"steigende absolute Reserve unabhaengig von der Dauer",()->{
            var i=TestDataFactory.projektInstanzMitVorgaengern(List.of(
                TestDataFactory.datensatz(1,List.of(2),40,1,0,0),
                TestDataFactory.datensatz(2,List.of(3),8,1,0,0),
                TestDataFactory.datensatz(3,List.of(),1,1,0,0)),1,1,1);
            var p=TiefenabhaengigerPufferPlaner.planeProjekt(i,List.of(),"Tiefenabhängig",new PufferKonfiguration(2,.25,1,3));
            Assert.equals(1,p.getAktivitaetNachId(1).getPuffer(),"Start");
            Assert.equals(2,p.getAktivitaetNachId(2).getPuffer(),"Mitte");
            Assert.equals(3,p.getAktivitaetNachId(3).getPuffer(),"Ende trotz kuerzester Dauer");
            Assert.equals(55,p.getMakespan(),"49 Basis-ZE plus 6 Reserve-ZE");
        });
        r.add(getName(),"Rundungsstufen und Tiefe null",()->{
            var c=new PufferKonfiguration(2,.25,1,3);
            Assert.equals(1,c.tiefenabhaengigerPuffer(0,0),"Keine Division durch null");
            Assert.equals(1,c.tiefenabhaengigerPuffer(0,11),"Start");
            Assert.equals(2,c.tiefenabhaengigerPuffer(1,11),"Erster Rundungssprung");
            Assert.equals(2,c.tiefenabhaengigerPuffer(5,11),"Letzte Stufe mit zwei ZE");
            Assert.equals(3,c.tiefenabhaengigerPuffer(6,11),"Zweiter Sprung");
            Assert.equals(3,c.tiefenabhaengigerPuffer(11,11),"Ende");
        });
    }
}
