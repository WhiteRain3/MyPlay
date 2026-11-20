# MyPlay: ordered task list

Planner. First written 2026-11-18 from unchanged AARE at `37f8236`; revised 2026-11-19 after the
first release, the owner's decisions and the fork report (`docs/research/forks.md` on branch
`claude/research-forks`). File names and counts come from reading and text searches; a worker
checks each before relying on it. Commit ids of other people's work are as the fork report gives
them; nothing from them has been built or run by us. Where something is not known, it says "not
known".

## Where things stand (2026-11-20, afternoon)

Released up to **0.1.16** (tags `myplay-v0.1.0` to `myplay-v0.1.16`). As reported by the
dispatcher; the Planner has not checked each release's contents.

| Tasks | State |
|---|---|
| A1 to A4, A7, B1, B2 (bench) | Done |
| The owner's parked test | Passed with 0.1.0 (2026-11-19). The connection log with buttons, knob, screen size and sound has **not** arrived yet |
| K2 to K11 (bench round 1) | Done. K1 (the Tester's long walks) not reported |
| W1 (Waze host shims) | Done, shipped since 0.1.2. Not yet tried by the owner as far as known |
| Phase E (phone app design) | Done, except B2 ("press it in the car"), which needs R8 |
| M1 to M4, M6 first half, M3-3 | Done |
| F2, F4, F6, D3, K9 | Done |
| R1, R2, R3 | Done. R4 to R7 with the car Worker now |
| B1 (Buttons page) | Pushed after 0.1.16, under review |
| P1, P2 | Done (tasks given by the dispatcher outside this plan) |

What is left, and what waits for the car, is in the next section.

## Next, safe without the car (2026-11-20)

**Phone app worker** (idle now), most valuable first:
1. **F3, colours for a look.** A user's own look stored as data, edited with the colour picker
   from AA-Experiments (`ThemeUi.kt`, `ColoursUi.kt`), each colour tied to the part of the car
   picture it paints, with a readable-contrast warning. Phone files plus a small data file on
   the car side for the stored look (ask the car Worker to add it, or agree the file first).
2. **F7 (new), whole looks.** A named look bundles everything visible: theme colours, layout
   preset (M1), wallpaper, status icons and clock (F4), bar items. Pick from a few built in,
   save your own, and share one as a file (built on F6's backup format, holding only the look).
3. **F5, wallpaper editor.** Crop, darken and blur from AA-Experiments (`CarWallpaperEditor.kt`,
   `core/ImageBlur.kt`), writing the file the car already reads.
4. **I2, an updater that installs.** Download and install the new release when the owner taps,
   instead of opening the release page (AA-Experiments `Updater.kt`, `core/UpdateCheck.kt`).

**Car Worker**, after R4 to R7 (then phase N, one original car screen, below the M and R sections, after the owner picks a design language):
1. **G1, keys into mirrored apps**, then **G2, the pointer**, driven by the pad's arrow keys. The
   bench sends arrow keys, so this can be built and tested now; only the Audi's actual codes wait
   for R8.
2. **M6 second half** (choosable bar items from upstream's `feat/rail-status-widgets`).
3. **F1, MyPlay's own dark looks** (themes in `ui/theme/`), which F7 then bundles.
4. **I4, a log the owner can send without a PC.**
5. **W3**, music apps through the host shims, one app at a time, if the owner wants them.

## Waits for the owner's connection log from the car

What to do: with 0.1.1 or newer, connect in the car, then turn the dash knob both ways, push it,
hold it in, push the pad up, down, left and right, press Menu and Back, hold Back, scroll and
click the steering-wheel wheel and hold it; then *Advanced > Connection log*, save and send it.
Also say: what the screen looked like (a photo), whether sound came through the car, whether
Waze opened (with the host shims module), and whether Google's Android Auto got in the way.

| Waits | Why |
|---|---|
| R8 (real button codes, Back's early hold), B2 | Which code each button sends; whether a held key repeats |
| C5 (was "the Audi's buttons") | Now part of R8 |
| Hold-and-turn gestures | Whether a knob turn arrives while a button is held |
| D2 final sizes, the free area for M | The screen size and margins the box really reports, and a photo |
| M5 / R5 moving the map with the pad | Only the codes; the behaviour itself is built with R5 |
| C4 | Whether the knob works with the phone locked in the car |
| W2 | Only if Waze still refuses with the module installed |
| A5, A6, A8 | Only if the log shows a start-up problem, a wrong picture, or Google's app interfering |
| H2 (sound) | Where sound goes while MyPlay is connected |

## Bench round 1: what the Tester found on 0.1.0, and what to do about it

Source: the Tester's report on 0.1.0 (PC bench, Audi profile, the owner's phone). **All of this
was seen on the bench, none of it in the car.** The bench sends knob turns, the knob press and
Back the same way a car does, so fixes that need only those three hold whatever the box sends.
Anything that needs the knob's tilt or the car's other buttons waits for the parked test.

What worked: it connects in about a second every time, with MyPlay's own certificate; no
crashes; the knob works with the phone locked; nothing is drawn under the 80-pixel strip; day
and night switch; the ring moves on the home screen, the bar and the app grid; pressing opens
things; Back returns from the grid, the call list and the voice bar.

Each finding was checked against the code (file and line given where found), against upstream's
unmerged branches (nothing there fixes any of these; `fix/aa18-input` is about long presses and
touchpads, `feat/hide-builtin-apps` helps K8), and against JVKeller's knob commits `570a67de`
and `a08c2073`, which were read, not run.

### Safe to do now (turn, press and Back only)

**K1. Find out exactly how far the knob gets inside an app (Tester, no code change).**
- Finding: in MyPlay's car Settings and in Organic Maps the ring "jumps straight to the bar" and
  no option was reached.
- From the code: the Settings options are ordinary focusable buttons, and the choice buttons
  already draw a ring (`media/MediaWidgets.kt` `Pill`). On the home screen the Tester saw focus
  go through the content, then the bar, then wrap. So the options may be reachable after the
  bar's last item, or may never be; the report's walks were too short to tell. The cause is
  **not established**.
- Do: two long walks on the bench, 30 single turns forward and 30 back, one picture per turn,
  starting just after opening Settings and just after opening Organic Maps.
- Tells us: whether K2 is only "start in the right place" or also "the content cannot be entered
  at all".

**K2. When an app or a new page opens, the ring starts on its first item.** *(Pushed, under
review, not seen on a device. Not called done until the Tester's K1 walks are in.)*
- Finding: same as K1, and it is the Tester's "fix first".
- From the code: `ui/car/CarShell.kt:153-157` asks for focus on the content when the screen
  changes, but does so before the new screen is laid out, and silently gives up if it fails. The
  next turn then starts from the bar. Hosted apps (Organic Maps) have a second form of it: every
  press brings a new page and focus is lost again. JVKeller found and fixed exactly that
  ("focus started over from the rail", `570a67de`, in `host/CarAppScreen.kt`: ask again one
  frame after the new page is laid out).
- Do: take JVKeller's change for hosted apps (keep the author), and apply the same "ask again
  once it is laid out" in `CarShell.kt` for MyPlay's own screens (Settings, Phone,
  Notifications). Do **not** take the "panes" part of his later commit: with panes, turning
  stays inside one area and only a tilt leaves it, and it is not known whether the Audi's knob
  sends a tilt.
- Bench check: open Settings from the grid, turn once: the ring is on the first option. Keep
  turning: every option gets the ring in order, the page scrolls with it, then the bar, then
  back to the first option. Same in Organic Maps for Search, Categories and the zoom buttons.

**K3. Back never traps.** *(Pushed, under review, not seen on a device. "Holding Back" relies on
the car sending a long press; whether the box does is not known, and the bench script cannot
play it. The double press does not depend on that.)*
- Finding: Back did nothing in Fermata Mirror.
- From the code: in a hosted app Back is handed to the app and nothing else happens
  (`host/CarRuntime.kt:671-682`, `host/CarAppConnection.kt:183`). An app that is on its first
  page ignores it, so there is no way out except the bar.
- Do: if Back is pressed again within two seconds and the app has not changed its page, go
  home. Holding Back goes home at once. (Our own rule; no fork has this.)
- Bench check: open Fermata Mirror, Back twice: home screen. In Organic Maps, open Search, Back
  once: back to the map, still inside the app.

**K4. A ring you can see, on everything.** *(Pushed, under review, not seen on a device.)*
- Findings: the play button only gets slightly darker; the app grid shows a faint grey tile and
  no ring; the "Car audio" message's close button was never reached.
- From the code: the ring is a 3 dp border in the accent colour (`ui/car/CarFocus.kt`); on a
  button already filled with the accent colour it cannot be seen. Grid tiles, notices and the
  Settings switch rows have no ring at all (plan task C1's table).
- Do: JVKeller's `a08c2073` has this part ("an inner ring and a veil so it shows on filled
  buttons", and rings on grid tiles, notices, notifications, Settings switches, the phone
  screen). Take those pieces by hand, with credit, without the panes and the cursor. Add C1's
  test that fails when something tappable has no ring.
- Bench check: the `home` walk and a grid walk: every stop shows a clear ring, including play.

**K5. No stop without a visible ring on the home screen; maps buttons that fit.** *(Pushed,
under review, not seen on a device.)*
- Findings: one stop on the home screen shows nothing; "Organic Maps" is cut to "Organ / ic /
  M…".
- From the code: the card draws up to four maps buttons in a row that cannot scroll
  (`ui/car/CarShell.kt:494`, `navApps.take(4)`); on this screen the row is wider than the card,
  so the rest are clipped but still take focus. That matches the report (Waze is installed), but
  nobody has seen the hidden button, so it stays "probably".
- Do: show only as many buttons as fit, names on one line with "…", and a "More" button that
  opens the list when some are left out.
- Bench check: `home` walk: the number of stops equals the number of things you can see.

**K6. The ringed thing is always on screen.** *(Pushed, under review, not seen on a device. The
Worker's open points: it may or may not cure the ring drawn below the edge, which only the
bench can show; and text below the last item that can take focus still cannot be reached with
the knob, because the knob moves from item to item and does not scroll by itself.)*
- Findings: the grid's last entry ("Exit to car") is ringed below the bottom edge; the Settings
  page continues below the edge.
- Do: when focus moves, scroll so the focused item is fully visible, in the app grid and in
  every list.
- Bench check: walk the grid to its end and the Settings page to its end: the ring is always
  fully visible.

**K7. The bench script's small faults.** *(Pushed, under review, not run on Windows yet. New
option `-Tile`; see `docs/TESTER_SETUP.md`.)*
- Findings: garbled characters in `headunit-out.txt`; the PowerShell profile's output mixed in;
  results land inside the repository checkout; the log filter lets unrelated lines through.
- Do: write the file as UTF-8, add `-NoProfile` to the documented command, default the results
  folder to outside the checkout (or list it in `.gitignore`), tighten the filter. Add walks
  `settings`, `maps`, `grid-end` and `back-out` so K1 to K6 are one command each.
- Check: the Tester runs it.

**K8. An app grid you can tell apart.**
- Findings: six entries all called "Android Auto" (one answers "Could not start Android Auto"),
  three "Phone", two "Google", two "Settings" plus "SETTINGS".
- From the code: the grid lists every component on the phone that offers a car screen; several
  belong to Google's Android Auto app, which MyPlay cannot host.
- Do: leave out entries that belong to Google's Android Auto app; show the app's own name plus
  the page's name when two entries would look the same; the phone's Apps page keeps the last
  word. Upstream's `feat/hide-builtin-apps` (`b69aecd`) hides the built-in Phone and
  Notifications entries from the grid and can be taken with it.
- Bench check: a picture of the grid: no two entries with the same name, none that cannot start.

### Small, safe, less urgent

**K9. Opening a maps app from the home screen should not quietly change the home screen.**
- Finding: the app opened from a maps button becomes the remembered maps app (`last_nav`), and
  its picture replaces the buttons from then on; with Fermata that was an "Unlock the phone"
  panel, later a black rectangle.
- This is how AARE means it to work (the last maps app owns the card). For MyPlay: a setting on
  the phone, "Maps on the home screen: last used / a chosen app / always ask", and fall back to
  the buttons when the remembered app shows nothing.
- Bench check: open Fermata from a button, go home: the card is as the setting says.

**K10. The Music entry with nothing playing.**
- Finding: it opens the full app grid.
- From the code: with no music app remembered, the bar's Music button opens the grid
  (`ui/car/CarShell.kt`, the same fallback as for maps). Do: open a list of music apps only.
- Bench check: press Music on a fresh install: only music apps are listed.

**K11. The first picture is dark.**
- Finding: every connection starts dark, then turns light when the head unit reports day.
- Plan task F2 ("Always dark", on by default, which is what the owner wants anyway) removes the
  switch-over. Do F2 early; it needs only the bench (`night` / `day` in a walk).

### Must wait for the parked test

| What | Why it waits |
|---|---|
| Panes: turning stays in one area, a tilt jumps to the next (JVKeller `a08c2073`) | Needs to know whether the box passes on the knob's tilt. Without a tilt, panes would trap the ring |
| The knob-driven pointer for mirrored apps and the browser (G2) | The same question: tilt, or a long press to change direction |
| Giving the Audi's buttons their actions (C5) | Needs the key codes the box really sends; 0.1.1 writes them to the phone's log |
| Final sizes for the small screen (D2), beyond K5 | Needs the screen size and margins the box really reports, and a photo of the real screen |
| A5, A6, A8, and everything about sound | Only the box can show whether they are needed |

Noted, nothing to do yet: with the phone locked the knob worked, although the phone's log had
"Touch mode switch rejected" once (the open question in C4); `drive` then `park` showed no
difference on the home screen, and it is not known whether anything there should change.

## After the car: the owner's three requests (2026-11-20)

The owner asked for these "only if nothing more important is waiting". Honest order against the
bench fixes: K2 to K7 first, because without them the knob cannot use any app at all, Waze
included. W1 is small and comes next. M is the biggest piece of work in this plan so far.

### W. Waze

The owner's words: "waze doesn't work ... it says something like allow app in settings and only
supporting google auto". The exact text they saw is not known. MyPlay has one message that
matches: "Waze only accepts Google's Android Auto. It needs to allow MyPlay (see the app list on
the phone)." (`carapp_error_google_only`).

What is going on, from the code and `docs/xposed.md`:

- Waze checks who is hosting its car screen against a list of Google's apps (name and signing
  key). MyPlay is not on it. Nothing in MyPlay's settings or Waze's settings can change that.
- AARE's answer for rooted phones is a separate small app, the "host shims" module in `xposed/`,
  loaded into Waze by an LSPosed-type framework (which the owner has). It makes Waze's check say
  yes when the host is MyPlay.
- That module is **not built or shipped** by any MyPlay release, and upstream's own notes say it
  was never verified on a device.
- **It cannot work as it is.** Three faults, found by reading:
  1. It looks for Waze's check under the name `androidx.car.app.HostValidator`. In the car
     library the class is `androidx.car.app.validation.HostValidator` (checked in the library
     file this project builds with). The hook is never installed.
  2. Its settings file (`META-INF/xposed/module.prop`) is not in the form the owner's working
     module in AA-Experiments uses (`minApiVersion=101`, `targetApiVersion=101`,
     `staticScope=true`), and it is built against version 102 of the hooking library while the
     owner's framework is known to support 101. Whether the framework would load it is not known.
  3. It is still called "AARE host shims" with id `net.prototux.aare.xposed` (the Reviewer's L4),
     and one line of its settings file was damaged by the rename (`scope=` now holds MyPlay's id).
- **Not known:** whether Waze's published app keeps that class under its real name. Apps often
  shorten internal names when they are built. If Waze did, the hook must find the check by its
  shape instead (W2).

So there are no steps the owner can take today. It needs a build from us first.

**W1. Make the module work for MyPlay and ship it.**
- Touches: `xposed/` only (a separate app; MyPlay itself is not changed): the class name; the
  settings file in the form and library version the owner's framework is known to load; name
  "MyPlay host shims", id `io.github.whiterain3.myplay.hostshims`; signed with the release key;
  built by CI and attached to the release as `MyPlay-hostshims-<version>.apk`. First version:
  only the Waze-type hook, and the module's default list holds only Waze. The hooks into
  Android's own system process (`android` in the list) are left out for now: upstream itself
  warns they are best-effort, and a wrong one can stop the phone from starting.
- Verified by: a unit test that the class the hook names exists in the car library the project
  builds with (so this fault cannot come back). Bench: the Tester's bench uses the owner's phone,
  which has Waze, so it can be checked without the car: open Waze from the app grid; its map
  appears instead of the "only accepts Google's Android Auto" message. Car: the owner drives a
  route with it.
- The owner's steps once it is released: install `MyPlay-hostshims-<version>.apk`; in the
  LSPosed-type manager enable "MyPlay host shims" and tick **only Waze**; force-stop Waze (or
  restart the phone); connect to the car and open Waze.

**W2. Only if W1 does not get through: find Waze's check by its shape.**
- If the bench still shows the refusal, the phone's log says whether the hook found the class.
  If not, Waze renamed it, and the hook must look for it another way. Needs the Tester to pull
  Waze's APK from the phone for reading. Not known until W1 is tried.

**W3. Later: the music apps the same module can open up** (Spotify, YouTube Music and others
refuse a non-Google host in a similar way; `docs/xposed.md` describes the hooks). Only after W1
is proven, one app at a time.

Earlier this plan listed "hooks that make Google Maps or Waze accept the app" under "do not
copy". That was about another fork's abandoned hook collection. The module here is upstream
AARE's own, already in this repository, and the owner has asked for Waze; the entry below is
reworded.

### M. The map behind everything, cards and bar on top

The owner's words: "the style itself, the design. Of course customizable from settings, like can
I have navigation nicely shown as the background, and on top of it, the cards, the bar." And:
"Important to follow the scaling correctly that I saw on waze, when it's only visible on the
left, it's more centered there, if it's expanded it goes to the other direction and so on where
space emptied out." They attached a picture of Google's current Android Auto on three screen
shapes: the map fills the screen, with a narrow bar and small floating cards on top, placed
differently per shape.

That picture is the idea, not the look. The result is MyPlay's own design (Phase F's looks apply
to it); F-Auto's and other forks' layout files are for reading only.

What exists today, from the code:

- On the home screen the map is one card among others (`ui/car/CarShell.kt`, `Dashboard`).
- Inside a maps app's own full screen, AARE already does what the owner describes: every card it
  draws over the map is marked (`Modifier.obstructsMap()`, `host/templates/MapUi.kt`), the
  largest free rectangle is worked out (`host/templates/MapGeometry.kt`, with unit tests), and
  the app is told through the car library's "visible area" and "stable area" messages
  (`host/CarAppConnection.kt:245-255`). A maps app that honours them centres itself in the free
  part and moves when it changes.
- So the new work is a home screen where the maps app's picture fills the screen and MyPlay's
  own cards and bar are those marked obstacles.
- Layouts are a setting already (`ui/car/CarLayout.kt`: bar with cards, dock, classic), so this
  is a fourth one and the others stay.
- Upstream has nothing like it on any branch (`feat/dashboard-cards` is already in `main`;
  `feat/rail-status-widgets` adds choosable items for the bar and is worth taking in M4).
- It only works with a maps app MyPlay can host: Organic Maps, OsmAnd, CoMaps and others built
  on the car library, and Waze once W1 works. Google Maps on the phone only gives MyPlay its
  next-turn text, not a map.

**The owner's decision (2026-11-20):** "add all those layouts for me to pick out on the app ...
Or literally just add a bunch of customization options like map as background, apps as pills".
The Design reviewer has drawn three car-screen layouts (A left rail and card column, B bottom
dock with corner cards, C top ribbon with right rail and one stage card; `docs/design-options/
car-screen/` on branch `claude/design-options` of AA-Experiments), is drawing three more where
the map runs under the bar too, and one sheet that turns all six into combinable options.

**So a layout is data, not a screen file.** The fork report's lesson: every fork that added a
look copied a whole shell file and then could not take upstream's fixes. Here one new screen
file draws any combination of options, and "A", "B", "C" and the three to come are named sets of
options (presets). AARE's three existing layouts stay as they are and stay selectable.

The Design reviewer's sheet (`customisation-system.png`, AA-Experiments `claude/design-options`
at `b0a721f`) settles the options: eight, the same for every layout, and six presets.

| # | Option | Values |
|---|---|---|
| 1 | Map as the background | on / off (plain dark) |
| 2 | Map goes under the bar | under it / stops at its edge |
| 3 | Bar style | solid rail / see-through rail / floating pills / bottom dock / top ribbon |
| 4 | Bar side | left / right / bottom / top |
| 5 | Where cards sit | column by the bar / corners / one stage card / top banner + corner |
| 6 | Card see-through amount | 0 to 100 %, always over a dark base |
| 7 | Which cards, in what order | next turn, arrival time, music, clock and status; "one extra at a time" for the stage layout |
| 8 | Hide after a while | never, or music and arrival time after 5 to 60 s |

| Preset | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| A Left rail + column | on | beside | solid rail | left | column by the bar | 14 % | all four | never |
| B Bottom dock | on | beside | bottom dock | bottom | corners | 14 % | all four | never |
| C Top ribbon | on | beside | top ribbon | top | one stage card | 14 % | one extra at a time | never |
| D See-through rail | on | under | see-through rail | left | corners | 40 % | all four | never |
| E Pills along the bottom | on | under | floating pills | bottom | corners | 40 % | all four | music after 15 s |
| F Pill column on the right | on | under | floating pills | right | top banner + corner | 40 % | all four | never |

**Rules** (from the sheet): a dock only at the bottom, a ribbon only at the top; cards never sit
in the free area; cards keep a dark base at any see-through amount; the knob reaches everything
in every combination. Choosing a preset sets all eight; any one can then be changed on its own.

**Proposals, not decisions:** the 14 % and 40 % amounts, the 15-second hide in E, and putting
the bar on the right next to the strip the car may not show. They depend on what the owner finds
readable in the car, and on the screen size and margins the box really reports.

Which preset is the default: not decided. The owner picks after seeing them on the bench.

**M1. The layout as data.**
- Touches: one new file `ui/car/CarLayoutSpec.kt` holding the eight options as one value, the
  six presets A to F from the table, the rules as one function that turns any combination into
  an allowed one, and reading and writing them through one new file
  of settings (`MyPlayLayoutSettings.kt`, extension properties on `Settings` so `Settings.kt` is
  not edited by two streams at once). No drawing yet.
- Verified by: unit tests: every preset round-trips through the settings; an option combination
  that breaks a rule (a dock on the left, a ribbon at the bottom) is corrected to the nearest
  allowed one; each preset's values match the table above.

**M2. Tell the maps app where the free space is, and keep telling it.** *(Already queued for
the Worker.)*
- Touches: `host/templates/MapGeometry.kt` and its callers. Tests for the arrangements the six
  layouts produce (a column on one side; corner cards; one stage card; a bar under the map).
  "Stable area" is what is free with every card the layout allows; "visible area" is what is
  free right now.
- Verified by: Tests. Bench: Organic Maps' position arrow sits in the middle of the free part;
  start music so the music card appears and the arrow moves; stop it and it moves back. Car: the
  same with Waze, where the owner saw it.

**M3. One screen file that draws a spec.**
- Touches: new `ui/car/SpecShell.kt`, one more value in `CarLayout.kt` ("Custom"), three lines
  in `CarShell.kt` to choose it. The maps app's picture fills the screen (or the area beside the
  bar); the bar in the chosen style; the existing dashboard cards in the chosen slots; every one
  of them marked as covering the map so M2 does its work. Falls back to the wallpaper when no
  maps app is chosen.
- Verified by: Tests on slot placement. Bench: one picture per preset with Organic Maps, compared
  side by side with the Design reviewer's drawings.

**M4. The knob on these layouts.**
- Turning goes through the bar and the cards in reading order; reading-only cards (next turn,
  arrival time, clock) are never a stop, as the drawings show. Moving and zooming the map with
  the knob waits for the box's answer about tilt (C3).
- Verified by: bench walks per preset: the number of stops is as the drawing says.

**M5. Choosing and editing it on the phone.** (Phone stream, Phase E: task E6.)

**M6. Placement by screen shape, and items for the bar.**
- Wide and very wide screens get side-by-side slots; the Audi's small one (about 900 x 543 in
  layout units) gets few, narrow cards. Upstream's `feat/rail-status-widgets` (clock, outside
  temperature, battery, weather as choosable bar items) is taken here.
- Verified by: tests on placement for the Audi's size and the three shapes in the owner's
  picture; bench pictures at each size (`--res`).

It only works with a maps app MyPlay can host: Organic Maps, OsmAnd, CoMaps, and Waze with the
host shims module (0.1.2). Google Maps on the phone gives only its next-turn text, not a map.

Safe now, on the bench: M1, M2, M3, M4 (turn, press and Back), M6's placement rules. Needs the
car: the real screen size and margins (the free area must leave out what the car does not show),
final card sizes for the Audi, moving the map with the knob.

### R. Controls: the steering-wheel wheel, the dash knob, gestures and remapping

The owner's words (2026-11-20): "Could you do holding the rotary clicked in (press and hold)
changes the song? what other gestures can we add? As I just have a scroll with a click on the
wheel, and I want to somehow make it fully usable, that I could use simply, intuitively and do
stuff that I need. then a rotary on the dash, with dpad on top of the knob, menu and back works.
want the dpad to work just as a tv remote does if possible."

So the car has two controls:
1. a **scroll wheel with a click** on the steering wheel;
2. the **MMI knob** on the dash: turn, push, a four-way pad on top of it, and **Menu** and
   **Back** buttons.

**Not known until the owner sends a connection log from 0.1.1 or later** (it records every key
and knob step with its code): which codes the box sends for each; whether the wheel and the knob
send different codes (if they send the same, MyPlay cannot tell them apart and they share one
set of gestures); whether a held button arrives as one long down, as repeated downs, or with a
"long press" flag; whether the box passes Menu at all or keeps it for its own screen; whether a
knob turn can arrive while a button is held (needed for "hold and turn"). Everything below is
built so the codes are a table filled in later, not code.

What exists, checked first (rule 6):
- Upstream `fix/aa18-input` (6 commits, not merged): long presses made from a held key, repeats,
  no long press on focus keys, the hold's start time kept. This is the base of the gesture
  engine; take it.
- Upstream `feat/car-key-remap` (`6c810cf`, 3 commits): a "Car buttons" page listing every key
  seen from the car, each choosable from a list of actions; the lookup is a pure, tested function
  (`car/CarKeyMap.kt`). Take it as the base of the action table; its page is replaced by the
  phone worker's (B1).
- MyPlay already has `car/HeldKey.kt` (press and hold measured, used for Back) and
  `car/LongPressKey.kt` (Home).
- The four-way pad is ordinary arrow keys. Arrow keys are already handed to the car screen
  (`CarSession.onKey`, the `else` branch), and the screen's own focus search moves the ring in
  the direction pressed. So "like a TV remote" mostly exists; it needs checking on every screen
  (some rows trap). This is also what JVKeller's "panes" (`a08c2073`) needed a tilt for: with the
  pad, jumping between the bar and the cards is just pressing toward them. Panes as such are not
  needed.

**Proposed default gestures** (for the owner to approve; every one can be changed on the phone):

| Control | Gesture | Default |
|---|---|---|
| Dash knob | turn | move the ring to the next / previous item, in reading order |
| | push | open or press what has the ring |
| | push and hold (about 0.6 s) | **next song** (the owner's request) |
| | double push | previous song |
| | hold and turn | volume of the song in MyPlay's own player only if the car leaves volume to the phone; otherwise skip songs one by one. Only if the box allows it |
| Pad on the knob | up / down / left / right | move the ring in that direction, like a TV remote; at the edge of an area, into the next area |
| | hold a direction | keep moving (repeat) |
| Menu | press | home; pressed on home, the app grid |
| | hold | voice assistant |
| Back | press | back |
| | hold | home (built in K3) |
| | two quick presses | leave an app that ignores Back (built in K3) |
| Wheel on the steering wheel | turn | same as the knob's turn (if the codes are the same, it is the same) |
| | click | play / pause |
| | hold | next song |
| | double click | previous song |

Where a screen needs it, the same gestures do the obvious local thing:
- **Hosted map app** (Organic Maps, Waze): turn moves through its buttons; push on the map itself
  enters "move the map": the pad pans, turning zooms, push or Back leaves (JVKeller `570a67de`).
- **Music** (now playing): left / right on the pad when the ring is on the progress bar seeks 10 s.
- **Mirrored phone app** (YouTube): the pad moves a pointer (faster while held), push taps, turn
  scrolls, Back goes back inside the app, two quick Backs leave (plan G2).
- **A call ringing**: push answers, Back declines (JVKeller `deda4fb2` puts focus on Answer).

**Feedback:** each gesture that does something away from the ring (next song, previous, volume,
assistant) shows a short line at the top of the car screen for about 1.5 s ("Next song: <title>").
No sound. A setting turns these off.

**One trade-off to tell the owner:** a double push means a single push has to wait a moment
(about a quarter of a second) to see whether a second one follows. So double push is off on the
knob while the ring is on a list or a button, and only used where a single push has no other
meaning (the music card, the home screen). If the owner finds even that slow, double push is
turned off and "previous song" moves to the pad (left on the music card).

Tasks for the car Worker, in order:

**R1. Take upstream's input branches.** Merge `fix/aa18-input` and `feat/car-key-remap` as they
are (one merge commit each, rule 6), drop or hide upstream's own buttons page.
- Verified by: their tests; all existing tests; bench `open-back` and `back-out` walks unchanged.

**R2. The gesture engine.** A pure class that turns the key and knob stream into press, hold,
double, hold-and-turn and repeat, per key code, with the timings as settings (hold 0.6 s, double
0.25 s). Built on R1's held-key handling; replaces `HeldKey` for Back without changing what Back
does.
- Verified by: unit tests for every gesture, a lost key-up, a hold with and without the car's
  flag, repeated downs.

**R3. The action table and its defaults.** One table: (control, gesture) to action, with the
defaults above, stored through the car side's settings file. Codes for "wheel", "knob push",
"pad", "Menu" start as the standard Android codes the bench sends (arrows, Enter or centre,
Menu, Back, media keys) and are corrected from the owner's log in R8.
- Verified by: unit tests (every default resolves; an unknown key does nothing; a remap wins).

**R4. The pad like a TV remote, everywhere.** Walk every car screen with the arrow keys and fix
the places where the ring cannot get out (rows that trap, the bar not reachable sideways, lists
that do not scroll with the ring).
- Verified by: bench walks with `dpad:` events on home, grid, Settings, a hosted map app, music;
  each screen reachable in all four directions.

**R5. The local meanings.** Map move mode (from JVKeller `570a67de`), seeking on the music
progress bar, push to answer a call; the pointer for mirrored apps comes with G2.
- Verified by: bench walks per screen; car for the map.

**R6. The on-screen line for gestures.**
- Verified by: bench pictures after a hold and a double push.

**R7. The bench script learns holds.** New events: `hold:CODE,MS`, `double:CODE`,
`holdturn:CODE,N` (needs a small change to the desktop head unit to send a key down, wait, then
up), and walks `gestures` and `pad`.
- Verified by: the Tester runs them.

**R8. The real codes.** When the owner's connection log arrives: fill in which code is the
wheel, the knob push, the pad, Menu and Back; if the wheel and knob share codes, say so to the
owner and give them one set. Kept as a rule for this box (`vehicles.json` style), so another
car does not inherit it.
- Back's hold (R2): today the hold fires at 0.6 s while Back is still held, so a lost key-up
  also goes home (Reviewer R2-2). When the owner's log shows whether the box repeats downs while
  a key is held: if it does, fire early only after a repeat down (proof it is still held) and
  otherwise on release; if it does not, keep it as it is. (Dispatcher's decision, 2026-11-20.)
- Verified by: the owner in the car.

For the Phone app worker (after R3 is in):

**B1. The Buttons page** (`ui/phone/ButtonsScreen.kt`, new). In the "Edit the picture" style:
a drawing of the two controls (wheel; knob with its pad, Menu and Back); tap a part, then see its
gestures (push, hold, double, hold and turn) each with its current action; tap one to choose from
the action list; "Reset to the defaults". Reads and writes only through the car side's table
(R3); no setting of its own. A tile "Buttons" in "Behind the scenes" opens it.
- Verified by: unit tests on what the page writes; Tester screenshots; the bench shows a changed
  gesture doing the new action.

**B2. "Press it in the car".** While connected, pressing a car button highlights it on the page
(from the same log as A7), so the owner can see which part of the drawing a button is. Waits for
R8.

Safe now, on the bench: R1 to R7 and B1. Needs the car: R8, B2, and whether "hold and turn" is
possible at all.

## Phase N: one original car screen (planned 2026-11-20, reworked the same evening)

The owner's words: "Sounds like you didn't actually change the original layout and stuff. What
would it be hard to maybe like remove all of the old one and redo with fully our custom layout
setup? Because there are more things that are different that I dislike."

**The owner's decisions (2026-11-20, through the dispatcher):**
1. "Don't care about future updates": deleting AARE's old car shells and screens is fine. For
   the car UI, phase N does not need to keep upstream merges easy (I1). AARE's credits and the
   GPL licence stay.
2. A **fully original design language** for the whole car screen, not inspired by Android Auto
   or CarPlay. The Design reviewer is drawing three languages; the owner picks one.

So phase N is: pick a language, then rebuild the whole car UI on it. The steps below are
in outline. **Each step is detailed only after the owner's pick**, because the language decides
what the bar, the cards and the screens even are.

**Why the screen still looks like AARE.** The "map behind" layout replaced only the home
screen and the bar. It draws apps, the app grid, notifications, calls, car settings and the
music browser through the same AARE code as the old layouts (`ContentArea`, `Launcher`,
`NoticeOverlay`, `CallOverlay` in `CarShell.kt`, and the screens in `ui/car/`). Removing the old
layouts alone would not change those, so phase N rebuilds them too.

### N-checklist: everything the car screen does today

Every row must work in the new UI before the old code is deleted (N9). From the code at 0.1.22
(`ui/car/CarShell.kt`, `DockShell.kt`, `ClassicShell.kt`, `SpecShell.kt`, `CarLayoutSpec.kt`).

From the code at 0.1.22 (`ui/car/CarShell.kt`, `DockShell.kt`, `ClassicShell.kt`,
`SpecShell.kt`, `CarLayoutSpec.kt`).

| Piece | Old shell(s) | Spec shell today | Gap |
|---|---|---|---|
| Home without a maps app | taskbar: dashboard with the "Where to?" map card | wallpaper plus the maps buttons panel (M3-3) | Small. No "plain colour" choice; covered by N1 |
| Home as cards | taskbar: map, media, calendar, notification, weather cards in a chosen order (`DashCard`, `DashboardOrder.kt`, `DashboardWidgets.kt`) | next turn, arrival, music, clock and status | **Calendar, notification and weather cards, and the map as a card**, are missing (N2) |
| Home as an app grid | classic: home *is* the app grid (`launcherHome`). Dock: an icon grid as home (`DockShell.Home`) | home is always the cards | Missing as a choice (N2, N3) |
| App grid (launcher) | all, the same `Launcher` | the same `Launcher` | None in function. Its look is AARE's (N3) |
| Full-screen app with the bar | all, the same `ContentArea` | the same, bar beside it, no cards | None in function |
| Notifications | all: `NoticeOverlay` banner, notifications app | the same, plus a bell in the bar | None in function. Look is AARE's (N5) |
| Calls | all: `CallOverlay`, `CallCard.kt`, `PhoneScreen.kt` | the same | None in function. Look is AARE's (N5) |
| Car settings screens | all: `CarSettingsScreen.kt`, `CarDataScreen.kt`, `DashboardSettings.kt`, `MyPlayStatusSettings.kt` | the same | They offer the layout choice (taskbar, dock, classic, map behind); that choice goes in N8. Look is AARE's (N6) |
| Voice button | taskbar rail, dock, classic bottom bar | in the bar (`MicButton`) | None |
| Music browser | all: `BrowserScreen.kt` | the same | None in function |
| Bar items (rail widgets) | taskbar rail: clock, temperature, battery, weather (`RailWidgets.kt`), segment clock, phone status | the same items (`barItems`), phone status, network icons | None. The `sideRailFit` shrinking for short screens is taskbar-only (N2) |
| App shortcuts in the bar | taskbar: nav, media, phone, settings. Dock: a column of app icons, recent apps first | nav, media, phone, notifications | **A row/column of chosen or recent apps** is missing (N2, N3) |
| Now-playing pill and turn badge in the bar | classic bottom bar (`ActivityPill`, `TurnBadge`) | not in the bar (cards show them) | Optional bar items (N2) |
| Driver on the right | dock and taskbar swap sides | the bar side is fixed by the preset (left/right/top/bottom) | Mirror "left/right" to "driver side / passenger side" (N2) |
| Portrait screens | dock moves to the bottom; taskbar goes horizontal | `SpecPlacement` works on any size; presets are drawn for landscape | Check and fix placement on tall screens (N2) |
| Split views | none: no shell shows two apps side by side. The map card next to other cards is the closest | — | Not ported; new work if the owner wants it (not in this phase) |
| Phone picture of the car screen | `CarPicture.kt` draws whichever shell is chosen | works with map behind | Follows automatically. Remove the old choices from it (N7) |
| Car check (phone) | `CarCheckPage.kt`, `CarCheckLog.kt` | does not depend on the layout | None |
| Knob walks and tests | `PadWalkTest` walks taskbar, dock, classic, map behind; `CarStylesTest`, `BarPageTest`, `MyPlayWeatherTest` name `CarLayout.TASKBAR` etc. | `mapBehindHome`, `mapBehindHomeAudi`, `launcherAudi`, `settingsScreenAudi` | Move every old-shell case to a preset (each step's tests) |
| R4 (pad like a TV remote) | walked on all shells | walked | Each new card and bar item gets a pad walk |
| R5 (map move mode, seeking) | the map card is a stop on the taskbar home | the map behind is not a knob stop (`LocalMapTapIsKnobStop` false) | With "map as a card" (N2) the map card must be a stop for R5. With the map behind, R5 needs its own way in (a long press, R5's own task) |
| Styles that pick a shell | `CarStyle`: AARE, Android Auto, Material You → taskbar; Android Auto 2019 → classic; CarPlay → dock | — | Styles must stop choosing a shell (N8) |
| Phone pages | `LayoutScreen.kt` (four choices), `BarPage.kt` and `CardsPage.kt` (notes per shell), setup wizard | map behind options with presets | Show only the new UI (N7) |


### What stays: the engine, not the look

These are not AARE's look and the new UI is built on them, whatever the language:

- layout as data (`CarLayoutSpec`, `SpecPlacement`): the options are re-cut to the language,
  but the idea of presets plus options, stored as named keys, stays;
- the map's free area (`MapGeometry`, `obstructsMap()`, the visible and stable area messages):
  what makes a maps app centre itself in the free part;
- the car model and actions (`CarModel.kt`), the knob helpers (`MyPlayFocus.kt`, `CarFocus.kt`,
  `PageInput.kt`), the phone status sources, the hosted-app and mirror code (`host/`, `mirror/`).

Settings ids already released are never changed; new ones are added beside them.

### The steps, in outline

| Step | What | Who | Rough size |
|---|---|---|---|
| N0 | **The owner picks a language** from the Design reviewer's three | owner | — |
| N1 | Foundation: the language as code. Colours, type, shapes, spacing, motion, the focus ring, and the basic parts (button, card, list row, toggle, slider) in a new package, with a screenshot test of each | car Worker | 2 |
| N2 | Home and the bar replacement: whatever the language puts where AARE has a rail. Background is one option (map, wallpaper, plain colour, or the map as a card), with the free-area reporting kept | car Worker | 2 to 3 |
| N3 | Getting to apps: what replaces the app grid and the app list, with the knob | car Worker | 1 to 2 |
| N4 | Music: the card, now playing, the browser | car Worker | 2 |
| N5 | Notifications and calls: banner, list, incoming call, in-call | car Worker | 2 |
| N6 | Car settings screens (MyPlay's own and the car data screen) | car Worker | 1 to 2 |
| N7 | Phone side: the layout page, bar and cards pages, setup wizard and the phone picture show the new UI only | phone worker | 1 to 2 |
| N8 | The switch: everyone on the new UI; stored layouts move to the nearest preset, keeping card order and bar items | car Worker | 1 |
| N9 | Delete the old code (taskbar, dock and classic shells and the replaced AARE screens), keep credits and licence | car Worker | 1 |

About **13 to 17 chunks**; a chunk is one change that gets one review and one release. N1 comes
first; N2 to N6 in the order the owner cares about most; N7 alongside; N8 and N9 last.
Until N8 the new UI is one more layout choice ("MyPlay"), so every release before the switch
is safe and the owner can try each piece in the car. No new work goes into the old shells.

**Tests and bench for every step:** a Robolectric screenshot of each new screen, day and night,
at 1280x720 and the Audi's 1200x723; a pad walk of each screen (R4: every stop reachable,
one ring, Back returns); unit tests for placement and stored settings. Bench walks per step:
`home` (N2), `grid-end`, `open-back`, `back-out` (N3), `night-day` (all), `settings` (N6),
`maps` (N2, with a maps app). N8 adds a migration test from each old layout id and style, and
a 0.1.x backup (F6) restored. N9 removes the old shells' cases from `PadWalkTest`,
`CarStylesTest`, `BarPageTest` and `MyPlayWeatherTest`.

**Deleting the old code (N9).** The owner has accepted that later upstream changes to the car UI
cannot be taken. Delete in one commit after N8 has shipped and the owner has driven with it.
Protocol, transport and hosting code are not part of phase N, so upstream fixes there can still
be taken (I1) for as long as those files are not rewritten. The shared parts the new UI still uses
move out of `CarShell.kt` into MyPlay files before it is deleted.

### Map colours: can the map itself, Waze included, be recoloured? (research, 2026-11-20)

The owner's words: "Maybe it would be possible to fully recolor and change Waze? The map itself
even." From the code and what is known about the apps; nothing here has been tried.

How the map reaches the screen today: a maps app built on the car library (Organic Maps, OsmAnd,
CoMaps, Waze once W1 works) draws its map itself into a drawing surface MyPlay hands it
(`host/CarAppScreen.kt`, `MapSurface`, a `SurfaceView`). MyPlay never sees the map's contents.
It only places that surface on the car screen.

| Way | What it can do | Effort | Fragility |
|---|---|---|---|
| **1. A colour filter on the map surface, in MyPlay** | Tint, hue shift, saturation, brightness, contrast, darker nights, for **any** maps app, Waze included. While a filter is on, draw the map in a `TextureView` with a colour-matrix layer paint (`setLayerPaint`, works from Android 8, MyPlay's minimum); with no filter it stays today's `SurfaceView`, at no extra cost. Off by default. A later version could pass the picture through a small GPU step with a colour lookup table, mapping the app's own colours to the look's colours | Filter: 1 chunk. Lookup table: 1 to 2 more | Low; it does not depend on the app's insides. Limits: it recolours everything the app draws on its map, including its own buttons and speed sign; it cannot tell water from roads, only one colour from another; Waze's own day and night palettes give two different starting points. A `TextureView` costs one extra GPU copy, which a 1280x720 picture at car frame rates should bear. The knob and touch forwarding need checking after the swap |
| **2. Open-source maps with a style of our own** | A fully custom map design: every road, label and colour. **OsmAnd** reads rendering styles from files (an XML format, documented), so MyPlay could ship a style file the owner imports, with no new app. **Organic Maps / CoMaps** build their styles into the app, so a custom style means building our own copy of that app (GPL allows it) and keeping it updated | OsmAnd style: 2 to 3 chunks, mostly design. Own Organic Maps / CoMaps build: large, and ongoing work at every one of their releases | OsmAnd: low, styles survive updates fairly well. Own build: high upkeep. None of these has Waze's live traffic and reports |
| **3. Changing Waze from inside, via the owner's LSPosed module** | In theory anything. In practice Waze draws its map in its own native code (C++, OpenGL), not in Java. Hooks can reach Java-side settings, such as forcing day or night, but recolouring the map would mean changing its native drawing or its packed style data | Very large, research first | Very high: obfuscated, breaks with Waze updates (often weekly), and modifying the Waze app is against its terms of use. **Not recommended** beyond W1's host check |

**Recommendation:** the colour filter (way 1) once W1 has Waze running, as part of the chosen
language's looks (F3/F7 decide the colours). Way 2 with OsmAnd if the owner wants a map that is
designed, not just tinted. Not way 3.

Open question for the owner (through the dispatcher): besides the home screen and bar, which
screens or details do you dislike most? That sets the order of N2 to N6.

## Standing rules for every task

1. Keep changes to AARE's existing files small, and put new MyPlay code in new files. The Kotlin
   package stays `net.prototux.aare`. This is what keeps merging upstream AARE cheap.
2. Network use only where a feature needs it (today: the updater, cover art, and the weather when
   it is turned on). A task that adds a network use says so in its note. The owner dropped the
   earlier "nothing phones home" rule for MyPlay on 2026-11-19.
3. Every look or behaviour value a task introduces becomes a setting with a sensible default
   (the owner: "we can have plenty of settings").
4. New text is plain English. AARE ships 20 languages; new MyPlay text is English only until the
   owner says otherwise.
5. One commit per piece. Build and tests pass before every commit.
6. **Where a feature already exists elsewhere, take it in this order:** an unmerged branch of
   upstream AARE first (it will merge cleanly when upstream finishes it), then a fork's version,
   then our own. When taking someone's commit, keep its author, credit them on the About page,
   and read it first: none of it has been run by us.
7. Third-party artwork and fonts are allowed (the owner dropped that rule for MyPlay on
   2026-11-19), with their licence text kept in the app as AARE already does.

## Phase A: a build the owner can try against the box

Everything after this phase depends on what the box does, so this phase is kept as small as
possible. Nothing about the look or the knob changes here.

### A1. Rename to MyPlay

- **Touches:**
  - `app/build.gradle.kts`: `applicationId` becomes `io.github.whiterain3.myplay`. `namespace`
    stays `net.prototux.aare` (rule 1).
  - `app/src/main/res/values*/strings.xml`: `app_name` and every user-visible "AARE" become
    "MyPlay" (40 lines in English; the other 19 languages the same way). Credits keep the name AARE.
  - `AndroidManifest.xml`: the provider authority `net.prototux.aare.car` becomes
    `${applicationId}.car`, with the matching constant in `host/CarControlProvider` and anything
    that calls it.
  - `headunit.sh`: the two places that name the package (`pm path`, `am start -n`). With a changed
    id and an unchanged namespace the activity is
    `io.github.whiterain3.myplay/net.prototux.aare.ui.MainActivity`.
  - `tools/magisk/` (package name in `privapp-permissions-aare.xml`, module id and name) and
    `xposed/` (`AARE_PACKAGE`, module scope). Neither is released yet; they are only kept correct.
  - `headunit/.../Main.kt`: window title becomes "MyPlay test head unit". The program keeps the
    name `aare-headunit`.
  - A new, plain, original launcher icon (no AARE artwork, no third-party artwork).
  - `ui/AboutContent.kt`: "MyPlay is a personal fork of AARE by prototux and contributors", links
    to both repositories, the licence, and the existing library credits untouched.
  - `README.md`: a short banner at the top saying what MyPlay is and pointing at
    `docs/HANDOFF.md`. The rest of AARE's README stays (credits, and fewer merge conflicts).
- **Left alone on purpose:** the phone certificate the app makes for itself (its subject says
  AARE). Other head units have accepted it as it is, so changing it before the box test would add
  an unknown. The abstract socket name `aare_hu` also stays.
- **Known consequence:** the app defines the permission `android.car.permission.TEMPLATE_RENDERER`,
  as AARE and F-Auto do. Android refuses to install two apps that define the same permission with
  different signatures, so MyPlay cannot be installed next to AARE or F-Auto. It can be installed
  next to Google's Android Auto (AARE's README says so; not tried by us).
- **Verified by:** Tests. The built APK reports the new package id and the label MyPlay
  (`aapt dump badging`). A text search for user-visible "AARE" finds only credits. Head unit: the
  Tester installs it and sees the home screen.

### A2. Network defaults (done)

Done in 0.1.0. It was planned under a "nothing phones home" rule that the owner has since dropped
for MyPlay; the results stand because the owner confirmed each one on 2026-11-19.

| What | In 0.1.0 | Owner's decision |
|---|---|---|
| Update check | Reads `WhiteRain3/MyPlay` on GitHub only. Off by default, with a "Check now" button | Confirmed. A private repository with updates from the owner's own server is a possible later task (I3) |
| Weather (Open-Meteo, sends an approximate position) | Off by default, works when turned on, the dashboard card unchanged | Stays as it is |
| Cover art (downloaded from the address a media app supplies) | On | Stays on |
| Certificate download | Unchanged from AARE: only when tapped; automatic refresh off by default | Not decided (handoff question 1) |
| Browser app on the car screen | Unchanged | Not a question: it is the user browsing |

### A3. Release chain for MyPlay

- **Touches (all new files, so upstream's stay mergeable):**
  - `myplay-version.txt`: one line. It starts as `0.0.0`, for which the workflow publishes
    nothing, so the first push cannot release before a review. The Release step sets `0.1.0`.
  - `.github/workflows/myplay-ci.yml`: on every push to `claude/myplay`, run
    `:aap:test :app:testDebugUnitTest :headunit:test :app:assembleDebug :headunit:installDist`
    (the same line as upstream's `ci.yml`, which only runs for `main` and pull requests).
  - `.github/workflows/myplay-release.yml`: on every push to `claude/myplay`, read
    `myplay-version.txt`; if no release `myplay-v<version>` exists yet, run the tests, build the
    release APK signed with the owner's key, build the desktop head unit, and publish a release.
    One build at a time (a `concurrency` group), and the "already published?" check is repeated
    just before publishing. This is the same pattern as `fork-release.yml` in the owner's
    AA-Experiments fork, including its lessons.
- **Details that must hold:**
  - Version name is the file's content; version code is major x 10000 + minor x 100 + patch, as
    upstream computes it.
  - Secrets: `MYPLAY_KEYSTORE_BASE64`, `MYPLAY_KEYSTORE_PASSWORD`, `MYPLAY_KEY_ALIAS`,
    `MYPLAY_KEY_PASSWORD`. The workflow hands them to Gradle under the names `app/build.gradle.kts`
    already reads (`AARE_KEYSTORE` and so on), so the build file is not edited.
  - **If the keystore secret is missing the release fails.** Upstream's workflow falls back to a
    debug-signed APK; on GitHub's machines that key is new on every run, so each such release
    could not be installed over the one before.
  - The workflow does not pass `AARE_PHONE_CERT` / `AARE_PHONE_KEY`. Releases then carry no
    built-in phone certificate and the app makes its own on first start (see the handoff,
    "Certificates").
  - Release files: `MyPlay-<version>.apk`, `myplay-headunit-<version>.zip`, `SHA256SUMS`. The
    Magisk module is left out until task H2.
  - Release notes say it is a fork of AARE and link the source at that commit (GPL).
- **Why not tags, as upstream does:** it is not known whether a cloud session may push tags to
  this repository, and the branch-push pattern is already proven with this owner. If tags turn
  out to work, the trigger can be changed later in one line.
- **Verified by:** a push without a version change publishes nothing. A push with a new version
  and the secrets in place publishes a release whose APK is signed by the owner's key
  (`apksigner verify --print-certs`, compared with the fingerprint in the handoff). A run without
  the secret fails with a clear message.

### A4. First release (done), Tester smoke run, and the owner's parked test (the gate)

- **Touches:** `myplay-version.txt` (0.1.0), nothing else. Released 2026-11-19.
- **Head unit:** the Tester follows `docs/TESTER_SETUP.md`: install, connect, home screen appears,
  the knob script moves something. This proves the build is not broken before the owner drives to
  the car.
- **Car:** the owner follows "The parked test" in `docs/HANDOFF.md` and sends back the connection
  log, photos of the car screen, and the answers listed there, including what the log says about
  the screen size and margins the box reports.
- **Known gap in 0.1.0:** the app's own connection log does not record which buttons the car
  sends; only the developer log read over USB does. Task A7 fixes that. Until then the "press
  each button" part of the test needs the PC, or waits for the next build.
- **What the result means, and the next step for each:**

| What the log or screen shows | Meaning | Next |
|---|---|---|
| MyPlay's screen on the car | The box accepts a non-Google phone app | Phase C onwards, in order |
| Nothing after pairing; no "TLS" line in the log | The start-up over Bluetooth and Wi-Fi did not get through | A5 |
| "TLS handshake did not complete" or "status -3" | The box (or what is behind it) refuses MyPlay's certificate | The owner decides about Google's certificate (handoff question 1). No other way through is known |
| Connected, but black, cut off or badly sized picture | The box's screen report needs correcting, or its video needs a different setting | A6 |
| It connects and then drops, or Google's app takes over | Both apps are answering the box | A8 |

### A5. Only if the start-up fails: compatibility with adapter boxes

The owner's box is the same kind of thing as the wireless adapters that gave AARE and its forks
trouble. What is known, from the fork report and F-Auto's `FORTUNER.md` (real-car notes for a
Kowan KA05 adapter):

- Cheap adapters may only finish the Bluetooth and Wi-Fi start-up when the phone answers in the
  exact order Google's app uses, and echoes the head unit's own version.
- An adapter can get stuck: it accepts Bluetooth but never starts. Forgetting the pairing and
  pairing again cleared it; switching it off and on did not.
- With Google's app disabled, Android may never notice the adapter offers wireless Android Auto,
  so the app never tries.
- F-Auto **also forced Google's certificate** on its adapter, so it is not known whether the
  message order alone is enough for the owner's box.

What to take, in this order:

1. **Upstream's branch `feat/wireless-adapter-handshake`** (`1daaf008`, with tests and a section
   in `docs/decisions.md`): a "Wireless compatibility" setting with Auto / Adapter / Built-in.
   Upstream deliberately does not guess from the Bluetooth name, because a wrong guess costs a
   connection the driver cannot explain. This replaces the earlier plan to port F-Auto's version.
2. JVKeller `8c72758d`: one retry loop with waits (7 tries over about 30 seconds) while Bluetooth
   stays up, and a clear message when a VPN blocks the car's Wi-Fi.
3. JVKeller `42de7bae`: stop asking the car's Bluetooth for its services on every start, which
   could collide with an adapter's start-up.
4. F-Auto's "Connect now" button (dial a paired device by name), only if the log shows the app
   never tried.
5. Later, if drops happen while driving: upstream's `feat/wireless-reconnect-after-drop`.

- **Touches:** `aap/.../wireless/`, `transport/wireless/WirelessConnector.kt`,
  `service/ProjectionService.kt`, the Connection page.
- **Verified by:** Tests (each of these comes with its own). Car: the owner tries again and sends
  the log.

### A6. Only if the picture is wrong

- **Wrong size, cut edges:** `docs/vehicles.md` describes a file the owner can import on the
  phone (*Advanced > Vehicle overrides*) that corrects density and margins for one head unit
  without a new build. Once the right values are known, the rule goes into
  `app/src/main/assets/vehicles.json`, matched on what the box reports in the log line
  `head unit:`.
- **Black screen although connected** (the most common first failure on unusual head units,
  according to the fork report):
  - filipeniquini `8a7107d1`: wait for the car's answer before sending video, send anyway if it
    never answers, assume 30 pictures a second when the car does not say.
  - F-Auto `cf582d7c`: a per-car "force H.264" setting, for boxes that connect but cannot show
    the newer video format.
- **Verified by:** Tests (both commits carry tests; `vehicles.json` parsing has tests). Car: a
  photo of the screen.

### A7. The car's buttons in the app's own log

Small, needs no car, and makes the parked test more useful. Do it while waiting.

- **What:** every key the car sends (not knob turns, which would flood it) is written to the
  connection log the owner can read on the phone. HyperCriSiS `d272542b` does exactly this in
  `car/CarSession.kt`. JVKeller `2afa9094` logs each key code once per session; either is fine.
- **Why:** nobody knows what the box sends for the MMI knob's press and tilt and for the buttons.
  Phase C and task C5 depend on it.
- **Verified by:** Tests. Head unit: keys pressed in the window appear in the phone's log.

### A8. Google's Android Auto taking the connection

Three forks hit this independently: while Google's app is enabled, Android may hand it the car
too, and both sessions break. The parked test already asks the owner to turn off wireless
Android Auto in Google's app.

- **What:** a warning on MyPlay's home screen when Google's app is enabled and set to answer the
  car, and (the phone is rooted) one tap to switch Google's app off and one to switch it back
  on. Upstream is building the same on branch `feat/shizuku-setup`; take that if it is ready.
- **Note for the owner:** Google's app is the daily driver today. Switching it off means Android
  Auto does not start until it is switched on again. Nothing is uninstalled.
- **Verified by:** Tests on the detection. Car.

## Phase B: a test bench that looks like the Audi

Does not need the car. Both tasks are done and in 0.1.0. The profile's margin (80 pixels on the
right) is from the owner's description and is corrected after the parked test.

### B1. Margins on the desktop head unit

The desktop head unit always reports margins of 0 (`HeadUnit.kt`, `setWidthMargin(0)`), so it
shows the full 1280x720. The owner's car shows 1200x723 of it according to Google's app, with
about 80 pixels unused on the right. What the box itself reports is not known until the parked
test's log.

- **Touches:** `headunit/.../HeadUnit.kt` and `Main.kt`: options `--width-margin N` and
  `--height-margin N`; a saved picture that marks the part the car would not show.
- **Verified by:** Tests (`:headunit:test`: the options reach the service description). Head
  unit: a run with `--width-margin 80` shows the app laid out inside 1200 pixels.

### B2. The Audi profile and a standard knob walk

- **Touches:** new `tools/tester/` with one PowerShell script (the owner's PC is Windows): start the head unit with the Audi profile and a named knob script, stop it
  after a set time, collect the pictures and the phone's log into a folder named after the
  version. `docs/TESTER_SETUP.md` is updated to use it.
- **Verified by:** the Tester runs it and pushes a report with pictures to `claude/test-reports`.

After the parked test, the profile is corrected to what the box really reported.

## Phase C: the knob works everywhere

What exists (read from the code): each knob click moves focus to the next or previous element
(`car/CarSession.kt` turns it into Tab / Shift+Tab), and a 3 dp ring in the accent colour shows
where focus is (`ui/car/CarFocus.kt`). The ring is only used in some files:

| File | Tappable things | With a ring |
|---|---|---|
| `ui/car/CarShell.kt` | 11 | 5 |
| `ui/car/PhoneScreen.kt` | 7 | 0 |
| `ui/car/DockShell.kt` | 5 | 0 |
| `ui/car/ClassicShell.kt` | 3 | 0 |
| `ui/car/CarSettingsScreen.kt` | 3 | 0 |
| `ui/car/NotificationsScreen.kt` | 4 | 0 |
| `ui/car/DashboardSettings.kt` | 4 | 0 |
| `ui/car/DashboardWidgets.kt` | 2 | 0 |
| `ui/car/CallCard.kt` | 3 | 0 |
| `ui/car/BrowserScreen.kt` | 1 | 0 |
| `host/AssistantBar.kt`, `host/CarAppScreen.kt` | 4 + 4 | 0 |
| `media/` (four files) | 27 | 13 |
| `host/templates/` (seven files) | 26 | 18 |

The counts are from a text search. Some elements share a helper that already draws the ring, so
the true gap is smaller; the worker checks each one.

**Someone has already solved most of this for a knob-only car.** JVKeller's fork drives a Mazda
commander knob (turn, tilt four ways, press, no touch). None of it is in upstream. Read it before
writing our own:

| Commit | What it does |
|---|---|
| `a08c2073` | **Panes:** turning moves inside one area (the bar, each dashboard card, the content) and wraps; tilting jumps to the next area. **Cursor:** on screens that only take touches, the knob drives a pointer. A ring on every button that lacked one. Switches on only after the car sends a knob turn |
| `934bb1f8` | A small hook that lets one screen take the car's keys first (Home and Back stay global). Everything knob-specific needs it |
| `570a67de`, `1903f5d9` | The knob moves and zooms the map; tilts move from key to key on the car keyboard |
| `deda4fb2` | When a call rings, focus is on Answer; once answered, on Hang up |

**Not known:** whether the box passes on the MMI knob's tilt at all, or only turn and press. The
pane idea needs some second input to jump between areas. If there is no tilt, a long press or one
of the MMI buttons has to do it. Task A7 and the parked test answer this, so they come first.

Recommended route: take `934bb1f8` and `a08c2073` as the base of C1 to C3 (keeping the author),
then adjust to what the Audi's knob really sends. If the worker finds they do not apply cleanly
on our branch, it says so and C1 to C3 are done as written below.

### C1. A ring on every tappable thing

- **Touches:** the files above, one commit per screen. A new unit test that reads the car-screen
  source files and fails when something tappable has no ring (with a short allow-list for real
  exceptions), so the gap cannot come back.
- **Verified by:** Tests. Head unit: a knob walk through each screen; every picture shows exactly
  one ring.

### C2. Starting point, order, and no dead ends

- **Touches:** the shells and screens: each screen puts focus somewhere sensible when it opens,
  the order follows the eye (bar, then content), Back always leaves, and the knob press opens the
  focused thing.
- **Verified by:** Head unit: from every screen a script gets home using only the knob, its press
  and Back.

### C3. A knob way for every touch-only gesture

Text search finds drag or pinch handling in `ui/car/CallCard.kt`, `media/NowPlayingView.kt`
(probably the seek bar) and `host/templates/MapUi.kt` (moving and zooming the map).

- **Touches:** those three files: focusable buttons or a knob mode for each gesture (for the map:
  zoom in and out, and a "move the map" mode where turning moves it).
- **Not known:** how a web page in the Browser app behaves with a knob.
- **Verified by:** Head unit, then Car.

### C4. The knob while the phone is locked

AARE's own notes (`docs/decisions.md`, "finding 6") say Android 14 and later may refuse to move
the car screen out of touch mode while the phone is locked; then the knob has nothing to focus.
The owner's phone runs Android 16.

- **Touches:** nothing until seen. If it happens: focus targets that also work in touch mode
  (`video/ProjectionDisplay.kt`, `ui/car/CarFocus.kt`).
- **Verified by:** Head unit with the phone locked (the phone's log says "Touch mode switch
  rejected" if it is refused). Car: the owner tries locked and unlocked in the parked test.

### C5. The Audi's own buttons

Which MMI buttons the box passes on (Back, Menu, Nav, Media, Tel, the knob's tilt, the
steering-wheel keys) is not known. Task A7 puts them in the log the owner can read.

- **Touches:** upstream's branch `feat/car-key-remap` (`6c810cfd`) already lets each car button
  be given another action; take it and add a phone page "Car buttons" in our design. Upstream's
  `fix/aa18-input` (6 commits: long presses while a key is held, and more) is worth taking with
  it.
- **Verified by:** Tests on the mapping. Car.

## Phase D: a compact layout for 1200x723

The sizes are in `CarDimens` (`ui/theme/CarTheme.kt`): tap targets 76 dp, bar 96 dp wide, and so
on, chosen for a touch screen. At 213 dpi the owner's visible area is about 900 x 543 dp, and
nobody touches it, so targets can be smaller.

### D1. Measure first

- **Touches:** nothing. The Tester takes pictures of every screen with the Audi profile.
- **Verified by:** a short list from the Design reviewer: what is cut off, what is too big, what
  is too small to read at arm's length. Not known how it looks until these pictures exist.

### D2. A compact size set

- **Touches:** `ui/theme/CarTheme.kt` (a second `CarDimens`), `ui/car/CarLayout.kt` (choose it
  when the screen is small in dp and has no touch), a setting to force either.
- **Verified by:** Tests on the choice. Head unit pictures before and after. Car.

### D3. Size settings

- **Touches:** upstream already has these on two unmerged branches: `feat/car-density-setting`
  (`489670a1`, a phone setting for how large everything is drawn) and
  `feat/car-text-control-size` (`a5587626`, text size and control size separately). Take those
  instead of writing our own; the picture from Phase E shows the result.
- **Verified by:** Tests. Head unit. Car.

### D4. Things other small screens already broke

From the fork report, to check on our pictures in D1 instead of discovering in the car:

- On an 800x480 unit the bar's third app was cut off (upstream #269, HyperCriSiS `c41c6529`).
- Short screens should scroll the bar instead of squeezing it, and drop cramped dashboard cards
  (F-Auto `561c146e`).
- Text drawn without an explicit colour came out black on dark looks (same commit). This matters
  for every look we add in Phase F.

## Phase E: the phone app in the chosen design

**Starts now (2026-11-20), built by a second worker ("Phone worker") in parallel with the car
Worker.** The owner chose this days ago and asked why it is not built yet.

The design: direction 1 "Edit the picture", dark, with direction 2's dark symbol tiles for
everything that cannot be seen on the car screen. Pictures: `merged-overview.png`,
`merged-1-customise.png`, `merged-2-battery.png`, `merged-3-behind-the-scenes.png` and the README
in `docs/design-options/` on branch `claude/design-options` of AA-Experiments. They were drawn for
the earlier project, so some tiles name things MyPlay does not have (Flags, Restart Android
Auto); the shapes, colours, controls and layout are what to follow, the contents come from
MyPlay's own settings.

What the phone app is today (`ui/MainActivity.kt`, 1,540 lines): a status card, "Finish setting
up", a "Preview car screen" button, then a list of nine pages (Car screen, Apps, Voice, Messages,
Sound, Connection, Car data, Advanced, About), each a long column of switches. It follows the
phone's light or dark setting. About 60 settings live in `Settings.kt`.

**Keeping the two workers apart.** The Phone worker writes only:
- new files under `app/.../ui/phone/` (all new screens and parts);
- `ui/MainActivity.kt`, and there only to point the start screen and the page routes at the new
  screens (the old page functions stay where they are and are called from the new tiles until
  each is redrawn);
- new strings in a new file `res/values/strings_phone.xml`.
It does not edit `Settings.kt`, anything under `ui/car/`, `host/`, `car/` or `media/`. A new
setting the phone needs goes into a new file of extension properties on `Settings`, like the car
side's `MyPlayLayoutSettings.kt`, one file per stream. The car Worker does not touch
`ui/MainActivity.kt` or `ui/phone/` while this phase runs. If either needs the other's file, it
asks through the dispatcher.

The picture of the car screen on the phone draws the car's own screen small, from the same code
the car uses (`CarShell` with sample content, as "Preview car screen" and the README pictures
already do). So the picture changes when the car side changes, without the Phone worker editing
car files.

**E1. Dark always, and the shared parts.**
- Touches: `ui/phone/PhoneParts.kt` (new): the symbol tile (icon, big value, small label, a dot
  for "needs attention"), the big symbol button, the simple row (icon, title, value, switch or
  arrow), the section heading with a line, colour dots, the "Saved" bar; `ui/MainActivity.kt`:
  the phone always dark (one line, `PhoneTheme(dark = true)`). Colours taken from the pictures.
- Verified by: unit tests where there is logic (a tile's value text); Tester: phone screenshots
  of a test page showing every part; Design reviewer.

**E2. The Customise start screen.**
- Touches: `ui/phone/CustomiseScreen.kt` (new), `ui/MainActivity.kt` (start screen points here).
  Top to bottom: the status line (connected or not) and "Finish setting up" while anything is
  missing, as today; the car-screen picture with "Tap a part of the screen to change it"; the
  "Behind the scenes" tiles (E4); "Preview car screen" stays as a button. Tapping the picture
  opens, for now, today's Car screen page.
- Verified by: Tester: phone screenshots. Every page that existed before is still reachable (a
  checklist in the commit message, one line per old page).

**E3. Tap a part of the picture.**
- Touches: `ui/phone/CarPicture.kt` (new): the small car screen with tappable regions (bar,
  clock, battery and signal, each dashboard card, the wallpaper, the app grid button), each
  outlined when touched, as in the picture.
- Verified by: unit test on the regions for the Audi's screen size; Tester: tapping each region
  opens its page.

**E4. "Behind the scenes" tiles.** One tile each, showing its own state:

| Tile | Value shown | Opens |
|---|---|---|
| Updates | installed version, a dot when a newer one is out | updates page |
| Connection | "Connected" / "Waiting" / "Off" | today's Connection page |
| Apps | how many apps on the car screen | today's Apps page |
| Sound | the car audio method | today's Sound page |
| Messages | on or off | today's Messages page |
| Voice | the assistant's name | today's Voice page |
| Certificates | which one is offered next | today's certificates card |
| Advanced | "Log on" when the detailed log is on | today's Advanced page (head unit server, overrides, log) |
| Car data | — | today's Car data page |
| About | the version | today's About page |

- Touches: `ui/phone/BehindTheScenes.kt` (new). The values are read from existing settings and
  state only, no new setting.
- Verified by: unit tests on the values; Tester screenshots; every old page reachable.

**E5. One page per part of the picture**, one task each, in this order: Status icons (the
Battery page in the pictures: icon, number and % as big symbol buttons; size as growing icons;
colour dots; colour by level), Clock, Bar, Dashboard cards, Wallpaper, Colours. Each is a new
file in `ui/phone/`. **What the car screen can already do** is wired straight away (bar side,
cards and their order, wallpaper, colours and looks exist today). **What it cannot do yet**
(battery number and colour by level, clock size) needs a car-side task first (Phase F, F4); the
page shows those controls only once the car side has them, so nothing on the phone does nothing.
- Verified by: tests on each setting the page writes; Tester: a phone screenshot and a bench
  picture of the car screen after each change.

**E6. Car screen layout: presets and options** (with Phase M).
- Follows the Design reviewer's sheet: the six presets as small pictures to pick from; then the
  eight options, each set the "Edit the picture" way (tap the bar or a card in the car picture
  and change that one thing); cards dragged into the dashed slots of the chosen card layout.
  Options a rule does not allow in the current combination are not offered (no dock on the
  side). The phone shows the result at once.
- Touches: `ui/phone/LayoutScreen.kt` (new): the presets as pictures to pick from, then the
  options of M1 as big symbol buttons, and the Design reviewer's "tap a card, drag it to a
  dashed slot" from the layout drawings. Reads and writes only through M1's
  `MyPlayLayoutSettings.kt` (the car Worker's file; the Phone worker uses it, does not edit it).
- Depends on M1 being merged. Verified by: Tester: pick each preset, then a bench picture.

**E7. Redraw the remaining pages** (Connection, Apps, Sound, Messages, Voice, Advanced, About) in
the simple-row style of "Behind the scenes". No behaviour change; every switch keeps its setting.
- Verified by: unit tests unchanged and passing; Tester screenshots; Design reviewer.

**E8. Backup.** The "Backup" tile and page once Phase F's F6 (settings backup) exists.

Order for the Phone worker: E1, E2, E4, E3, E5 (Status icons page first, as in the pictures),
E7, then E6 when M1 is in, E8 when F6 is in.

## Phase F: the look system

AARE already has themes (colours, type, shapes), layouts, an accent colour, a wallpaper (built-in
or the user's photo) and a contrast test that holds every theme to readable contrast
(`docs/decisions.md`, "Car screen themes and layouts").

A lesson from the forks: every fork that added a look did it by copying a whole screen file, and
each now carries several near-identical copies that can no longer take upstream's fixes. For
MyPlay a look is **data** (colours, shapes, spacing, which status items) applied to the existing
one or two layouts. F3 depends on that.

The looks AARE ships, including "Android Auto", "Android Auto (2019)", "CarPlay" and "Retro" with
its clock font, all stay (owner's decision, 2026-11-19).

### F1. MyPlay's own dark looks

- **Touches:** new theme files next to the existing ones in `ui/theme/`, original palettes
  (the owner's AA-Experiments looks can be brought over: same licence, our own work). MyPlay's
  default look is one of them.
- **Verified by:** Tests (`CarThemeContrastTest` covers new themes). Head unit pictures per look.

### F2. Always dark

Today the car's light sensor switches most themes between day and night colours.

- **Touches:** a setting "Always dark" (on by default) and a separate one for whether map apps
  still follow the car's lights.
- **Verified by:** Tests. Head unit (`day` / `night` in the knob script).

### F3. Edit a look: colours

- **Touches:** a stored, user-made theme (themes are Kotlin values today; AARE's notes say
  reading them from data was kept possible), the colour picker and theme editor brought over
  from AA-Experiments (`ThemeUi.kt`, `ColoursUi.kt`), each colour tied to the part of the picture
  it paints.
- **Verified by:** Tests (saving, loading, contrast warning). Head unit pictures.

### F4. Status icons and clock in detail

Battery icon, number and % sign on or off, size, colour, colour by level; clock size and weight;
signal on or off. All drawn by us in `ui/car/PhoneStatus.kt` and `CarShell.kt`, so each is an
ordinary setting.

- **Verified by:** Tests. Head unit pictures.

### F5. Wallpaper editor

- **Touches:** crop, darken and blur from AA-Experiments (`CarWallpaperEditor.kt`,
  `core/ImageBlur.kt`), writing the file AARE already reads (`wallpaper.jpg`).
- **Verified by:** Tests on the image maths. Head unit pictures.

### F6. Look files, backup and undo

- **Touches:** start from upstream's branch `feat/settings-backup` (all settings in one file,
  leaving out per-phone values and private keys; originally HyperCriSiS `6a1e6717`, with tests).
  Add on top: a file holding only the look, and undo of the last change (`core/LookBundle.kt`
  from AA-Experiments, without the parts about Google's app).
- **Verified by:** Tests (round trip). Tester.

## Phase G: mirrored apps with the knob

AARE can run an ordinary phone app on a car-sized virtual screen ("Phone apps", needs root or
Shizuku; the owner's phone is rooted). Today only touches and Back reach that app
(`ui/car/StandaloneAppScreen.kt`), so on a screen without touch it cannot be used. It is shown
only while the car reports it is parked; how passenger mode affects that is to be read from the
code in G1.

What the forks add (none of it run by us):

| From | What | Use for us |
|---|---|---|
| JVKeller `a08c2073` (`ui/car/CarCursor.kt`) | A knob-driven pointer on touch-only screens: tilt moves it (faster while held), press taps or drags, turn scrolls, tilting against an edge leaves | **The base of G2.** The only work anywhere that makes mirrored apps usable with a knob |
| HyperCriSiS `7fdeeccb`, `ec13dad3`, `38f4c575`, `67819eda`; upstream branch `feat/media-app-video` (`5fd0bc4e`) | "Video mode": a button in the music controls runs the media app itself (YouTube, NewPipe) on a car-sized screen, with play, pause, skip and seek underneath. Parked or passenger only | **The base of G3.** Touch-based, so it needs G2 |
| HyperCriSiS `73043fe2`, `55556370` | The phone's screen mirrored through the root helper, with no "start recording?" question each time | G4 |
| filipeniquini `33000400` | A size per mirrored app; YouTube opens phone-sized by default | G3, if YouTube lays out badly at car size |
| JVKeller `346b51b2` and three more | AARE's own video player worked entirely by the knob (local files and Jellyfin; not YouTube) | A model for knob-first video controls |

### G1. Knob and buttons reach the mirrored app

- **Touches:** `ui/car/StandaloneAppScreen.kt`, `car/CarSession.kt`, `launch/`: while a mirrored
  app is on screen, knob turns, the knob press and the direction keys are sent into it the same
  way Back already is (JVKeller `934bb1f8` is the hook for this). A per-app choice of what a turn
  means (next/previous item, up/down, left/right).
- **Verified by:** Tests on the key mapping. Head unit with a simple mirrored app. Car.

### G2. A pointer for apps that ignore keys

- **Touches:** take JVKeller's cursor (`a08c2073`, `ui/car/CarCursor.kt`) for mirrored apps, the
  phone screen and the browser. If the Audi's knob has no tilt (not known), the pointer needs
  another way to change direction: a long press that switches between left-right and up-down.
- **Verified by:** Tests (JVKeller's first version `2afa9094` has unit tests). Head unit. Car.

### G3. YouTube

- **Touches:** video mode from upstream's `feat/media-app-video`, or HyperCriSiS's version if
  upstream's is not ready, made usable with G1 and G2.
- **Not known:** how well the YouTube phone app can be driven with keys alone, and whether it
  plays normally on a virtual screen. Found out on the head unit, then in the car.
- **Verified by:** Car: the owner plays a video parked using only the knob.

### G4. The phone's own screen on the car screen

- **Touches:** AARE's "Phone screen" app (`mirror/`) needs the phone unlocked, asks for screen
  recording, and uses an accessibility service for touches. On a rooted phone HyperCriSiS
  `73043fe2` and `55556370` do it through the root helper without the question. With G2's pointer
  on top it becomes usable with the knob.
- **Verified by:** Head unit. Car.

## Phase H: sound and calls with this box

The owner's sound goes over the car's own Bluetooth. Not known until the parked test: whether
music and navigation voice simply keep playing over that link while MyPlay is connected, and
whether the box also expects sound over its own connection.

Two things from the fork report to keep in mind: many adapters claim a hands-free microphone that
is not really there (F-Auto switches calls to speakerphone automatically), and some head units
only accept one sound format (upstream branch `fix/aac-only-media-sink`). Neither is known to
apply to this box.

### H1. Find out

- **Touches:** nothing. Questions in the parked test.

### H2. Only if needed: full sound capture with root

- **Touches:** `tools/magisk/` renamed for MyPlay and added to the release (it installs the app
  as a system app so it can capture all sound, as AARE documents). A wrong system module can stop
  a phone from starting, so this is tested on the Tester's phone first and the owner is told how
  to remove it.
- **Verified by:** Tester's phone. Car.

## Phase I: keeping it going

### I1. Taking in upstream AARE

Upstream moves fast: besides `main` it has about 50 unmerged branches (286 commits) as of
2026-11-18, several of which this plan wants. The forks that stayed healthy (HyperCriSiS,
JVKeller) kept one feature per change, added tests, and did not rewrite `CarShell.kt`,
`MainActivity.kt` or `Settings.kt` in bulk. The forks that did rewrite them can no longer take
upstream's fixes.

- **Routine:** the owner presses "Sync fork" on `main` (or asks a session to); a worker merges
  `main` into `claude/myplay`, resolves conflicts, runs all tests; the Reviewer reads the merge;
  a release follows the usual way. Whenever upstream merges one of the branches named in this
  plan, and otherwise about monthly.
- **Taking an unmerged upstream branch early** (rule 6): merge that branch as it is, in one
  commit that names it, so the later merge of `main` recognises it.

### I2. An updater that installs

- **Touches:** today a found update opens the release page in the browser. Bring over the
  download-and-install updater from AA-Experiments (`Updater.kt`, `core/UpdateCheck.kt`), still
  only when the owner taps.
- **Verified by:** Tests. The owner updates from one release to the next.

### I3. Possible later, not decided: private repository, updates from the owner's own server

The owner is considering making the repository private and serving updates from their own
Raspberry Pi web server, as their other app does. To be looked at again after the parked test.
What it would touch: where the updater looks (`UpdateChecker.kt`), a step in the release
workflow that puts the APK and a small version file on that server, and the tester guide. One
thing to settle then: MyPlay is GPL, so anyone who is given the app must also be able to get its
source; with a private repository and a single user that is the owner themselves.

### I4. A log the owner can send without a PC

- **Touches:** l0rdg3x `89c8b070`, `4a263e07` (`devlog/DevLog.kt`): the detailed log written to
  files on the phone, with private details filtered, surviving a crash. Useful because nobody
  but the owner is ever at the car.
- **Verified by:** Tests. Tester.

### I5. Release builds must not depend on downloading the Paparazzi plugin

The first 0.1.1 release run failed because Maven did not deliver the Paparazzi plugin; running the
same commit again worked. Paparazzi only draws the README's pictures (`tools/mockups.sh`, used
with `-Pmockups`), but it is declared in the root `build.gradle.kts` (`apply false`), so every
build resolves it.

- **Touches:** the root `build.gradle.kts` and `app/build.gradle.kts`: declare the plugin only
  when `-Pmockups` is given, or in a way that an ordinary build never resolves it. Small edit to
  two upstream files; check that `tools/mockups.sh` still works.
- **Verified by:** a build with an empty Gradle cache and that plugin's address blocked still
  succeeds; CI stays green.

## Not planned

- Using Google's Desktop Head Unit for MyPlay: it refuses any phone app without Google's
  certificate (`docs/certificates.md`).
- Anything that changes Google's Android Auto app. MyPlay does not use it.
- Split screen (two apps side by side; upstream branch `feat/car-split-screen`): two panes on a
  7-inch screen would be too small. Can be revisited.

## Do not copy (from the fork report)

- **Code that extracts or forces Google's certificate** (F-Auto and athanasso
  `cert/GearheadKeyExtractor.kt`, background updaters for it, one-tap prompts). Never commit a
  certificate or key. The download button AARE already has is the owner's open question; nothing
  more is added. Google's current certificate also ends on 2026-12-31 by upstream's notes.
- **meraz9000's hook collection for Google Maps** (`tools/aare-hooks/`, abandoned in October).
  Fragile. Upstream's own module in `xposed/` is a different thing: it is in this repository
  and is what task W1 repairs for Waze.
- **Guessing from the Bluetooth name**, and fixed density presets by head unit name (athanasso
  sets 160 dpi for a list of names; the owner's screen is 213).
- **Mirrored apps running while the car moves, on by default** (athanasso). MyPlay keeps AARE's
  model: parked only, plus a passenger mode the user switches on.
- **paperwolf165's root tricks that change the whole phone** (screen size and density, display
  off, and typing a saved PIN to unlock). The PIN part is never taken. Its plan for fitting the
  phone's display to the car, with its tests, can be read for ideas.
- **Bulk rewrites** of `CarShell.kt`, `MainActivity.kt` and `Settings.kt` (see I1).

## Not known, and only the car can tell

- Whether the box accepts MyPlay, and with which certificate.
- Which start-up order the box needs.
- What screen size, density, margins and video format the box reports and accepts.
- What the box sends for the MMI knob (turn only, or tilt too) and for each button.
- Whether the knob moves focus while the phone is locked (Android 16; no fork mentions 16).
- Where sound goes while MyPlay is connected.
- Whether any of the forks' work has ever run on an Audi MMI 3G or on this box: the fork report
  found no mention.
