"""Render the completed reference run as Markdown, figures and a PDF."""
from pathlib import Path
import json
from collections import Counter
from html import escape
from PIL import Image as PILImage, ImageDraw, ImageFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT/'reports/j601_1-beispiellauf'
PDF = ROOT/'output/pdf/j601_1-beispiellauf.pdf'
data = json.loads((OUT/'ergebnisse.json').read_text(encoding='utf-8'))
results = data['results']
names = list(results)
PDF.parent.mkdir(parents=True, exist_ok=True)
font_dir = Path('C:/Windows/Fonts')
pdfmetrics.registerFont(TTFont('Body', str(font_dir/'arial.ttf')))
pdfmetrics.registerFont(TTFont('BodyBold', str(font_dir/'arialbd.ttf')))
pdfmetrics.registerFontFamily('Body', normal='Body', bold='BodyBold', italic='Body', boldItalic='BodyBold')
styles = getSampleStyleSheet()
for name in ['Normal', 'BodyText', 'Title', 'Heading1', 'Heading2']:
    styles[name].fontName = 'BodyBold' if name in ['Title', 'Heading1', 'Heading2'] else 'Body'
styles['BodyText'].fontSize = 9
styles['BodyText'].leading = 13
styles['BodyText'].spaceAfter = 8
styles['Heading1'].fontSize = 17
styles['Heading1'].leading = 22
styles['Heading2'].fontSize = 11
styles.add(ParagraphStyle('Cell', fontName='Body', fontSize=8, leading=10))
story, md = [], ['# Beispiellauf j601_1.sm\n\nStand: 05.10.2026. Python-Referenzlauf.\n']

def h(text, level=1):
    story.append(Paragraph(escape(text), styles['Heading1' if level == 1 else 'Heading2']))
    md.append('\n'+'#'*(level+1)+' '+text+'\n')

def p(text):
    story.append(Paragraph(escape(text), styles['BodyText']))
    md.append(text+'\n')

def table(headers, rows, widths=None):
    cells = [[Paragraph(escape(str(x)), styles['Cell']) for x in row] for row in [headers]+rows]
    t = Table(cells, colWidths=widths, repeatRows=1, hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#e6eef5')),
        ('VALIGN',(0,0),(-1,-1),'TOP'), ('BOTTOMPADDING',(0,0),(-1,-1),6),
        ('TOPPADDING',(0,0),(-1,-1),6), ('LINEBELOW',(0,0),(-1,0),.7,colors.HexColor('#526c84')),
        ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f5f7f9')]),
    ]))
    story.extend([t, Spacer(1, .25*cm)])
    md.extend(['| '+' | '.join(headers)+' |', '| '+' | '.join(['---']*len(headers))+' |'])
    md.extend('| '+' | '.join(str(x) for x in row)+' |' for row in rows)
    md.append('')

def page(): story.append(PageBreak())
def pct(x): return 'n/a' if x is None else f'{100*x:.1f} %'

h('Beispiellauf: ein Projekt, vier Verfahren')
p('05.10.2026 | PSPLIB j601_1.sm | 60 Aktivitäten | 12 feste Ereignisse')
h('Prüfung vor der Ausführung: 9/10', 2)
p('Subjektive Bewertung der Eignung als nachvollziehbarer Beispielvergleich, keine wissenschaftliche Validierungskennzahl. Zielbezug, feste gemeinsame Eingaben, dokumentierte Annahmen und ausführbare Prüfungen sprechen für das Vorgehen. Der verbleibende Punkt betrifft den noch ausstehenden Abgleich mit der Java-Anwendung und die begrenzte Aussagekraft eines einzelnen Beispiels.')
p('Der unveränderte Java-Stand wäre ungeeignet: Er hat drei Ressourcenfelder und eine fallende tiefenabhängige Pufferregel. Ausgeführt wurde deshalb eine explizite Python-Referenzimplementierung, die das vorhandene Prüfskript erweitert. Java-Code und aktive Konfiguration wurden nicht verändert. Der Bericht belegt keinen fertigen Java-Lauf.')
h('Festgelegtes Modell und Begründung', 2)
p('Ein synthetisches Benchmarkprojekt aus der PSPLIB [1, 2], keine Repräsentativitätsbehauptung. Die 60 Aktivitäten bieten gegenüber der Vorgängerarbeit mehr Erklärstoff bei weiterhin vier erneuerbaren Ressourcen und einem Ausführungsmodus. Dummy-Knoten 1 und 62 werden entfernt; echte IDs 2 bis 61 bleiben erhalten. Kapazitäten: 13 / 11 / 12 / 13.')
p('Prioritätsregelbasierte wellengebundene Planung: einmalige Standardauswahl anhand von sum(r_ik/C_k)/d_i; Gleichstände nach mehr direkten Nachfolgern, kürzerer Basisdauer und kleinerer ID. Der gemeinsame Normierungsfaktor des Java-Scores ändert die Rangfolge nicht; Python verwendet exakte Brüche. Alle Nachfolgepläne übernehmen diese Auswahl und Wellenzuordnung unverändert.')
p('Left Shift innerhalb der Wellenlogik bedeutet: Die nächste Welle startet unmittelbar nach dem spätesten Ende der vorherigen. Frei werdende Kapazität innerhalb einer laufenden Welle führt zu keiner neuen Auswahl. Diese Wellenbarriere ist eine eigene Modellannahme. Der Bezug zu Thünemann betrifft die dokumentierte gruppenweise Fortschaltung, keine identische Replikation seines Codes [3].')
table(['Verfahren', 'Reserve je Aktivität', 'Begründung'], [
    ['Standard','0 ZE','Ungepufferte Referenz'],
    ['Fest','2 ZE','Einfache gleiche lokale Reserve'],
    ['Proportional','ceil(0,25 x Basisdauer)','Reserve abhängig von der Dauer'],
    ['Tiefenabhängig','ceil(1 + 2 x Tiefe/11)','Steigende positionsbezogene Reserve, 1 bis 3 ZE'],
], [2.4*cm,5*cm,9.1*cm])
p('Diese Werte wurden für den Beispiellauf bestätigt; sie sind eigene Modellannahmen, keine empirisch kalibrierten oder optimalen Werte. Keine zusätzlichen Min-/Max-Deckelungen. Rundung nach oben kann gleiche Reserven über mehrere Tiefenstufen ergeben. Effektive Dauer = Basisdauer + max(Reserve, Zusatzdauer).')

page(); h('Ergebnis: Planabweichung und absolute Dauer')
table(['Verfahren','Basis ZE','Ereignis ZE','Differenz ZE','Gesamtpuffer ZE','Verbraucht ZE','Überhang ZE'],
      [[n, v['base']['metrics']['makespan'],v['event']['metrics']['makespan'],v['extension'],
        v['event']['metrics']['total_buffer'],v['event']['metrics']['used'],v['event']['metrics']['overhang']]
       for n,v in results.items()], [2.5*cm]+[2.33*cm]*6)
p('Alle Verfahren erhalten dieselben zwölf Zusatzdauern mit insgesamt 33 ZE. Die Summe der lokalen Zusatzdauern ist nicht die Projektverlängerung: Parallelität und das Maximum jeder Welle bestimmen den Endtermin.')
p('Standard endet mit Ereignissen bei 127 ZE und damit absolut am frühesten. Seine Abweichung gegenüber dem eigenen Basisplan beträgt jedoch 13 ZE. Tiefenabhängig hat mit 1 ZE die kleinste Abweichung vom eigenen Basisplan, endet aber bei 145 ZE. Fest endet bei 142 ZE und damit unter den Pufferverfahren am frühesten; proportional endet bei 151 ZE. Es gibt deshalb keine uneingeschränkte Rangfolge über alle Kriterien.')
p('Die gepufferten Basispläne sind bereits länger: fest um 26 ZE, proportional um 35 ZE und tiefenabhängig um 30 ZE gegenüber Standard. Die höhere Terminstabilität wird durch eingeplante Reserve erreicht. Bei unveränderten Wellen und Basisdauer + max(Puffer, Ereignis) kann ein gepufferter Ereignisplan den ungepufferten Ereignisplan nicht in der absoluten Dauer unterbieten; dies folgt direkt aus dem Modell.')
h('Puffernutzung', 2)
table(['Verfahren','Verbrauch / Gesamtpuffer','Lokale Aufnahme / 33 ZE'],
      [[n,pct(v['event']['metrics']['utilization']),pct(v['event']['metrics']['used']/33)] for n,v in results.items()],
      [4*cm,6*cm,6.5*cm])
p('Verbrauch = Summe min(Puffer, Zusatzdauer); Überhang = Summe max(0, Zusatzdauer - Puffer). Die lokale Aufnahme ist nicht gleich der vermiedenen Projektverlängerung. Unterschiedliche Gesamtpuffer werden offen ausgewiesen; die Parametrisierung ist kein Vergleich bei identischem Pufferbudget.')
h('Reservierte Ressourcenauslastung: Basis / Ereignis', 2)
table(['Verfahren','R1','R2','R3','R4'],
      [[n]+[f"{100*v['base']['metrics']['resources'][k]:.1f} / {100*v['event']['metrics']['resources'][k]:.1f} %" for k in range(4)] for n,v in results.items()],
      [3*cm]+[3.375*cm]*4)
p('Berechnung je Ressource: Summe(Bedarf x effektive Dauer) / (Kapazität x Projektdauer). Enthält reservierte Pufferzeit; keine Aussage über produktive Arbeitsleistung. In allen acht Plänen bleiben die Werte im zulässigen Bereich.')

page(); h('Ereignisliste und Erklärung einzelner Wirkungen')
p('Die gespeicherte CSV wurde unverändert eingelesen. Ein Kontrollzug mit Python random.Random(20260913), sample aus den sortierten IDs 2 bis 61 und anschließend randint(1,5) in Ziehungsreihenfolge reproduziert exakt dieselbe Liste. Dieser Kontrollzug ist keine neue Ereignisrealisierung.')
event_acts = {a['id']: a for a in results['Standard']['event']['activities']}
table(['ID','Welle','Basisdauer ZE','Zusatzdauer ZE','Fest p_i','Prop. p_i','Dyn. p_i'],
      [[i,event_acts[i]['wave'],event_acts[i]['duration'],e]+[next(a['buffer'] for a in results[n]['event']['activities'] if a['id']==i) for n in names[1:]]
       for i,e in sorted((int(i),e) for i,e in data['events'].items())],
      [1.3*cm,1.5*cm,3*cm,3*cm,2.5*cm,2.5*cm,2.7*cm])
p('Zwölf Ereignisse entsprechen 20 % der Aktivitäten: mehrere beobachtbare Fälle bei einer noch überschaubaren Tabelle. 1 bis 5 ZE erzeugen unterschiedliche ganzzahlige Zusatzdauern bei Basisdauern von 1 bis 10 ZE. Beide Festlegungen und die Gleichverteilung sind eigene Vereinfachungen, keine empirischen Störungsraten. Bei kurzen Aktivitäten sind 5 ZE eine starke relative Verzögerung. Die Liste wurde nicht nach Pufferergebnissen verändert.')
for wave in [2,13]:
    pieces=[]
    for n,v in results.items():
        a=[a for a in v['base']['activities'] if a['wave']==wave]
        b=[a for a in v['event']['activities'] if a['wave']==wave]
        db=max(x['end'] for x in a)-a[0]['start']; de=max(x['end'] for x in b)-b[0]['start']
        pieces.append(f'{n} {db} -> {de} ZE')
    p(f'Welle {wave}, Dauer ohne -> mit Ereignissen: '+ '; '.join(pieces)+'.')
p('In der letzten Welle liegt nur Aktivität 59: Basisdauer 3 ZE, Zusatzdauer 3 ZE. Standard benötigt 6 statt 3 ZE. Fest benötigt 6 statt 5 ZE; proportional 6 statt 4 ZE; tiefenabhängig 6 statt 6 ZE. Der tiefenabhängige Puffer verhindert hier eine zusätzliche Abweichung, hat die drei Reserve-ZE aber schon im Basisplan eingeplant. Dieses Beispiel erklärt einen beobachteten Effekt; es wurde nicht eigens als weiteres Szenario konstruiert.')

page(); h('Prüfungen, Grenzen und Reproduktion')
table(['Prüfung','Ergebnis / Zweck'],[
    ['Eingaben','60 echte Aktivitäten, vier Kapazitäten, zwölf eindeutige gültige Ereignis-IDs'],
    ['Zufallsroutine','CSV stimmt exakt mit Kontrollgenerierung aus Seed 20260913 überein'],
    ['Standardplan','13 Wellen und 114 ZE; Wellenfolge stimmt mit gespeicherter September-Vorrechnung überein'],
    ['Alle acht Pläne','Vorgängerenden vor Starts; Ressourcenprüfung an sämtlichen Intervallgrenzen'],
    ['Strukturtreue','Wellen werden einmal gebildet und für alle Varianten übernommen'],
    ['Zeitrechnung','Fortgeschriebener Endtermin gleich unabhängig berechneter Summe der Wellenmaxima'],
    ['Pufferrechnung','Verbrauch + Überhang = 33 ZE; Rundungs- und Absorptionsgrenzen geprüft'],
], [4*cm,12.5*cm])
p('Diese Prüfungen kontrollieren die Umsetzung der Regeln, nicht deren allgemeine Praxistauglichkeit. Keine statistischen Aussagen, keine weiteren Projekte, keine Parameteroptimierung. Der tiefenabhängige Puffer ist eine eigene Tiefenheuristik; spätere Reserven holen keine bereits entstandenen früheren Verzögerungen auf.')
p('Reproduktion vom Projektordner aus: python data/reference/psplib/run_example.py. Danach: python data/reference/psplib/render_example_report.py. Python-Version des Laufs: '+data['python'].split()[0]+'. Das erste Skript erzeugt ergebnisse.json und aktivitaeten.csv; das zweite diesen Bericht, Markdown sowie alle acht PNG-Grafiken und deren ZIP-Paket. Die Eingabe-CSV wird nicht überschrieben. SHA-256-Prüfsummen für Projekt, Ereignisliste und Rechenskript stehen in ergebnisse.json.')
p('Die 9/10 beziehen sich auf die Eignung dieses dokumentierten Beispielvorgehens. Für eine abschließende Bewertung der gesamten Bachelorarbeit oder der Java-Implementierung wäre diese Zahl nicht belegt. Vor einer Beschreibung als Java-Auswertung müssen vier Ressourcen und die bestätigten Pufferformeln dort umgesetzt und mit diesen Referenzwerten abgeglichen werden.')
h('Quellen und Beleggrenzen',2)
p('[1] Kolisch, R.; Sprecher, A. (1997): PSPLIB - A project scheduling problem library. European Journal of Operational Research 96(1), 205-216. DOI: 10.1016/S0377-2217(96)00170-1. Belegt die Benchmarkbibliothek, nicht die selbst gewählten Ereignis- oder Pufferparameter.')
p('[2] Offizielle PSPLIB: https://www.om-db.wi.tum.de/psplib/library.php und getdata.php?mode=sm. Originaldatei j601_1.sm aus dem J60-Archiv, heruntergeladen am 13.09.2026; Bibliotheksbeschreibung geprüft am 05.10.2026. Die Website nennt teils 1996; die Zeitschriftenfassung ist von 1997.')
p('[3] Thünemann, Julius (2023): Robuste Personaleinsatzplanung im RCPSP. Bereitgestellte Bachelorarbeit, S. 27-29 (Projektgröße), S. 38-40 (gruppierter Ablauf), bereits im Leitfaden geprüft. Eigene methodische Anknüpfung; kein Beleg für identischen Quellcode.')
p('Alle Ergebniszahlen sind eigene Berechnungen des hier gespeicherten Referenzskripts. Die Annahmen zur Wellenbarriere und zur Pufferwirkung entsprechen dem vereinbarten Modell und benötigen keine nachträglich erfundene Fremdquelle.')

# Regenerate all eight standalone images from the same saved results.
import runpy
runpy.run_path(str(Path(__file__).with_name('export_gantt_images.py')), run_name='__main__')
for n,v in results.items():
    for kind,label,slug in [('base','ohne Ereignisse','ohne-ereignisse'),
                            ('event','mit Ereignissen','mit-ereignissen')]:
        page();h(f'{n}: Plan {label}')
        p(f"Basisplan {v['base']['metrics']['makespan']} ZE; Ereignisplan {v['event']['metrics']['makespan']} ZE; Abweichung {v['extension']} ZE. Alle Diagramme verwenden dieselbe Zeitachse und die unveränderte Standardreihenfolge.")
        path=OUT/'gantt-bilder'/f'gantt-{n.lower()}-{slug}.png'
        story.append(Image(str(path),width=16.5*cm,height=22*cm))
        md.append(f'![{n}: {label}](gantt-bilder/{path.name})\n')


def footer(canvas,doc):
    canvas.setFont('Body',8);canvas.setFillColor(colors.HexColor('#566574'))
    canvas.drawString(1.8*cm,1.1*cm,'j601_1 | Python-Referenzlauf | 05.10.2026')
    canvas.drawRightString(19.2*cm,1.1*cm,str(doc.page))

SimpleDocTemplate(str(PDF),pagesize=(21*cm,29.7*cm),rightMargin=1.8*cm,leftMargin=1.8*cm,
                  topMargin=1.6*cm,bottomMargin=1.7*cm,title='Beispiellauf j601_1 - Puffervergleich',author='Projektanalyse').build(story,onFirstPage=footer,onLaterPages=footer)
md.append('\n## Vollständige Rechendaten\n\n[JSON](ergebnisse.json) | [Alle 480 Planaktivitätszeilen als CSV](aktivitaeten.csv)\n')
(OUT/'beispiellauf.md').write_text('\n'.join(md),encoding='utf-8')
print('Created Markdown and PDF report.')
