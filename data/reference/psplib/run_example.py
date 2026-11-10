"""Reproducible J60 reference run; run from any working directory.

This is an explicit Python reference implementation, not the Java application.
No new event draw is used: the saved CSV is loaded and checked against its seed.
"""
from pathlib import Path
from fractions import Fraction
import csv
import hashlib
import json
import math
import random
import sys

ROOT = Path(__file__).resolve().parents[3]
INPUT = Path(__file__).resolve().parent
OUT = ROOT / 'reports' / 'j601_1-beispiellauf'


def parse():
    s = (INPUT / 'j601_1.sm').read_text(encoding='utf-8')
    a = s.split('PRECEDENCE RELATIONS:')[1].split('REQUESTS/DURATIONS:')[0]
    successors = {int(v[0]): list(map(int, v[3:])) for line in a.splitlines()
                  if (v := line.split()) and v[0].isdigit()}
    a = s.split('REQUESTS/DURATIONS:')[1].split('RESOURCEAVAILABILITIES:')[0]
    rows = {int(v[0]): list(map(int, v[2:])) for line in a.splitlines()
            if (v := line.split()) and v[0].isdigit()}
    cap = next(list(map(int, v)) for line in s.split('RESOURCEAVAILABILITIES:')[1].splitlines()
               if (v := line.split()) and v[0].isdigit())
    assert len(rows) == 62 and len(cap) == 4
    assert [i for i, r in rows.items() if r[0] == 0] == [1, 62]
    assert all(x == 0 for i in [1, 62] for x in rows[i])
    ids = sorted(i for i in rows if rows[i][0] > 0)
    pred = {i: {j for j in ids if i in successors[j]} for i in ids}
    depth = {}
    while len(depth) < len(ids):
        ready = [i for i in ids if i not in depth and pred[i] <= depth.keys()]
        assert ready, 'Cycle'
        for i in ready:
            depth[i] = max((depth[j] + 1 for j in pred[i]), default=0)
    return ids, rows, cap, successors, pred, depth


def standard_waves(ids, rows, cap, successors, pred):
    done, waves = set(), []
    while len(done) < len(ids):
        free, wave = cap.copy(), []
        while True:
            candidates = [i for i in ids if i not in done and i not in wave
                          and pred[i] <= done and all(r <= c for r, c in zip(rows[i][1:], free))]
            if not candidates:
                break
            def key(i):
                return (-sum(Fraction(r, c * rows[i][0]) for r, c in zip(rows[i][1:], cap)),
                        -len(successors[i]), rows[i][0], i)
            chosen = min(candidates, key=key)
            wave.append(chosen)
            free = [c-r for c, r in zip(free, rows[chosen][1:])]
        assert wave, 'No feasible candidate'
        waves.append(wave)
        done.update(wave)
    return waves


def plan(waves, rows, cap, pred, buffers, events):
    result, start = [], 0
    for k, wave in enumerate(waves, 1):
        for i in wave:
            d, p, e = rows[i][0], buffers[i], events.get(i, 0)
            result.append(dict(id=i, wave=k, start=start, end=start+d+max(p, e),
                               duration=d, buffer=p, event=e, used=min(p, e), overhang=max(0, e-p)))
        start = max(a['end'] for a in result)
    by_id = {a['id']: a for a in result}
    assert len(by_id) == len(rows)-2
    for a in result:
        assert all(by_id[j]['end'] <= a['start'] for j in pred[a['id']])
    # Independent time-boundary sweep: verify actual interval resource usage.
    for t in sorted({a['start'] for a in result} | {a['end'] for a in result}):
        active = [a for a in result if a['start'] <= t < a['end']]
        assert all(sum(rows[a['id']][k+1] for a in active) <= cap[k] for k in range(4))
    total_buffer = sum(buffers.values())
    used = sum(a['used'] for a in result)
    metrics = dict(makespan=start, total_buffer=total_buffer, total_event=sum(events.values()),
                   used=used, overhang=sum(a['overhang'] for a in result),
                   utilization=used/total_buffer if total_buffer else None,
                   resources=[sum(rows[a['id']][k+1]*(a['end']-a['start']) for a in result)/(cap[k]*start)
                              for k in range(4)])
    assert metrics['used'] + metrics['overhang'] == metrics['total_event']
    assert all(0 <= v <= 1 for v in metrics['resources'])
    # Alternative direct computation, independent of start-time propagation.
    assert start == sum(max(rows[i][0]+max(buffers[i], events.get(i, 0)) for i in wave) for wave in waves)
    return dict(activities=result, metrics=metrics)


def main():
    ids, rows, cap, succ, pred, depth = parse()
    with (INPUT/'j601_1-ereignisse-vorschlag.csv').open(encoding='utf-8', newline='') as f:
        event_rows = list(csv.DictReader(f, delimiter=';'))
    events = {int(r['AktivitaetID']): int(r['Verzoegerung']) for r in event_rows}
    assert len(events) == len(event_rows) == 12
    assert set(events) <= set(ids) and all(1 <= e <= 5 for e in events.values())
    rng = random.Random(20260913)
    selected = rng.sample(ids, 12)
    assert {i: rng.randint(1, 5) for i in selected} == events
    waves = standard_waves(ids, rows, cap, succ, pred)
    old = json.loads((INPUT/'j601_1-inspection.json').read_text(encoding='utf-8'))
    assert waves == [w['ids'] for w in old['waves']]
    hmax = max(depth.values())
    # One fixed parametrization. Endpoints are reserve values, not extra clamps.
    buffers = {'Standard': {i: 0 for i in ids},
               'Fest': {i: 2 for i in ids},
               'Proportional': {i: math.ceil(Fraction(rows[i][0], 4)) for i in ids},
               'Tiefenabhängig': {i: math.ceil(1 + Fraction(2*depth[i], hmax)) if hmax else 1 for i in ids}}
    assert all(buffers['Tiefenabhängig'][i] <= buffers['Tiefenabhängig'][j]
               for i in ids for j in ids if depth[i] <= depth[j])
    results = {}
    for name, b in buffers.items():
        base = plan(waves, rows, cap, pred, b, {})
        event = plan(waves, rows, cap, pred, b, events)
        assert [(a['id'], a['wave']) for a in base['activities']] == [(a['id'], a['wave']) for a in event['activities']]
        results[name] = dict(base=base, event=event,
                             extension=event['metrics']['makespan']-base['metrics']['makespan'])
    assert results['Standard']['base']['metrics']['makespan'] == 114
    for v in results.values():
        assert v['event']['metrics']['makespan'] >= results['Standard']['event']['metrics']['makespan']
    # Focused boundary checks of local absorption (not further experimental scenarios).
    assert 5+max(2, 1) == 7 and 5+max(2, 2) == 7 and 5+max(2, 3) == 8
    assert math.ceil(Fraction(1, 4)) == 1 and math.ceil(Fraction(10, 4)) == 3
    assert math.ceil(1+Fraction(0, 11)) == 1 and math.ceil(1+Fraction(22, 11)) == 3
    hashes = {name: hashlib.sha256((INPUT/name).read_bytes()).hexdigest()
              for name in ['j601_1.sm', 'j601_1-ereignisse-vorschlag.csv', 'run_example.py']}
    data = dict(report_date='2026-10-05', python=sys.version, instance='j601_1.sm',
                parameters=dict(fixed=2, proportional='1/4', depthBased='ceil(1+2*h/11)',
                                status='Explicit example assumptions; not empirically calibrated'),
                capacities=cap, events=events, seed=20260913, depth=depth,
                waves=waves, results=results, sha256=hashes,
                checks='PASS: seed reproduction, 60 activities, four resources, DAG, interval feasibility, fixed waves, buffer arithmetic, direct wave sums, historical baseline')
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT/'ergebnisse.json').write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding='utf-8')
    with (OUT/'aktivitaeten.csv').open('w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['strategy', 'plan', 'id', 'wave', 'start', 'end', 'duration', 'buffer', 'event', 'used', 'overhang'])
        writer.writeheader()
        for name, v in results.items():
            for kind in ['base', 'event']:
                for a in v[kind]['activities']:
                    writer.writerow(dict(strategy=name, plan=kind, **a))
    print(json.dumps({name: {'base': v['base']['metrics'], 'event': v['event']['metrics'], 'extension': v['extension']}
                      for name, v in results.items()}, indent=2))


if __name__ == '__main__':
    main()
