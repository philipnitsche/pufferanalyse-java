package tests.config;
import config.PufferKonfiguration;
import testsupport.*;
public final class PufferKonfigurationTests implements TestSuite {
    public String getName(){ return "config/PufferKonfiguration"; }
    public void register(TestRegistry r){
        r.add(getName(),"ungueltige Parameter und nicht endliche Faktoren",()->{
            Assert.throwsException(IllegalArgumentException.class,()->new PufferKonfiguration(-1,.25,1,3),"feste Puffer");
            Assert.throwsException(IllegalArgumentException.class,()->new PufferKonfiguration(2,-.25,1,3),"proportionale Faktor");
            Assert.throwsException(IllegalArgumentException.class,()->new PufferKonfiguration(2,Double.NaN,1,3),"endlich");
            Assert.throwsException(IllegalArgumentException.class,()->new PufferKonfiguration(2,Double.POSITIVE_INFINITY,1,3),"endlich");
            Assert.throwsException(IllegalArgumentException.class,()->new PufferKonfiguration(2,.25,3,1),"nicht fallend");
        });
        r.add(getName(),"exakte Rundung und Ueberlauf",()->{
            Assert.equals(7,new PufferKonfiguration(0,.07,1,3).proportionalerPuffer(100),"Dezimale Grenze darf nicht versehentlich auf 8 aufrunden");
            Assert.throwsException(IllegalArgumentException.class,()->new PufferKonfiguration(0,2,1,3).proportionalerPuffer(Integer.MAX_VALUE),"Zahlenbereich");
        });
    }
}
