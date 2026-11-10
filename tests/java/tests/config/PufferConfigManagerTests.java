package tests.config;
import config.*;
import testsupport.*;
public final class PufferConfigManagerTests implements TestSuite {
    public String getName(){ return "config/PufferConfigManager"; }
    public void register(TestRegistry r){r.add(getName(),"verbindliche Referenzparameter",()->{
        var c=PufferConfigManager.getKonfiguration();
        Assert.equals(2,c.getFesterPuffer(),"Feste Reserve");
        Assert.equals(.25,c.getProportionalerFaktor(),0,"Daueranteil");
        Assert.equals(1,c.getTiefenabhaengigeStartReserve(),"Startreserve in ZE");
        Assert.equals(3,c.getTiefenabhaengigeEndReserve(),"Endreserve in ZE");
    });}
}
