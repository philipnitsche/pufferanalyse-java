# Versuchsdesign und Auswertung

## Fälle und Wiederholbarkeit

Vier Basispläne werden einmal berechnet. Das gespeicherte Detailbeispiel enthält
zwölf Ereignisse mit zusammen 33 Zeiteinheiten (ZE) Zusatzdauer. Die zusätzliche Serie
umfasst 100 Szenarien und damit 400 Ereignispläne. Sie zählt das Detailbeispiel
nicht mit. Alle vier Verfahren erhalten je Szenario dieselben Ereignisse.

Eine mit 20260913 gestartete `java.util.Random`-Instanz liefert durch
aufeinanderfolgende `nextLong()`-Aufrufe die Szenario-Startwerte (Seeds).
Je Seed werden aus sortierten IDs zwölf verschiedene Aktivitäten gezogen
(partielle Fisher-Yates-Auswahl). Danach werden in Auswahlfolge gleichverteilte
Zusatzdauern von 1 bis 5 gezogen, anschließend die Ereignisse nach ID sortiert.
Alle 100 Listen werden verwendet; Seeds und Listen werden gespeichert.

Derselbe Seed reproduziert dieselben Szenarien nur bei gleicher Generator- und
Aufrufkonvention. Python-Random verwendet eine andere Folge. Ein erneuter Lauf
mit denselben Seeds ist keine zusätzliche Stichprobe.

## Kennzahlen

Erfasst werden Endtermin, Abweichung vom eigenen Basisplan, Reservebudget,
lokaler Verbrauch, Überhang und Ressourcenauslastung. Quoten zeigen
Verbrauch/Budget, Verbrauch/Ereignissumme und vermiedene Abweichung je
Puffer-Zeiteinheit. Lokaler Überhang ist nicht gleich Projektterminverzug.

Die Zusammenfassung enthält Mittelwert, Median, P90, Maximum und Anzahl ohne
Abweichung. Bei gerader Anzahl ist der Median der Mittelwert der beiden
mittleren Werte. P90 verwendet den Rang `ceil(0,9 × n)`, bei 100 Werten also den
90. sortierten Wert. Quoten werden je Szenario berechnet und dann gemittelt;
ein Nenner von 0 wird als `n/a` ausgegeben. Auslastungsdifferenzen sind Prozentpunkte.

## Einordnung der mitgelieferten Ergebnisse

| Verfahren | Mittlere eigene Terminabweichung (ZE) |
| --- | ---: |
| Standard | 12,02 |
| Fest | 4,48 |
| Proportional | 2,75 |
| Tiefenabhängig | 4,04 |

Im Detailfall ist Tiefenabhängig am stabilsten, in der Serie Proportional.
Proportional hat gegenüber Fest 91 kleinere, 7 gleiche und 2 größere Abweichungen;
gegenüber Tiefenabhängig sind es 68/22/10. Es hat zugleich den längsten Basisplan.
Fest hat unter den Pufferverfahren die kürzeste mittlere absolute Dauer
(144,48 ZE) und den höchsten mittleren Nutzungsgrad (18,03 %).

Bei Fest sind höchstens 24 von 120 Reserve-Zeiteinheiten lokal nutzbar.
Unter den gewählten Zusatzdauern beträgt der erwartete Nutzungsgrad 18 %.
Diese Quote allein entscheidet nicht über die beste Regel.

Die verschiedenen Budgets erlauben keine Aussage über die reine Verteilung bei
gleichem Aufwand. Eine Instanz und 100 künstliche Szenarien liefern keine
allgemeine Einhaltungswahrscheinlichkeit. Schon nach den Modellregeln kann die
eigene Terminabweichung nicht größer sein als bei Standard. Die Serie zeigt,
wie stark die einzelnen Pufferregeln diese Abweichung verringern.
