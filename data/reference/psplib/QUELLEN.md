# Projektdaten lokal ergänzen

Die PSPLIB-Originaldaten und gespeicherten Aktivitätsreferenzen sind nicht
enthalten, da keine ausdrückliche Weitergabeerlaubnis belegt wurde.
Die offizielle [PSPLIB-Beschreibung](https://www.om-db.wi.tum.de/psplib/library.php)
gestattet die Nutzung zur Bewertung von Lösungsverfahren.

## Daten beziehen

Auf der [offiziellen Downloadseite](https://www.om-db.wi.tum.de/psplib/getdata.php?mode=sm)
die Single-Mode-Archive **J30** und **J60** laden. Nur diese beiden Dateien
unverändert nach `data/reference/psplib/` entpacken:

- `j601_1.sm` aus J60: für Hauptprogramm und Tests.
- `j301_1.sm` aus J30: zusätzlich für einen Importtest.

Der Java-Lauf benötigt keine weiteren PSPLIB-Dateien. Bauen mit `build.ps1`
ist auch ohne Daten möglich; zum Ausführen müssen sie lokal vorliegen.

## Testreferenz vorbereiten

Mit Python ab 3.9 einmalig im Projektordner ausführen. Die vorhandene
Python-Referenzimplementierung erzeugt die Vergleichswerte. Die Prüfsumme
sichert ab, dass die CSV der bisherigen festen Referenz entspricht.

```powershell
python data/reference/psplib/inspect_j60.py
if ($LASTEXITCODE -ne 0) { throw "Datenpruefung fehlgeschlagen." }
python data/reference/psplib/run_example.py
if ($LASTEXITCODE -ne 0) { throw "Referenzlauf fehlgeschlagen." }
$referenz = 'reports/j601_1-beispiellauf/aktivitaeten.csv'
$erwartet = '9fa4c036b34c4acaac338e09a330dc86c9eba321e66608815355ec18affc4ba6'
if ((Get-FileHash -LiteralPath $referenz -Algorithm SHA256).Hash -ne $erwartet) {
    throw "Referenz weicht vom geprueften Stand ab."
}
New-Item -ItemType Directory -Force tests/resources/reference | Out-Null
Copy-Item -LiteralPath $referenz -Destination tests/resources/reference/j601_1-aktivitaeten.csv
```

Danach können die 94 bestehenden Tests mit `test.ps1` ausgeführt werden.
Originaldaten, erzeugte Referenzen und Downloadarchive sind über `.gitignore`
ausgeschlossen und gehören nicht in einen Commit oder ein Upload-Paket.

## Quelle

Kolisch, R., & Sprecher, A. (1997). PSPLIB – A project scheduling problem
library. *European Journal of Operational Research, 96*(1), 205–216.
https://doi.org/10.1016/S0377-2217(96)00170-1

Eine Lizenz des eigenen Codes erteilt keine Rechte an separat bezogenen
PSPLIB-Daten. Quellen und Dateiabgleich wurden am 9. November 2026 geprüft.
