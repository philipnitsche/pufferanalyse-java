package tests.planning;
import java.util.List;
import config.PufferKonfiguration;
import planning.ProportionalerPufferPlaner;
import testsupport.*;
public final class ProportionalerPufferPlanerTests implements TestSuite {
    public String getName(){return "planning/ProportionalerPufferPlaner";}
    public void register(TestRegistry r){r.add(getName(),"Aufrundung ohne kuenstliche Deckelung",()->{
        var i=TestDataFactory.projektInstanzMitVorgaengern(List.of(
            TestDataFactory.datensatz(1,List.of(),1,1,0,0),
            TestDataFactory.datensatz(2,List.of(),4,1,0,0),
            TestDataFactory.datensatz(3,List.of(),20,1,0,0)),3,1,1);
        var p=ProportionalerPufferPlaner.planeProjekt(i,List.of(),"Proportional",new PufferKonfiguration(2,.25,1,3));
        Assert.equals(1,p.getAktivitaetNachId(1).getPuffer(),"ceil(0.25)");
        Assert.equals(1,p.getAktivitaetNachId(2).getPuffer(),"ceil(1)");
        Assert.equals(5,p.getAktivitaetNachId(3).getPuffer(),"Kein alter Maximalpuffer");
    });}
}
