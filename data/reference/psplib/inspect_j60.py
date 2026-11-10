from pathlib import Path
from fractions import Fraction
import json
import random

root = Path('data/reference/psplib')
s = (root / 'j601_1.sm').read_text(encoding='utf-8')
a = s.split('PRECEDENCE RELATIONS:')[1].split('REQUESTS/DURATIONS:')[0]
succ = {int(v[0]): list(map(int, v[3:])) for line in a.splitlines()
        if (v := line.split()) and v[0].isdigit()}
a = s.split('REQUESTS/DURATIONS:')[1].split('RESOURCEAVAILABILITIES:')[0]
rows = {int(v[0]): list(map(int, v[2:])) for line in a.splitlines()
        if (v := line.split()) and v[0].isdigit()}
cap = next(list(map(int, v)) for line in s.split('RESOURCEAVAILABILITIES:')[1].splitlines()
           if (v := line.split()) and v[0].isdigit())
ids = {i for i in rows if rows[i][0] > 0}
pred = {i: {j for j in ids if i in succ[j]} for i in ids}
depth, ef = {}, {}
while len(depth) < len(ids):
    ready = [i for i in sorted(ids - depth.keys()) if pred[i] <= depth.keys()]
    assert ready, 'Cycle'
    for i in ready:
        depth[i] = max((depth[j]+1 for j in pred[i]), default=0)
        ef[i] = rows[i][0] + max((ef[j] for j in pred[i]), default=0)
done, waves, t = set(), [], 0
while done != ids:
    free, wave = cap.copy(), []
    while True:
        eligible = [i for i in ids-done-set(wave) if pred[i] <= done
                    and all(x <= y for x, y in zip(rows[i][1:], free))]
        if not eligible:
            break
        def key(i):
            return (-sum(Fraction(r, c*rows[i][0]) for r, c in zip(rows[i][1:], cap)),
                    -len(succ[i]), rows[i][0], i)
        i = min(eligible, key=key)
        wave.append(i)
        free = [x-y for x, y in zip(free, rows[i][1:])]
    assert wave
    assert all(pred[i] <= done for i in wave)
    assert all(sum(rows[i][k+1] for i in wave) <= cap[k] for k in range(len(cap)))
    end = t + max(rows[i][0] for i in wave)
    waves.append(dict(wave=len(waves)+1, start=t, end=end, ids=wave))
    done.update(wave)
    t = end

# One proposed draw, fixed rule and seed; no filtering by outcomes.
rng = random.Random(20260913)
chosen = rng.sample(sorted(ids), 12)
events = sorted([dict(id=i, delay=rng.randint(1, 5)) for i in chosen], key=lambda x:x['id'])
out = dict(instance='j601_1.sm', activities=len(ids), capacities=cap,
           durations_min_max=[min(rows[i][0] for i in ids), max(rows[i][0] for i in ids)],
           sum_durations=sum(rows[i][0] for i in ids), depth_range=[min(depth.values()), max(depth.values())],
           critical_path_without_resources=max(ef.values()), wave_makespan=t, waves=waves,
           event_rule=dict(count=12, duration_uniform_inclusive=[1,5], seed=20260913,
                           algorithm='Python random.Random; sample sorted IDs, then randint in sampled order'),
           proposed_events=events)
(root/'j601_1-inspection.json').write_text(json.dumps(out, indent=2), encoding='utf-8')
print(json.dumps(out, indent=2))
