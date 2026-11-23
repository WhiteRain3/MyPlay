# Speed limit coverage in Lithuania: why roads show no limit, and what to do

Research only, 2026-11-23. No code has changed.

## Why so many roads show "no limit"

The file builder (`speedlimit/.../Build.kt`, `LithuaniaRules.rules`) is conservative on purpose:
"a road whose limit is not tagged and not implied by its kind is left out". A road gets a limit
only from one of these:

1. its own `maxspeed` tag;
2. a parseable `maxspeed:type`, `source:maxspeed` or `zone:maxspeed` (`LT:urban` 50,
   `LT:rural` 90, `LT:motorway` 130, `LT:living_street` 20), marked DEFAULTED;
3. `highway=motorway` (130) or `highway=living_street` (20), also DEFAULTED.

Everything else is dropped. That means a trunk, primary, secondary, tertiary, unclassified,
residential or service road with no tag shows "–". In Lithuania that covers most minor roads and
most town streets. Mappers there tag `maxspeed` mainly on main roads, and they seldom add
`LT:urban` or `LT:rural`.

### Numbers

The last CI build (run 44935716324, extract from 2026-11-20) printed:

    lithuania: 579665 ways, 60018 pieces, 2986665 bytes

- 579,665 is every `highway=*` way in the extract. That includes footways, paths, tracks and
  cycleways, which are never roads for cars here.
- 60,018 is the number of *pieces* written. A way with a limit gives at least one piece: more if
  it is long or has a limit for each direction. So **at most about 60,000 ways have a limit**,
  probably well under that.
- I could not count car roads with and without `maxspeed` by class. This container's network
  blocks download.geofabrik.de, and osmium isn't installed here. A one-line CI step would give
  the exact table, and I suggest adding it before the change is built (see "Next step").
  Lithuania's OSM road network has a few hundred thousand car-road ways (residential, service
  and unclassified make up most of them). So the honest estimate is that **roughly one car road
  in five, or fewer, has a limit in the file today**, and almost all of those are the main
  roads.

### A second, smaller cause to keep in mind

Even on a tagged road, the phone's matcher can show "–" when the GPS fix is far from the way or
the heading doesn't fit, for example in a tunnel, at a junction or in a parking lot. That isn't
a coverage problem, and the options below don't change it. The drive summary already logs
signed vs by-road time, so a drive log can separate the two causes.

## Options

The phone already supports a "probably" limit. `LimitShown.Known(kmh, defaulted)` carries the
flag from the file's DEFAULTED bit, and DriveSummary counts it apart. **But no skin draws it
differently yet.** ORBIT, DECK and the test skin all show the number the same way. Any option
that adds defaults needs that small UI change too (see option A).

### A. Lithuanian default limits by road type and urban/rural, in the CI builder (recommended)

Lithuanian traffic rules (KET) for cars:

- **In settlements (urban): 50.**
- **Outside settlements: 90** on asphalt or concrete, **70 on gravel** (`surface=gravel`,
  `unpaved`, `compacted`, `dirt` and so on).
- **Expressway: 120. Motorway: 130.** Both 110 in winter, which the builder already applies to
  classes 0–1.
- **Living street: 20.**

How the builder would decide "in a settlement", all offline in CI:

1. Use explicit tags first, as now (`maxspeed`, then `maxspeed:type` / `zone:maxspeed` /
   `source:maxspeed`).
2. Otherwise, a way whose midpoint lies inside a settlement area counts as urban (50):
   - `place=city|town|village|hamlet` areas and `boundary=administrative` settlement polygons
     where they exist;
   - otherwise `landuse=residential|commercial|retail|industrial` polygons, which are mapped
     well in Lithuania.
3. Otherwise it counts as rural: 90, or 70 if unpaved.
4. `highway=residential` outside any polygon still gets 50, since a residential street in a
   small village whose area isn't mapped is most likely inside the town signs.
5. `highway=service` gets no default. Parking aisles and driveways would only add noise and
   size.

The osmium step has to keep those polygons too (`tags-filter` with `place`, `landuse` and
`boundary` added). The builder needs a point-in-polygon test, which is plain Kotlin in CI. The
phone is unchanged except for the display.

How to show it: the number in a **dashed or dimmed ring** instead of the solid sign, with
"probably" in the small line under it (where "map data" is now). The limit pulse and any
"over the limit" colour don't fire for a probable limit.

- **Coverage gain:** large. Every car road except service roads gets a number, so roughly from
  one road in five to nearly all.
- **Accuracy risk:** medium on its own, and shown honestly as "probably":
  - The town sign rarely sits exactly on the polygon edge, so the limit can be wrong by up to a
    few hundred metres at town entries.
  - Some 70 or 80 stretches outside towns are signed but not tagged. A wrong 90 there would be
    too high, which is the bad direction.
  - Main roads (trunk, primary) are the best-tagged roads, so this risk falls mostly on minor
    roads, where 90/70/50 matches KET.
  - A signed tag always wins over a default.
- **Size:** the file is 3.0 MB today. With residential, unclassified, tertiary and secondary
  roads added, I estimate **about 3–4 times the pieces, so roughly 9–12 MB**. Two ways to keep
  it down:
  - join adjacent ways that have the same limit and class into one piece run;
  - leave out service roads.

  It is still a once-a-month download over the existing opt-in.
- **Privacy:** no change. It is built in CI from the same Geofabrik extract and downloaded over
  the same opt-in. The phone makes no new calls.

### B. Waze's own limit when it sends one

The androidx car-app library that Waze talks to our host through has **no speed-limit field**.
`NavigationTemplate`, `RoutingInfo`, `Step` and `Trip` carry maneuvers, distances, lanes and
ETAs, but never a limit. Waze draws its limit inside its own map surface. We would only get it
from pixels (OCR of the map surface) or from the Xposed hook route (N22). Both are fragile and
break with every Waze update, and OCR costs battery and CPU.

- **Coverage gain:** large where Waze is running, zero otherwise. Waze's limits are good in
  Lithuania.
- **Accuracy risk:** low for the value itself, but high for the plumbing breaking silently.
- **Size:** nothing.
- **Privacy:** fine. It stays on the phone. But it means reading another app's screen or memory.

Not recommended as the main fix. It could be a later extra: when a Waze limit can be read, it
wins over a "probably".

### C. Other open sources

- **Lithuanian state road data (Via Lietuva / the former LAKD, geoportal.lt):** the state road
  register covers national roads (magistraliniai, krašto, rajoniniai). Whether a *speed-limit*
  layer is published openly, and under which licence, still needs checking. Even if it is, it
  covers only state roads, which are already the best-tagged part of OSM, and not town streets,
  where the gaps are. Joining it to OSM geometry is real work.
  - **Coverage gain:** small to medium. **Accuracy:** high where it exists.
  - **Size:** small.
  - **Privacy:** fine if built in CI.
  - **Licence:** unknown.
- **Mapillary traffic-sign detections:** open data with detected speed-limit signs as points.
  It would need a CI download, given a token, and sign-to-road matching. Coverage outside
  Vilnius and Kaunas is thin, and detections can be wrong (80 read as 30, signs for side
  roads).
  - **Coverage gain:** small. **Accuracy risk:** medium. **Size:** small.
  - **Privacy:** fine in CI; never from the phone.
- **Commercial sources (HERE, TomTom):** paid and online. That rules them out.

## Recommendation

**Option A, built in CI, shown as "probably N" in a distinct style.** It is the only option that
closes the gap where it actually is (town streets and minor roads). It follows the country's
written rules, so it is right most of the time and honest when it might not be. It adds no
network call and no new permission. The phone already carries the DEFAULTED flag end to end.
The work:

1. The builder: settlement polygons, the KET defaults above, the gravel rule, joining runs.
2. The workflow: keep the polygons in the osmium filter, and print a per-class coverage table.
3. The skins: draw DEFAULTED limits in a dashed or dimmed ring with "probably", and leave out
   the pulse.

Waze's limit (option B) can be added later on top, if N22 makes it readable. Option C isn't
worth doing now.

## Next step (before building)

Add one count step to the workflow and run it once. It needs no code change to the app:

    osmium tags-count work/roads.osm.pbf -m 'highway=*' > highway.txt
    osmium tags-filter work/roads.osm.pbf w/maxspeed -o work/with-maxspeed.osm.pbf
    osmium tags-count work/with-maxspeed.osm.pbf -m 'highway=*' > highway-with-maxspeed.txt

That gives the exact share of each road class with and without a limit. It confirms the size
estimate and shows how much option A would add.

## Option A as built (2026-11-23): the defaults and what they assume

The builder (`speedlimit/`, `LithuaniaRules.defaultLimit`, `Settlements`) gives a road with no
limit tagged, and none implied by its kind or zone, a default from the rules of the road. Every
default is marked DEFAULTED ("probably"), and the car draws it as an estimate. A limit tagged on
the road always wins.

**The rule for every default: when in doubt, the lower one.** The file must never show a limit
higher than the real one may be. A default that is too low only colours the speed too early; one
that is too high tells the driver that a speed is fine when it is not.

| Road, nothing tagged | Default | Why |
|---|---|---|
| Inside a settlement | 50 | KET's limit in settlements. |
| Outside a settlement, paved | 90 | KET's limit outside settlements. |
| Outside a settlement, unpaved (`surface`=gravel, dirt, compacted, ground and so on) | 70 | KET's limit on gravel. |
| `highway=residential`, anywhere | 50 | A residential street is assumed to be inside the town signs, even where no settlement is mapped around it. |
| Motorway or motorway link | 110 | The lowest a motorway has (its winter limit); 130 only when tagged. |
| Expressway (`motorroad=yes`) outside settlements | 110 | The lowest an expressway has (winter); 120 only when tagged. |
| Service roads (parking aisles, driveways) | none | No sensible default. |

**What counts as a settlement:** a point inside
- an area tagged `place=city/town/village/hamlet`, or `landuse=residential/commercial/retail/industrial`
  (each area with its holes outside); or
- within a radius of a settlement mapped only as a point (most Lithuanian villages are): 700 m round
  a hamlet or village, 1.5 km round a town, 3 km round a city. The radii are generous on purpose:
  inside means 50, the lower guess.

The test is made at the middle of each piece of road (a piece is at most about 10 km and 32 points),
so a road leaving a town changes from 50 to 90 at a piece boundary, not exactly at the sign.

**Known limits of the estimate:**
- At a town's edge the change from 50 to 90 can be a few hundred metres off the sign.
- A village mapped neither as an area nor as a point gets 90 on its roads. The coverage table in the
  build log, and car drives, will show how often this happens.
- Industrial or retail areas beside a rural road give 50 where the sign may say 90. That is too low,
  the safe direction.
- Signed 70 or 80 stretches outside towns that are not tagged get 90. Only the map (a tag) can fix
  these, so the estimate is drawn as one.

The build prints a coverage table by road class (signed, from the road's kind or zone, from these
defaults, none), and the release notes say that limits on untagged roads are estimates.

**Two files (SL-5).** The file with these estimates is published as `speed-limits-<region>-v2.bin`
(and its `-v2.json`), beside `speed-limits-<region>.bin` built as before (signed and kind limits
only), so an app that does not draw estimates never downloads one. The format is the same (1);
only the name differs. Apps that draw the "probably" sign fetch the `-v2` file.
