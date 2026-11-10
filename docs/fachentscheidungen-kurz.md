# Modellregeln

## Eingaben

- Referenzprojekt: `j601_1.sm`, 60 echte Aktivitäten (IDs 2–61), Kapazitäten
  13/11/12/13. Die beiden künstlichen Start-/Endknoten werden nicht geplant.
- Das Hauptprogramm verwendet genau vier Ressourcen. Die Kapazitäten in
  `config.properties` müssen mit der Projektdatei übereinstimmen.
- Unterstützt wird PSPLIB-Single-Mode mit erneuerbaren Ressourcen und zwei
  Randknoten ohne Dauer. Der Import prüft die benötigten Tabellen, nicht
  sämtliche Kopfangaben. Projekt-CSV wird nicht unterstützt.
- Ereignis-CSV: Kopfzeile `AktivitaetID;Verzoegerung`, höchstens ein Ereignis
  je Aktivität. Eine leere Datei bedeutet keine Ereignisse; Leerzeilen
  innerhalb einer befüllten Datei sind unzulässig.

## Planung

Standard legt Auswahlfolge und Wellen einmal fest. Eine Welle ist eine Gruppe
zugleich startender Aktivitäten. Alle Verfahren und Szenarien übernehmen diese
Gruppen. Die nächste Welle wartet auf das letzte reservierte Ende der vorherigen;
innerhalb einer Welle wird nicht nachgestartet.

Die Auswahlpriorität ist `sum(r_ik / C_k) / d_i`: Ressourcenbedarf geteilt durch
Kapazität, über alle Ressourcen summiert und durch die Basisdauer geteilt.
Bei Gleichstand entscheiden mehr direkte Nachfolger, kürzere Basisdauer und
kleinere ID. Eine ursprüngliche Kante zum Endknoten zählt bei der Nachfolgerzahl mit.

Mit `d` als Basisdauer und `h` als Länge der längsten Vorgängerkette gelten standardmäßig:

- Fest: `p = 2`.
- Proportional: `p = ceil(d / 4)`.
- Tiefenabhängig: `p = ceil(1 + 2h / H)`; bei maximaler Tiefe `H = 0` gilt `p = 1`.

`ceil` rundet auf die nächste ganze Zahl auf. Die Reserven werden vorab bestimmt;
es gibt keine zusätzliche Deckelung oder nachträgliche Anpassung.

## Ereigniswirkung

Für Reserve `p` und Ereigniszusatzdauer `e` gilt:

- reservierte Dauer: `d + max(p, e)`;
- lokal verbrauchte Reserve: `min(p, e)`;
- lokaler Überhang: `max(0, e - p)`.

Reserven werden nicht übertragen. Spätere Wellen holen frühere Verzögerungen
nicht auf. Ressourcen bleiben über das gesamte reservierte Intervall belegt,
auch über ungenutzte Reservezeit. Ereignisse verlängern Aktivitäten; sie ändern
keine Kapazitäten oder Bedarfe und bilden keine gemeinsamen Ausfälle ab.

Je Welle gilt `E_a = max(B_a, E_0)`: Die Dauer mit Puffer und Ereignissen ist
das Maximum aus ihrer gepufferten Basisdauer und ihrer Standarddauer mit
Ereignissen. Daher ist die eigene Terminabweichung höchstens so groß wie bei
Standard; der absolute Ereignisendtermin ist dagegen nicht früher als bei
Standard. Gleiche Basismaxima je Welle ergeben gleiche Endtermine. Die feste
Reihenfolge ist eine Modellvorgabe, kein nachgewiesener Puffervorteil.

Basispläne und zugrunde liegende Datensätze dürfen während eines Vergleichs
nicht nachträglich verändert werden. Die Klassen sind für den internen Ablauf
ausgelegt, nicht als allgemeine öffentliche Bibliothek.
