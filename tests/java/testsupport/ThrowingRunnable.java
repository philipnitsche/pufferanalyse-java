package testsupport;

import java.io.IOException;

/**
 * Funktionsschnittstelle fuer Testfaelle, die gepruefte Ausnahmen werfen
 * duerfen. Der Testsupport beschraenkt checked Exceptions bewusst auf die
 * tatsaechlich genutzten Bibliotheksgruppen Datei-I/O und Reflexion.
 */
@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws IOException, ReflectiveOperationException;
}
