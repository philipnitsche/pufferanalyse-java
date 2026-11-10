# Pufferanalyse

Java-Programm zur Bachelorarbeit: Vergleich eines Standardplans mit festen,
proportionalen und tiefenabhängigen Zeitreserven. Betrachtet werden Terminverzug,
Reserveaufwand und absolute Projektdauer.

## Starten

Voraussetzung: **JDK 17 oder neuer** (`java` und `javac`) sowie PowerShell.
Keine zusätzlichen Java-Bibliotheken nötig. Die PSPLIB-Daten werden separat
bezogen: zuerst die [Daten lokal ergänzen](data/reference/psplib/QUELLEN.md).
Für die einmalige Vorbereitung der Testreferenz wird zusätzlich Python ab 3.9
benötigt. Danach im Projektordner ausführen:

```powershell
.\build.ps1
.\test.ps1
.\run.ps1
```

Nach Codeänderungen erneut bauen; `run.ps1` startet den vorhandenen Build.
Die **94 Tests** prüfen auch feste Referenzwerte. Geänderte Versuchsparameter
können deshalb zu fehlgeschlagenen Referenztests führen.

## Eingaben und Aufbau

`config.properties` legt Dateien, Kapazitäten, Pufferwerte und Szenarien fest.
Voreingestellt ist `j601_1.sm` mit 60 Aktivitäten und vier Ressourcen.
Mitgeliefert sind ein eigenes Ereignisbeispiel und die Einstellungen für
100 Zufallsszenarien. Die ursprünglichen PSPLIB-Dateien sind nicht enthalten.

| Ordner | Inhalt |
| --- | --- |
| `src` | Java-Code; Einstieg in `Main.java` |
| `tests` | Tests; die Aktivitätsreferenz wird lokal erzeugt |
| `data` | Eigene Ereignisdateien, Downloadhinweise und Python-Helfer |
| `docs` | [Modellregeln](docs/fachentscheidungen-kurz.md), [Parameter](docs/begruendung-pufferparameter.md), [Auswertung](docs/versuchsdesign-und-auswertung.md) |

## Ergebnisse

`reports/letzter-lauf.txt` nennt den letzten erfolgreich exportierten Datenlauf.
Darin liegen:

- `aktivitaeten.csv`, `kennzahlen.csv`, `lauf.json`: Detailfall und Laufnachweis.
- `studie/`: Ereignisse, Aktivitätspläne, Kennzahlen, Zusammenfassung und Auswertung.

Der Gantt-Bericht liegt unter `reports/gantt-run-*.md`. Zur Anzeige ist ein
Markdown-Betrachter mit eingebettetem SVG nötig. Ein Gantt-Exportfehler wird
als Warnung gemeldet; die Datenexporte können trotzdem erfolgreich sein.
Protokolle stehen unter `logs`. Erzeugte Dateien sind über `.gitignore` ausgeschlossen.

Gleiche Eingaben liefern gleiche Rechenwerte. Lauf-IDs, Zeitangaben und
Quellcodeprüfsummen können sich unterscheiden.

## Grenzen und Hilfsmittel

Das Programm untersucht ein festgelegtes Modell. Eine kleinere Abweichung vom
eigenen Plantermin bedeutet nicht automatisch einen früheren Projektabschluss.
Ressourcenauslastung bezeichnet reservierte Zeit, nicht produktive Arbeit.

Die Python-Helfer unter `data/reference/psplib` sind für den Java-Lauf nicht nötig:

| Datei | Verwendung |
| --- | --- |
| `run_example.py` | Historischer Detailfall und Testreferenz; nach `inspect_j60.py`. |
| `inspect_j60.py` | Aus dem Projektordner starten; überschreibt `j601_1-inspection.json`. |
| `render_example_report.py` | Nach `run_example.py`; benötigt Pillow, ReportLab und Windows-Schriften. |
| `export_gantt_images.py` | Nach `run_example.py`; benötigt Pillow und Windows-Schriften. |

Lokal ergänzte PSPLIB-Dateien und erzeugte Referenzen sind über `.gitignore`
ausgeschlossen. Eine Lizenz für den eigenen Code ist noch festzulegen.
