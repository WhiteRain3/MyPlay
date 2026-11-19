# MyPlay: ordered task list

Planner. First written 2026-11-18 from unchanged AARE at `37f8236`; revised 2026-11-19 after the
first release, the owner's decisions and the fork report (`docs/research/forks.md` on branch
`claude/research-forks`). File names and counts come from reading and text searches; a worker
checks each before relying on it. Commit ids of other people's work are as the fork report gives
them; nothing from them has been built or run by us. Where something is not known, it says "not
known".

## Where things stand (2026-11-19, evening)

| Tasks | State |
|---|---|
| A1 rename, A2 network defaults, A3 release chain | Done, released in 0.1.0 |
| B1 head unit margins, B2 Tester script | Done, in 0.1.0. The script ran on the owner's Windows PC without errors |
| A4 first release and Tester smoke run | 0.1.0 released and run on the PC bench: report `test-reports/0.1.0/REPORT.md` on branch `claude/test-reports` |
| A7 the car's buttons in the phone's own log | Done, released in 0.1.1: buttons and knob in the connection log on the phone, at most 120 such lines per connection; the log keeps 1000 lines. Not yet seen on the bench |
| Bench round 1 fixes (K1 to K11 below) | Not started. K1 to K8 can be done before the parked test |
| The owner's parked test | Not done yet. Everything about the box still waits for it |

Next: K1 to K6 in order, then a release; the parked test whenever the owner can.

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

**K2. When an app or a new page opens, the ring starts on its first item.**
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

**K3. Back never traps.**
- Finding: Back did nothing in Fermata Mirror.
- From the code: in a hosted app Back is handed to the app and nothing else happens
  (`host/CarRuntime.kt:671-682`, `host/CarAppConnection.kt:183`). An app that is on its first
  page ignores it, so there is no way out except the bar.
- Do: if Back is pressed again within two seconds and the app has not changed its page, go
  home. Holding Back goes home at once. (Our own rule; no fork has this.)
- Bench check: open Fermata Mirror, Back twice: home screen. In Organic Maps, open Search, Back
  once: back to the map, still inside the app.

**K4. A ring you can see, on everything.**
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

**K5. No stop without a visible ring on the home screen; maps buttons that fit.**
- Findings: one stop on the home screen shows nothing; "Organic Maps" is cut to "Organ / ic /
  M…".
- From the code: the card draws up to four maps buttons in a row that cannot scroll
  (`ui/car/CarShell.kt:494`, `navApps.take(4)`); on this screen the row is wider than the card,
  so the rest are clipped but still take focus. That matches the report (Waze is installed), but
  nobody has seen the hidden button, so it stays "probably".
- Do: show only as many buttons as fit, names on one line with "…", and a "More" button that
  opens the list when some are left out.
- Bench check: `home` walk: the number of stops equals the number of things you can see.

**K6. The ringed thing is always on screen.**
- Findings: the grid's last entry ("Exit to car") is ringed below the bottom edge; the Settings
  page continues below the edge.
- Do: when focus moves, scroll so the focused item is fully visible, in the app grid and in
  every list.
- Bench check: walk the grid to its end and the Settings page to its end: the ring is always
  fully visible.

**K7. The bench script's small faults.**
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

How each task is checked:

- **Tests**: unit tests on a computer (`:aap:test`, `:headunit:test`, `:app:testDebugUnitTest`).
- **Head unit**: AARE's own desktop head unit, run by the Tester on the owner's PC as an Audi-like
  screen (1280x720 video, 213 dpi, no touch, knob only). See `docs/TESTER_SETUP.md`.
- **Car**: the owner, parked, with the box. Only the car can answer questions about the box.

Video on the car screen is for when the car is parked, or for a passenger. That is said here once
and applies to every task that mentions video or mirrored apps.

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

The owner chose direction 1, "Edit the picture", dark, with dark symbol tiles for what cannot be
seen on the car screen (`docs/design-options/` on branch `claude/design-options` of
AA-Experiments; the "merged" pictures). They do not like light themes.

Two facts make this easier here than it was there: MyPlay draws the car screen itself, so the
picture on the phone can be the real car screen drawn small with sample content (AARE already
does this for "Preview car screen" and for its README pictures), and a change can show at once
without restarting anything. The second is believed from the code and is checked in E2.

### E1. Dark only, and the shared parts

- **Touches:** the phone theme (always dark, whatever the phone's setting), new file(s) for the
  shared parts from the design: symbol tile, big symbol button, simple row, colour dots, section
  heading, the "saved" bar.
- **Verified by:** Tests where there is logic. Tester: phone screenshots. Design reviewer.

### E2. The Customise screen

- **Touches:** new screen that replaces the category list in `ui/MainActivity.kt` as the home
  screen: the car-screen picture on top with tappable parts (bar, clock, status icons, dashboard
  cards, wallpaper, app grid), and below it tiles for what cannot be seen: Connection, Apps,
  Sound, Messages, Voice, Updates, Backup, Advanced, About. Each tile shows its own state.
  "Finish setting up" stays above everything until setup is done. The old pages stay reachable
  from the tiles until each is rebuilt.
- **Verified by:** Tester: phone screenshots, and one changed setting visible on the head unit
  without reconnecting.

### E3. One page per part of the picture

One small task each, in this order: Bar (side, what is on it), Clock, Status icons (battery,
signal), Dashboard cards (which, order), Wallpaper, Colours, App grid. Each page has the part
drawn large on top and big controls under it, as on the Battery picture.

- **Touches:** one new file per page, and the setting it edits in `Settings.kt`.
- **Verified by:** Tests on each setting. Tester screenshots. Design reviewer. Car for anything
  about readability.

### E4. The "behind the scenes" pages

Connection, Apps, Sound, Messages, Voice, Advanced: the existing pages restyled with the simple
rows. No behaviour change.

- **Verified by:** Tests unchanged and passing. Tester screenshots.

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
- **Hooks that make Google Maps or Waze accept the app** (meraz9000's `tools/aare-hooks/`,
  abandoned in October). Fragile, and YouTube does not need them.
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
