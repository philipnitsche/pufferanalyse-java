package tests;
import java.util.List;
import testsupport.*;
public final class MainTests implements TestSuite {
    public String getName(){return "Main";}
    public void register(TestRegistry r){r.add(getName(),"echter 60-Aktivitaeten-Einstieg mit einmaliger Standardauswahl",()->{
        var main=Class.forName("Main").getMethod("main",String[].class);
        List<String> messages=LogCapture.captureMessages(()->main.invoke(null,(Object)new String[0]));
        Assert.equals(1L,messages.stream().filter(s->s.equals("Verfahren: Standardreihenfolge ohne Puffer und Ereignisse")).count(),"Nur eine Standardauswahl");
        Assert.isTrue(messages.contains("Aktivitaeten: 60"),"Echte Projektdatei");
        Assert.isTrue(messages.contains("Kapazitaet R4=13"),"Vierte Ressource in der Ausgabe");
        Assert.isTrue(messages.stream().anyMatch(s->s.startsWith("Gantt-Report: ")),"Report erzeugt");
        Assert.isTrue(messages.stream().anyMatch(s->s.startsWith("Rechendaten und Laufnachweis: ")),"CSV/Manifest erzeugt");
        Assert.isTrue(messages.contains("Zufallsserie: 100 Szenarien; getrennt vom Detailbeispiel"),"Zufallsserie ausgefuehrt");
    });}
}
