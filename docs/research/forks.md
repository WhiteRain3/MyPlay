# Other forks of AARE: what to take, what to avoid

Research for MyPlay, 2026-11-18. Read-only: nothing from any fork was built or run, and no fork
code was copied into this repository.

**Target setup this was judged against:** Audi A4 (2012-2015, MMI 3G, no factory Android Auto), a
generic aftermarket wireless Android Auto decoder box as the head unit, a 7-inch non-touch screen
(1200x723, 213 dpi) driven only by a rotary knob, a rooted Android 16 phone. Goals: knob-only
control everywhere, YouTube and other mirrored apps, own look, many settings.

## How this was done

- The fork list comes from GitHub's forks page for `prototux/AARE` (3 pages, 55 forks including
  MyPlay). The GitHub API was not reachable from this session, so star counts and "last updated"
  dates were not cross-checked through it.
- `malashhab/F-Auto` is not in that list (it is a separate repository, not a GitHub fork) but
  shares history with AARE, so it was included.
- Every branch of upstream and of all 55 repositories was fetched into one bare git repository
  and compared. "Own commits" below means non-merge commits reachable from any branch of the
  fork that are not on **any** upstream branch. A fork that keeps the same work on two branches
  is counted twice (JVKeller especially), and a fork whose work upstream later re-wrote under a
  new hash still counts as "own".
- Upstream `main` is `37f8236` (2026-11-14), the same commit MyPlay is on.

**One finding that is not about forks:** upstream itself has about 50 branches that are not
merged into `main` (286 commits). Several are exactly what MyPlay wants. They are listed in
[section 2.7](#27-upstreams-own-unmerged-branches), and they are the first place to look, before
any fork.

## 1. The forks

37 of the 55 have no commits of their own (plain copies). These 18 do:

| Fork | Own commits | Last own commit | What it does |
|---|---|---|---|
| [HyperCriSiS/AARE](https://github.com/HyperCriSiS/AARE) | 189 | 2026-11-17 | The most active fork (Mercedes MBUX). Video mode for media apps (YouTube, NewPipe), split screen, a new apps page, settings backup, car audio fixes. Tracks upstream and sends pull requests back. |
| [JVKeller/AARE](https://github.com/JVKeller/AARE) | 52 (about half are duplicates across branches) | 2026-11-17 | **Mazda commander knob, non-touch.** Knob focus panes, a knob-driven cursor for touch-only screens, a built-in video player worked by the knob, wireless retry fixes, car audio without root. |
| [filipeniquini/AARE](https://github.com/filipeniquini/AARE) | 51 | 2026-11-15 | VW Play (Visteon) fixes, "GTS" style, interface size setting, animations, per-app size for mirrored apps, AAC audio, Bluetooth fixes, OBD2 card. |
| [meraz9000/AARE](https://github.com/meraz9000/AARE) | 47 | 2026-10-28 | Experiments to run Google Maps' private car screen through on-device hooks; split screen; 1920x720 displays. Stopped in October. |
| [paperwolf165/screencast](https://github.com/paperwolf165/screencast) | 22 | 2026-11-16 | "Wingmirror": strips AARE down to a full-screen cast of the phone's own screen, with root. |
| [athanasso/AARE](https://github.com/athanasso/AARE) | 20 | 2026-11-13 | "AARE Cockpit": rebrand with new shells and palettes, OBD2, key remapper, a DPI preset for Chinese head units. Shares a lot of code with F-Auto. One day of commits. |
| [malashhab/F-Auto](https://github.com/malashhab/F-Auto) | 18 | 2026-11-18 | **Aftermarket wireless adapter fork** (Toyota Fortuner + Kowan KA05 adapter). Adapter handshake, several car layouts, offline maps. Active. |
| [l0rdg3x/AARE](https://github.com/l0rdg3x/AARE) | 16 | 2026-11-16 | Developer log to rotating files with a privacy filter; test options for the desktop head unit; small dashboard options. |
| [freezed23/AARE](https://github.com/freezed23/AARE) | 12 | 2026-11-03 | Opt-in diagnostics (navigation events, video ACK counters) saved to Downloads. |
| [s-ratashnyuk/AARE](https://github.com/s-ratashnyuk/AARE) | 10 | 2026-11-17 | Option to hide the car bar (with a movable reveal button), multi-touch to standalone apps, an Android 17 launch fix. |
| [jameswynn/AARE](https://github.com/jameswynn/AARE) | 5 | 2026-11-08 | Ford Mustang Mach-E fixes (upstream #74). Upstream has since taken this work in its own form. |
| [benoitjadinon/AARE](https://github.com/benoitjadinon/AARE) | 5 (4 are jameswynn's) | 2026-11-07 | Touch mapping as a per-car setting. |
| [guizmo84170/AARE-sync3](https://github.com/guizmo84170/AARE-sync3) | 5 | 2026-11-09 | Diagnostics build for Ford SYNC 3 over USB. |
| [jhahvhi/AARE](https://github.com/jhahvhi/AARE) | 4 | 2026-11-14 | Optional margin under the dashboard map card; a clip fix for Android 10. |
| [dev-mindz/AARE](https://github.com/dev-mindz/AARE) | 2 | 2026-11-17 | CI only (also builds the Xposed APK). |
| [nightlys/AARE](https://github.com/nightlys/AARE) | 1 | 2026-11-14 | A touch calibration tool. |
| [rutgerputter/AARE](https://github.com/rutgerputter/AARE) | 1 | 2026-11-06 | Adds the background-location permission to the manifest. |
| [candiesdoodle/AARE](https://github.com/candiesdoodle/AARE) | 1 | 2026-11-09 | CI only. |

No own commits: AnDevX/Selfhost_AARE, dev copies by WPeterPal, tinhtran88, MattMaaaan (his fixes
are already merged upstream), geazzysena, abebatista, yhhanec, jesuslg123, J3DOK3DO, bobdavis84,
Snoopiam, akanshu-sapra, davidmoate88, xA907x, tjipenk, KrishnaX12, pafolol, paulhiggs,
alfazzy/AARE-gibbo, jooray, thuxeko, teamturnersolutions, sgmaat, pnoch, pierluigipisanti,
Phyzx72, easternkite, Dvalin21, Armone0, dustfeather, wakamex, nethunterXI, PhaniCodes,
vaimalaviya1233, tompcum/AAREBeta, Ojabrane, Innvohub-abdulrahman.

## 2. Worth taking

Most useful first. "Upstream has it" means upstream `main` (what MyPlay is on) unless a branch is
named.

### 2.1 Knob-only control: JVKeller (the closest match to this car)

JVKeller drives a Mazda commander knob, which is the same problem as the MMI knob: turn, tilt in
four directions, press, no touch. Upstream `main` only has the basics: `ui/car/CarFocus.kt` is 40
lines that draw a border on whatever Compose focuses. Upstream's README credits JVKeller for
that. Everything below is **not in upstream**, on any branch.

| Commit | Branch | What | Main files |
|---|---|---|---|
| `a08c2073` | `feat/knob-panes-cursor` | The big one. **Panes**: turning moves inside a pane (rail, each dashboard card, content) and wraps; tilting jumps to the next pane, like Android Auto's focus areas. **Cursor**: on screens that only take touches (phone screen mirror, hosted phone apps, browser) the knob drives a pointer: tilt moves it (faster while held), press taps or drags, turn scrolls, tilting against an edge leaves to the next pane. Keyboard as its own pane. Focus highlight on every button that lacked one. Switches on only after the car sends a knob turn. | `ui/car/CarFocus.kt` (+224), `ui/car/CarCursor.kt` (new, 297), `host/templates/MapUi.kt`, `mirror/MirrorScreen.kt`, `ui/car/StandaloneAppScreen.kt`, `ui/car/BrowserScreen.kt` |
| `570a67de` | `audio-tts-2` | Knob drives the map in pan mode (tilt pans, turn zooms, press leaves); floating panes (call strip) reachable; focus goes to the first item of a new template instead of back to the rail. Folded into `a08c2073`. | `host/templates/MapUi.kt`, `host/CarAppScreen.kt` |
| `1903f5d9` | `audio-tts-2` | Tilts on the car keyboard move key to key. Folded into `a08c2073`. | `host/templates/KeyboardUi.kt` |
| `2afa9094` | `video` | The first version of the pointer, for the browser only, with unit tests. Says its design follows Fermata Auto's D-pad cursor, "written anew". Also logs each key code a head unit sends, once per session, to check the mapping in a real car. | `ui/car/BrowserCursor.kt`, `BrowserCursorTest.kt` |
| `934bb1f8` | `feat/car-videos` | `CarRuntime.screenInput`: a screen can take the car's keys and knob first (Home and Back stay global). Small, and the hook everything else knob-specific needs. | `car/CarSession.kt`, `host/CarRuntime.kt` |
| `deda4fb2` | `fix/wheel-button-answers-calls` | Focus lands on Answer when a call rings and on Hang up once answered, so one knob press does it. | `ui/car/CallCard.kt`, `car/CarSession.kt` |

Why it matters: `a08c2073` is the only work anywhere that makes **mirrored and touch-only
content usable with a knob**, which is exactly what YouTube on a non-touch screen needs. Start
here. Read it before designing MyPlay's own focus system, even if it is rewritten.

Related, from others:

- F-Auto `561c146e`: "Rotary knob focus is visible on the rail" in its own layouts, and rail tabs
  that scroll on short screens instead of being squeezed (`ui/car/RailTabs.kt`, with a test).
- HyperCriSiS `d272542b`: car keys written to the connection log, except knob and touchpad moves
  (`car/CarSession.kt`). Useful on day one to learn what the decoder box actually sends for the
  MMI knob and buttons.
- Upstream branch `feat/car-key-remap` (`6c810cfd`): remap steering-wheel buttons. athanasso has
  a simpler `car/KeyRemapper.kt` (`b95b4c1d`). Prefer upstream's.
- Upstream branch `fix/aa18-input` (6 commits): key metastate, long presses while a key is held,
  focus moved by a touchpad, input feedback.

### 2.2 Aftermarket wireless box: F-Auto and upstream's adapter branch

The decoder box is an aftermarket wireless adapter in front of a screen, the same family of
problem F-Auto solved for a Kowan KA05 adapter in a Toyota. Findings from F-Auto's `FORTUNER.md`
(describes real-car tests, worth reading in full):

- Cheap adapters may only finish the Bluetooth/Wi-Fi bootstrap when the phone answers in **the
  exact order Google's Android Auto uses**: version response 6.1 with status only, the start
  request acknowledged before the credentials are asked for, no pings from the phone. Also echo
  the head unit's own version in the AAP version exchange. F-Auto `bb53d64f`:
  `aap/.../wireless/WirelessBootstrap.kt` (`officialPhoneOrder`), `aap/.../AapSession.kt`
  (`echoHeadUnitVersion`), `service/ProjectionService.kt`, `transport/wireless/WirelessConnector.kt`.
  athanasso `6c6c7a25` is the same change.
- The adapter can get **stuck**: it accepts the Bluetooth connection but never starts the
  handshake, and only forgetting the pairing and re-pairing clears it (a power cycle did not).
- With Google's Android Auto disabled, Android may never cache the adapter's wireless service, so
  AARE never notices it. F-Auto adds a "Connect now" button that dials a paired device by name
  (`WirelessConnector.findBondedByName`).
- An adapter that hangs up mid-handshake ("Broken pipe") gets the other handshake order at once
  (`561c146e`, with `WirelessBootstrapTest`).
- Many adapters advertise Bluetooth hands-free with no real microphone behind it; F-Auto switches
  calls to speakerphone automatically (`autoSpeakerOnCall` in `Settings.kt`).
- Pause wireless probing while a USB accessory is plugged in, so the two don't tear each other
  down (`cf582d7c`).

**Upstream status:** not in `main`, but upstream has its own cleaner version on the unmerged
branch `feat/wireless-adapter-handshake` (`1daaf008`, with `HandshakeChoiceTest` and a section in
`docs/decisions.md`). It is a "Wireless compatibility" setting: Auto / Adapter / Built-in.
Upstream deliberately **rejected** F-Auto's guessing from the Bluetooth name and its automatic
fallback, because a wrong guess costs a connection the driver cannot explain. Take upstream's
branch, and use F-Auto's notes as the field report.

Other wireless fixes worth having:

| Commit | Where | What | In upstream? |
|---|---|---|---|
| `8c72758d` | JVKeller `feat/car-voice-engine-phone-mute` | One retry loop with backoff (7 tries over about 30 s) while the Bluetooth link stays up; waits for the phone's Wi-Fi instead of failing, "which left an AAWireless dongle stuck mid-handshake"; a clear message when a VPN blocks the car's Wi-Fi. `aap/.../wireless/WirelessRetryPolicy.kt` + test. | No |
| `42de7bae` | JVKeller `fix/wireless-sdp-throttle` (also HyperCriSiS `f3bb0a54`) | Stop querying the car's own hands-free Bluetooth on every start, "which could collide with a wireless dongle's bootstrap". `aap/.../wireless/SdpThrottle.kt` + test. | No |
| `e362385b`, `e8ff5e7e`, `cb672cd8`, `f896e569` | filipeniquini | Turn phone Bluetooth on when the car connects and keep it on; announce the phone again once the session runs; send the Bluetooth address in upper case. | Upstream #292 covers the first (HyperCriSiS `c7c34f95` cherry-picks it) |
| upstream branches | `feat/wireless-reconnect-after-drop`, `feat/companion-wireless`, `fix/aa18-wireless`, `fix/aa18-wpp45`, `fix/aa18-wireless-off-byebye` | Reconnect by itself after a mid-drive drop; one-tap wireless; redial an adapter after its quick first try; many protocol corrections. | Unmerged upstream |

### 2.3 YouTube and mirrored apps

Upstream `main` already has: the phone-screen mirror (screen capture plus an accessibility
service for touch), a browser, ordinary phone apps on the car screen with root or Shizuku, and a
passenger mode. What forks add:

| Commit | Fork | What | Notes |
|---|---|---|---|
| `7fdeeccb`, `ec13dad3`, `38f4c575`, `67819eda` | HyperCriSiS | **Video mode**: a button in the now-playing controls runs the media app itself (YouTube, NewPipe) on its own virtual display sized to the pane, through the root/Shizuku helper, with play/pause/skip and a seek slider underneath. Parked or passenger mode. `launch/LaunchHostService.kt`, `mirror/PhoneVideo.kt`. | Upstream has its own take on branch `feat/media-app-video` (`5fd0bc4e`), not in `main`. Touch-based: needs JVKeller's cursor to be usable with a knob. |
| `73043fe2`, `55556370` | HyperCriSiS | Mirror the phone screen through the privileged helper "the way scrcpy does": no screen-capture dialog. Second commit makes it work on Android 17. | Not in upstream main. Good fit for a rooted phone. |
| `346b51b2`, `7910f8fa`, `d4eac55f`, `701b1aeb` | JVKeller `feat/car-videos` | AARE's **own video player** (media3 ExoPlayer) on the car screen: local videos and Jellyfin, sound sent straight to the car's media channel, sound-sync adjustment, **every control worked by the knob**, scrubbing that speeds up the faster you turn, fit/zoom/stretch. | Not in upstream. Does not play YouTube, but it is the model for a knob-first player UI. |
| `b5a18776`, `0b9e594f`, `ebb15a99`, `b762e6e1` | paperwolf165 | **Root screen cast**: the phone's display full screen on the car, real touch injection, display resized and re-densitied to fit the head unit (`cast/CastDisplayPlan.kt`, tested), secure display so protected windows are not black, phone panel off, everything restored on disconnect or after a crash. | Not in upstream. The simplest route to "any app on the car screen" for a rooted phone. See the warnings in 3.4. |
| `33000400` | filipeniquini | A size per mirrored app (phone, tablet, or 1 to 8); YouTube opens phone-sized by default; AARE's own keyboard on the car instead of the phone's. `apps/PhoneAppScreen.kt`, `apps/ShellDisplayService.kt`. | Not in upstream. |
| `f26dfeb6` | filipeniquini | Mirror touch sent live instead of on release; mirror quality 100/75/50%; encoder never fed more frames than the stream's rate (cheaper on 120 Hz phones). | Check against upstream before taking: partly overlapping. |
| `fa61aa63` | s-ratashnyuk | Multi-touch forwarded to standalone apps. | Not relevant without touch. |
| `e5cbc983`, `4ad85b7d` | HyperCriSiS | Split screen: two apps side by side with a draggable divider (`ui/car/SplitPane.kt`). | Upstream branch `feat/car-split-screen`. On a 7-inch 1200x723 screen two panes will be small. |

### 2.4 Small and odd screens

1200x723 at 213 dpi is small. Upstream `main` already lets a per-car rule in `vehicles.json` set
`dpi`, `widthMargin` and `heightMargin` (`docs/vehicles.md`).

- Upstream branches `feat/car-density-setting` (`489670a1`, a phone setting for the car screen's
  density) and `feat/car-text-control-size` (`a5587626`, text size and control size). Take these
  first.
- filipeniquini `0438d449`: an interface size setting (100/90/80/70%) in phone and car settings.
- HyperCriSiS `c41c6529` (cherry-pick of upstream #269): on an 800x480 unit the side rail's third
  app was cut off; fixed by dropping items below a width. Same class of bug to expect here.
- F-Auto `561c146e`: short screens scroll the rail instead of squeezing it; cramped dashboard
  cards are dropped on narrow screens. Also: text drawn without an explicit colour came out black
  on dark themes, a trap for anyone writing new themes.
- F-Auto `cf582d7c`: a per-vehicle `forceH264` override for head units that connect but show a
  black screen on H.265 (`car/VehicleOverrides.kt`, tested). Cheap decoder boxes are candidates.
- filipeniquini `8a7107d1`: wait for the car's video-focus answer before sending video, send
  anyway if it never answers, default a missing frame rate to 30 fps
  (`aap/.../VideoFocusFallbackTest.kt`). F-Auto `561c146e` has the same "never answers focus"
  fallback. Black screen on connect is the most common first failure on unusual head units.
- meraz9000 `7877d14a`: support for 1920x720 displays in video selection, with a test
  (`VideoSelectionTest.kt`), and a matching option in the desktop head unit. Shows where
  resolution choice lives if 1200x723 is not picked correctly.
- jhahvhi `0068c06f`: a margin under the dashboard map card. nightlys `f1b744aa`: touch
  calibration. Neither matters without touch.

### 2.5 Look, themes, layouts

Upstream `main` already has styles (theme plus layout), accent colours, wallpaper or own photo,
and dashboard cards. Forks add whole layouts rather than a theming system:

- F-Auto: Classic, Dusk, CarPlay-style, Strada, Material and Cockpit shells, each a separate
  file of 300-650 lines (`ui/car/DuskUIShell.kt`, `HybridShell.kt`, `CarPlayShell.kt`,
  `StradaShell.kt`, `MaterialShell.kt`, `CockpitShell.kt`), day and night palettes, a hideable
  side bar (`a8687566`), a status top bar with weather, phone, clock, notifications
  (`f55e8785`). Good as a gallery of layout ideas and of status-bar contents.
- filipeniquini: "GTS" style (`0438d449`, `8ee83573`), animations with a full/reduced/off setting
  measured at about +3 CPU points (`ff08249b`, `ui/car/CarMotion.kt`), corner cards (clock and
  others, `60f5371e`, `1e5e7075`), phone dark mode following the car's day/night (`4f3c68ec`).
- athanasso: five named palettes (`eecbbd40`, `ui/theme/CarTheme.kt`), night mode from the
  phone's light sensor (`b95b4c1d`; upstream has its own on `feat/night-from-light-sensor`).
- s-ratashnyuk `c9534ae2`: hide the car bar. Its reveal button is dragged by touch, so it would
  need a knob way back.
- Upstream branches: `feat/rail-status-widgets`, `feat/rail-apps-and-shortcuts`,
  `feat/hide-builtin-apps`.

Lesson from reading them: every fork that added a look did it by copying a whole shell file, and
they now each carry several near-identical shells. If MyPlay wants user themes, a theme should
be data (colours, shapes, spacing, which status items) applied to one or two shells.

### 2.6 Settings and tooling ideas

- **Settings backup** to one JSON file, leaving out per-phone values and private keys:
  HyperCriSiS `6a1e6717` (`SettingsBackup.kt` + test); upstream took it on branch
  `feat/settings-backup` and credits him. Essential once there are "many settings".
- **Developer log** to rotating files with a privacy filter, resumes after a crash: l0rdg3x
  `89c8b070`, `4a263e07` (`devlog/DevLog.kt`). Very useful for debugging in a car with no laptop.
- **Desktop head unit test options**: l0rdg3x `3500fe73`, `ab1b30a2`, `0fa08780` add fake
  identities, fault injection and protocol events for scripts. The desktop head unit (already in
  upstream, `headunit/`) accepts wheel and arrow keys as a rotary controller, so knob work can be
  developed at a desk; F-Auto `561c146e` makes it save a screenshot after each scripted input.
- **"Google's Android Auto steals the connection"**: three forks hit this independently
  (paperwolf165 `beefebd0`, filipeniquini `f26dfeb6`, F-Auto `cf582d7c`). With the official app
  enabled, Android hands it the car too and both sessions break. All three add a warning and a
  one-tap disable (`pm disable-user`, reversible). Upstream is doing the same on
  `feat/shizuku-setup`. Plan for this from the start.
- AAC audio for head units that only accept AAC: filipeniquini `5b710c29` (`audio/AacEncoder.kt`,
  tests). Upstream has `fix/aac-only-media-sink`. Only matters if the box turns out AAC-only.
- Car audio without root (AARE as a text-to-speech engine, phone mute): JVKeller `bddfd2a1`. The
  target phone is rooted, so low priority.

### 2.7 Upstream's own unmerged branches

Fetched 2026-11-18; none are in `main`. Relevant ones: `feat/wireless-adapter-handshake`,
`feat/wireless-reconnect-after-drop`, `feat/companion-wireless`, `fix/aa18-wireless` (21
commits), `fix/aa18-wpp45` (34), `fix/aa18-input`, `feat/car-key-remap`,
`feat/car-density-setting`, `feat/car-text-control-size`, `feat/media-app-video`,
`feat/car-split-screen`, `feat/hide-builtin-apps`, `feat/rail-apps-and-shortcuts`,
`feat/rail-status-widgets`, `feat/settings-backup`, `feat/night-from-light-sensor`,
`fix/aa18-video` (31), `fix/aa18-car-ui-chrome` (28). The `fix/aa18-*` family looks like a
large pass aligning the protocol with Android Auto 18. Because these come from the upstream
author they will merge cleanly later; a fork's version of the same feature will not.

## 3. What not to do

### 3.1 Google's certificate and key

Background (from upstream's own `docs/certificates.md`): the head unit checks the certificate the
phone presents; strict head units only accept the one embedded in Google's Android Auto app,
which is identical on every install. Upstream `main` **already** handles this: an optional
download of that certificate from a third-party GitHub repository
(`cert/CertificateDownload.kt`), and `extract_key.py` to pull it from an APK on a computer.

What forks add, described only:

- F-Auto and athanasso (`aa5222cd`) add `cert/GearheadKeyExtractor.kt`: it reads Google's Android
  Auto app installed on the same phone, finds the embedded certificate and private key and
  decrypts them on the device, plus `cert/CertificateAutoUpdate.kt`, a background updater. F-Auto
  also forces the Google certificate for its adapter.
- paperwolf165 `06cb93ef` puts a one-tap "download Google's certificate" prompt on the home
  screen when a car refuses the phone's own. HyperCriSiS `2ed42c46` keeps offering the Google
  certificate after a cut handshake.

Advice: do not copy any of this into MyPlay, and never commit a certificate or key to the
repository (the forks read correctly here: none of them ships the key in git, as far as was
checked by file names). It is Google's private key; distributing it, or code whose only purpose
is extracting it, is the part of this project most likely to draw a takedown. The certificate's
validity also ends **2026-12-31** according to upstream's document, so anything built around the
current one has weeks left. Whether the decoder box checks the certificate at all is not known;
upstream reports a similar cheap unit (ZLink) accepting AARE's own self-signed certificate.
Test with the self-signed one first.

### 3.2 Hooks that defeat Google's app checks

- meraz9000 (`2abb2320` to `9482f867`, about 30 commits, October only) built Xposed/LSPosed hooks
  (`tools/aare-hooks/`, ships a prebuilt `api-82.jar`) to make Google Maps and Waze accept AARE
  as if it were Google's host, then fought touch input for days (several "WIP", "experiment" and
  "investigation" commits). The work stops on 2026-10-28 and looks abandoned. Upstream has its
  own maintained Xposed module (`xposed/`) for the same purpose.
- F-Auto's `debug/maps-projection-proof` branch carries a "smoke test for patched Maps"
  (`fc2b99b2`).

Advice: this path is fragile (breaks with every Maps update), needs root plus LSPosed, and
circumvents a signature check. For YouTube it is not needed at all: mirroring an ordinary app on
a virtual display (2.3) does not touch any of it.

### 3.3 Guessing by name, and hard-coded presets

- F-Auto and athanasso pick the wireless handshake from the adapter's Bluetooth name and fall
  back automatically. Upstream reviewed and rejected this (see 2.2).
- athanasso `eecbbd40` returns `dpi = 160`, zero margins for any head unit whose name contains
  "zlink", "tlink", "carlinkit", "autokit", "topway", "ts10" and so on
  (`car/VehicleOverrides.kt`). A 160 dpi guess would be wrong for a 213 dpi screen. Make density
  a visible setting instead.
- athanasso sets "keep standalone apps running while the vehicle moves" to **on by default**
  (`allowMovingStandalone`, `eecbbd40`). Video for the driver while moving is illegal in most
  places and dangerous. Upstream's model (parked only, plus an explicit passenger mode) is the
  one to keep.

### 3.4 Risky root tricks (paperwolf165)

The root screen cast is attractive but changes global phone state: `wm size` and `wm density`,
display power off through SurfaceFlinger, mock location, and optionally **typing a saved PIN to
unlock the phone** when the car connects (`cast/CodeVault.kt`, `ebb15a99`). If the app dies
between "change" and "restore" the phone is left with a car-shaped display; the fork has a
restore-after-reboot path, which shows the author hit this. Take the display-plan idea and its
tests; skip the PIN auto-unlock entirely. This fork also deleted most of AARE's features and
permissions (`e2e2da42`), so it cannot be merged, only read.

### 3.5 Rebrand-and-diverge forks

- athanasso: 20 large commits in one day, new application id, mixed-purpose commits ("search
  bar, hide built-in apps, dpi selector, obd telemetry pill, media keys, night sync" in one),
  no tests added, nothing since 2026-11-13. Comments inside still say "Fortuner fork" and refer
  to a `D:\aa-phone-side` folder, so much of it was lifted from F-Auto. Treat as a snapshot.
- F-Auto: its first commit (`bb53d64f`) is one squash of 67 files (+4272/-1876) that also
  checked in a `build-log.txt`; its newest work lives on a branch named
  `debug/maps-projection-proof` while `main` is older; it rewrote `MainActivity.kt` by 800 lines;
  it keeps upstream's application id `net.prototux.aare` (cannot be installed beside upstream)
  and its update checker still points at upstream's releases. Valuable for its adapter notes,
  painful to merge from.
- General lesson: forks that rewrote `CarShell.kt`, `MainActivity.kt` and `Settings.kt` in bulk
  can no longer take upstream fixes. HyperCriSiS and JVKeller stayed mergeable by keeping one
  feature per branch, adding tests, and sending work upstream. That is the pattern to copy, and
  it matters here because upstream is moving fast (50 open branches).

### 3.6 Licence

No fork changed the `LICENSE` file; all remain GPL-3.0-or-later, and F-Auto, athanasso and
paperwolf165 credit upstream in their READMEs. F-Auto says it ported features from "StradaOS
(GPL-3.0)"; that claim was not checked. filipeniquini adds the Barlow font with its OFL licence
text, which is the right way to do it. For MyPlay: keep the licence, keep credit to prototux and
to any fork author whose work is taken, keep commit authorship when cherry-picking, and publish
the source of any APK that is shared.

### 3.7 Phoning home

Nothing hidden was found. New network addresses in fork code, by searching the added lines:

- HyperCriSiS, athanasso, paperwolf165: update checks against their own GitHub releases instead
  of upstream's. paperwolf165 turns the update check and the weather **off by default**, a good
  default to copy.
- F-Auto: map downloads from `download.mapsforge.org` (offline maps, user-started).
- JVKeller: only the Jellyfin server the user types in.
- Upstream itself already contacts GitHub (update check, certificate download) and
  `api.open-meteo.com` (weather, sends the location).

No analytics, crash reporters or advertising SDKs were seen. This was a search of added source
lines for URLs, not a review of every dependency the forks added (media3, mapsforge and an
on-device background-removal library in F-Auto were not audited).

## 4. Not known / could not check

- **Nothing was built, installed or run.** Every claim that something "works" is the fork
  author's, from commit messages and notes.
- The fork list came from GitHub's web page. Private forks, deleted forks, and copies that are
  not registered as forks (like F-Auto) cannot be found this way; there may be more of the
  latter.
- Whether any fork has been tested on an Audi MMI 3G, on this decoder box (GP0A2020H-Q5H /
  EWMCP-N726), or on any 1200x723 screen: no mention was found in the commits read. The box's
  behaviour (which handshake order, whether it checks the certificate, which codec, what key
  codes it sends for the MMI knob, whether it reports itself as touch or non-touch) is unknown
  until tested.
- Whether JVKeller's knob work, written for a Mazda knob with tilt, maps onto what the box sends
  for the MMI knob (which may arrive as plain D-pad keys, or as turn-and-press only).
- Not every diff was read line by line. HyperCriSiS (189), filipeniquini (51) and meraz9000 (47)
  were read by commit message and file list, with the diffs opened only for the commits listed
  above. freezed23, guizmo84170, dev-mindz, rutgerputter and candiesdoodle were read by commit
  message only.
- Pull requests, issues and discussions on upstream or the forks were not read (no API access),
  so review comments, known bugs, and why upstream's branches are still unmerged are unknown.
- Android 16 specifically: several forks fix Android 17 (HyperCriSiS `55556370`, s-ratashnyuk
  `b72ca0bc`) and one Android 10; none mention 16.
- Releases and APKs of the forks were not downloaded or inspected.
