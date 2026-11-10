"""Export all eight saved reference plans as standalone 300-dpi PNGs."""
from pathlib import Path
import json
import zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
REPORT = ROOT/'reports/j601_1-beispiellauf'
OUT = REPORT/'gantt-bilder'
OUT.mkdir(exist_ok=True)
data = json.loads((REPORT/'ergebnisse.json').read_text(encoding='utf-8'))
order = [a['id'] for a in data['results']['Standard']['base']['activities']]
font_dir = Path('C:/Windows/Fonts')
small = ImageFont.truetype(str(font_dir/'arial.ttf'), 29)
normal = ImageFont.truetype(str(font_dir/'arial.ttf'), 34)
title = ImageFont.truetype(str(font_dir/'arialbd.ttf'), 48)
palette = ['#356a94', '#ab7ad1', '#b9cfdf', '#d35f51']
files = []
for name, pair in data['results'].items():
    for kind, label, slug in [('base','ohne Ereignisse','ohne-ereignisse'),
                              ('event','mit Ereignissen','mit-ereignissen')]:
        plan = pair[kind]
        acts = {a['id']: a for a in plan['activities']}
        assert set(acts) == set(order)
        im = Image.new('RGB', (2400, 3200), 'white')
        d = ImageDraw.Draw(im)
        d.text((90,55), f'{name}: {label}', font=title, fill='#203b51')
        d.text((90,130), f"j601_1.sm | 60 Aktivitäten | Projektdauer: {plan['metrics']['makespan']} ZE", font=normal, fill='#334b5d')
        d.text((90,185), 'Prioritätsregelbasierte wellengebundene Planung | Python-Referenzlauf', font=small, fill='#52616d')
        for k,(color,legend) in enumerate(zip(palette,['Basisdauer','Verbrauchter Puffer','Unverbrauchter Puffer','Zusatzdauer über Puffer'])):
            x=140+(k%2)*1120; y=260+(k//2)*60
            d.rectangle((x,y+5,x+40,y+33), fill=color)
            d.text((x+58,y),legend,font=normal,fill='#253b4b')
        x0,x1,y0,step=180,2310,470,41
        scale=(x1-x0)/160
        for t in range(0,161,10):
            x=x0+t*scale
            d.line((x,y0-20,x,y0+60*step),fill='#dce3e9',width=2)
            d.text((x-18,y0-70),str(t),font=small,fill='#34434f')
            d.text((x-18,y0+60*step+15),str(t),font=small,fill='#34434f')
        d.text((55,y0-70),'ID',font=normal,fill='#34434f')
        for row,i in enumerate(order):
            a=acts[i]; left=a['start']; y=y0+row*step
            for length,color in zip([a['duration'],a['used'],a['buffer']-a['used'],a['overhang']],palette):
                if length:
                    d.rectangle((round(x0+left*scale),y,round(x0+(left+length)*scale)-1,y+29),fill=color)
                left += length
            assert left == a['end']
            d.text((55,y-3),str(i)+('*' if a['event'] else ''),font=small,fill='#253b4b')
            if row and a['wave']!=acts[order[row-1]]['wave']:
                d.line((45,y-6,x1,y-6),fill='#a5b5c3',width=2)
        d.text((1010,3020),'Zeiteinheiten',font=normal,fill='#253b4b')
        d.text((90,3090),'* = Ereignis | Horizontale Trennlinien = Wellen | Gleiche Zeitachse in allen Bildern',font=small,fill='#52616d')
        path=OUT/f'gantt-{name.lower()}-{slug}.png'
        im.save(path,dpi=(300,300)); files.append(path)
        with Image.open(path) as check:
            assert check.size==(2400,3200)
            check.verify()
with zipfile.ZipFile(REPORT/'gantt-diagramme-alle.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in files:z.write(p,p.name)
print(f'Exported and verified {len(files)} PNGs and ZIP archive.')
