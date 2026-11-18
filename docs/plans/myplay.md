# MyPlay: ordered task list

Planner, 2026-11-18. Written from reading this repository at `37f8236` (upstream AARE, unchanged),
the earlier study `docs/plans/f-auto.md` in `WhiteRain3/AA-Experiments`, the owner's chosen design
(direction 1, "Edit the picture", dark) and F-Auto's source at `7f87c1c`. No app code was written.
File names and counts below come from reading and text searches; a worker checks each before
relying on it. Where something is not known, it says "not known".

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
2. No new network use. See `docs/HANDOFF.md`, "The owner's rules".
3. Every look or behaviour value a task introduces becomes a setting with a sensible default
   (the owner: "we can have plenty of settings").
4. New text is plain English. AARE ships 20 languages; new MyPlay text is English only until the
   owner says otherwise (open question 9 in the handoff).
5. One commit per piece. Build and tests pass before every commit.

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

### A2. Network use brought in line with the owner's rules

What the app does today, read from the code:

| What | Today | Change |
|---|---|---|
| Update check (`UpdateChecker.kt`) | On by default, asks GitHub about `prototux/AARE` at most once a day, off in debug builds | Point at `WhiteRain3/MyPlay`. Off by default. Add a "Check for updates" button that asks once when tapped. Understand the `myplay-v` tag prefix |
| Weather (`car/DashboardInfo.kt`, Open-Meteo) | On by default, sends an approximate position (about 10 km) | Off by default. Whether it stays as an opt-in or goes is the owner's choice (handoff question 2) |
| Certificate download (`cert/CertificateDownload.kt`) | Only when tapped. Automatic refresh exists and is off by default | No change in this task. Owner's decision (handoff question 1) |
| Browser app on the car screen | Opens DuckDuckGo when the user opens it | No change: it is the user browsing |

- **Touches:** `UpdateChecker.kt`, `Settings.kt` (two defaults), the Connection page in
  `ui/MainActivity.kt` (the button), `UpdateVersionTest.kt`.
- **Verified by:** Tests (tag parsing and version comparison). The Reviewer searches the code for
  every network address and confirms the table above is complete.

### A3. Release chain for MyPlay

- **Touches (all new files, so upstream's stay mergeable):**
  - `myplay-version.txt`: one line, for example `0.1.0`.
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

### A4. First release, Tester smoke run, and the owner's parked test (the gate)

- **Touches:** `myplay-version.txt` (0.1.0), nothing else.
- **Head unit:** the Tester follows `docs/TESTER_SETUP.md`: install, connect, home screen appears,
  the knob script moves something. This proves the build is not broken before the owner drives to
  the car.
- **Car:** the owner follows "The parked test" in `docs/HANDOFF.md` and sends back the connection
  log, photos of the car screen, and the answers listed there.
- **What the result means, and the next step for each:**

| What the log or screen shows | Meaning | Next |
|---|---|---|
| MyPlay's screen on the car | The box accepts a non-Google phone app | Phase C onwards, in order |
| Nothing after pairing; no "TLS" line in the log | The start-up over Bluetooth and Wi-Fi did not get through | A5 |
| "TLS handshake did not complete" or "status -3" | The box (or what is behind it) refuses MyPlay's certificate | The owner decides about Google's certificate (handoff question 1). No other way through is known |
| Connected, but black, cut off or badly sized picture | The box's screen report needs correcting | A6 |

### A5. Only if the start-up fails: adapter compatibility from F-Auto

F-Auto (GPL, same licence) made a similar clone-style adapter work. Read from its `FORTUNER.md`
and code, it needed three things together:

1. a "Connect now" button that dials a paired device by name, skipping Android's cached service
   check (`WirelessConnector.findBondedByName`);
2. the exact message order of Google's phone app during the Bluetooth and Wi-Fi start-up
   (`WirelessBootstrap.Options.officialPhoneOrder`), with the TCP connection retried 20 times;
3. answering the head unit's version request with the same version
   (`SessionOptions.echoHeadUnitVersion`).

On that adapter F-Auto **also forced Google's certificate**. So it is not known whether 1 to 3
are enough on their own for the owner's box.

- **Touches:** `aap/.../wireless/WirelessBootstrap.kt`, `transport/wireless/WirelessConnector.kt`,
  `service/ProjectionService.kt`, one switch on the Connection page ("Compatibility mode for
  adapter boxes", off by default, with the box's Bluetooth name). Credit F-Auto in the About page
  and in the commit.
- **Verified by:** Tests (F-Auto's bootstrap tests come along; the existing ones still pass with
  the switch off). Car: the owner tries again and sends the log.

### A6. Only if the picture is wrong: a screen rule for the box

- **Touches:** nothing at first. `docs/vehicles.md` describes a file the owner can import on the
  phone (*Advanced > Vehicle overrides*) that corrects density and margins for one head unit
  without a new build. Once the right values are known, the rule goes into
  `app/src/main/assets/vehicles.json`, matched on what the box reports in the log line
  `head unit:`.
- **Verified by:** Tests (`vehicles.json` parsing has tests). Car: a photo of the screen.

## Phase B: a test bench that looks like the Audi

Does not need the car. Can run alongside A4.

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
steering-wheel keys) is not known. The app writes every key it receives to its log
(`input key: code=...`).

- **Touches:** after the owner presses each button once with the detailed log on:
  `car/CarSession.kt` (map any unhandled key) and a phone page "Car buttons" where each button's
  action can be chosen.
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

### D3. "Size of everything" setting

- **Touches:** a setting on the phone that changes the density MyPlay draws at (the same value
  `vehicles.json` calls `dpi`), from smaller to larger, with the picture from Phase E showing the
  result.
- **Verified by:** Tests. Head unit. Car.

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

- **Touches:** one file holding the whole look, save and load, undo of the last change
  (`core/LookBundle.kt` from AA-Experiments, without the parts about Google's app).
- **Verified by:** Tests (round trip). Tester.

### F7. Only after the owner answers question 3: brand-named styles and the bundled font

AARE ships styles named "Android Auto", "Android Auto (2019)" and "CarPlay", and its Retro style
uses a third-party font for the clock (DSEG7 Classic, open licence). Under the owner's rules
these may have to go. Nothing is removed until the owner says.

## Phase G: mirrored apps with the knob

AARE can run an ordinary phone app on a car-sized virtual screen ("Phone apps", needs root or
Shizuku; the owner's phone is rooted). Today only touches and Back reach that app
(`ui/car/StandaloneAppScreen.kt`), so on a screen without touch it cannot be used. It is shown
only while the car reports it is parked; how passenger mode affects that is to be read from the
code in G1.

### G1. Knob and buttons reach the mirrored app

- **Touches:** `ui/car/StandaloneAppScreen.kt`, `car/CarSession.kt`, `launch/`: while a mirrored
  app is on screen, knob turns, the knob press and the direction keys are sent into it the same
  way Back already is. A per-app choice of what a turn means (next/previous item, up/down,
  left/right).
- **Verified by:** Tests on the key mapping. Head unit with a simple mirrored app. Car.

### G2. A pointer for apps that ignore keys

- **Touches:** a pointer drawn over the mirrored app: turning moves it along one direction, a
  long press switches direction, a press taps. For apps where G1 does nothing.
- **Verified by:** Head unit. Car.

### G3. YouTube

- **Not known:** how well the YouTube phone app can be driven with keys alone, and whether it
  plays normally on a virtual screen. Found out with G1 and G2 on the head unit, then in the car.
- **Touches:** whatever G1 and G2 show is missing; possibly a small preset for that app.
- **Verified by:** Car: the owner plays a video parked using only the knob.

### G4. The phone's own screen on the car screen

The same question for AARE's "Phone screen" app (`mirror/`), which needs the phone unlocked and
uses an accessibility service for touches. Whether keys can be sent that way is not known; with
root there are other ways. Planned after G1 to G3.

## Phase H: sound and calls with this box

The owner's sound goes over the car's own Bluetooth. Not known until the parked test: whether
music and navigation voice simply keep playing over that link while MyPlay is connected, and
whether the box also expects sound over its own connection.

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

- **Routine:** the owner presses "Sync fork" on `main` (or asks a session to); a worker merges
  `main` into `claude/myplay`, resolves conflicts, runs all tests; the Reviewer reads the merge;
  a release follows the usual way. Suggested whenever upstream fixes something we need, and
  otherwise about monthly.

### I2. An updater that installs

- **Touches:** today a found update opens the release page in the browser. Bring over the
  download-and-install updater from AA-Experiments (`Updater.kt`, `core/UpdateCheck.kt`), still
  only when the owner taps.
- **Verified by:** Tests. The owner updates from one release to the next.

## Not planned

- Using Google's Desktop Head Unit for MyPlay: it refuses any phone app without Google's
  certificate (`docs/certificates.md`).
- Anything that changes Google's Android Auto app. MyPlay does not use it.
