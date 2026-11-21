# Tester setup: a car screen on the Windows PC

For the Tester session on the owner's PC. It shows MyPlay's car screen on the computer using
AARE's own desktop head unit (the program is called `aare-headunit`), drives it like a rotary
knob, and saves pictures.

Google's Desktop Head Unit, which the Tester used for the earlier project, does **not** work
here: it refuses any phone app that does not present Google's certificate
(`docs/certificates.md`).

**What was and was not checked when this was written (2026-11-18):** the head unit was built and
its tests pass on Linux in a cloud session; the options and file names below were read from its
source (`headunit/src/main/kotlin/.../Main.kt`). Nobody has run these steps on Windows or with a
phone yet. If a step does not behave as written, fix this file in the same report.

## What you need

| Thing | Why |
|---|---|
| Java 17 or newer | the head unit is a Java program |
| ffmpeg, on the PATH | it decodes the car screen's video; the head unit stops with "ffmpeg not found" without it |
| adb (Android platform-tools) | the connection to the phone |
| A phone with USB debugging on, plugged in | MyPlay runs on it. Android 8 or newer |
| A MyPlay release | the app (`MyPlay-<version>.apk`) and the head unit (`myplay-headunit-<version>.zip`) from https://github.com/WhiteRain3/MyPlay/releases |

The first release, 0.1.0, is out (2026-11-19); the head unit in its zip already has the
`--margins` option.

## Steps (PowerShell)

### 1. Install the tools, once

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Gyan.FFmpeg
winget install Google.PlatformTools
```

Close PowerShell, open it again, and check all three answer:

```powershell
java -version
ffmpeg -version
adb version
```

If `winget` does not know a package, download it by hand (Temurin 21 from adoptium.net, an ffmpeg
Windows build from ffmpeg.org's download page, platform-tools from developer.android.com) and add
its folder to the PATH.

### 2. Get MyPlay, once per version

Download the two files of the newest release into `C:\myplay\` and unpack the zip there:

```powershell
cd C:\myplay
Expand-Archive .\myplay-headunit-<version>.zip -DestinationPath . -Force
adb devices
adb install -r .\MyPlay-<version>.apk
```

`adb devices` must list the phone as `device`. The head unit is now at
`C:\myplay\aare-headunit\bin\aare-headunit.bat`.

`adb install -r` keeps the app's settings. It fails if another build of MyPlay signed with a
different key is installed, or if AARE or F-Auto is installed (they claim the same permission):
uninstall that one first.

### 3. On the phone, once

1. Open MyPlay and go through "Finish setting up".
2. *Advanced > Head unit server*: turn it on. Leave "Accept from the network" off.

### 4. Connect with a window first

```powershell
adb forward tcp:5277 localabstract:aare_hu
C:\myplay\aare-headunit\bin\aare-headunit.bat --res 1280x720 --dpi 213 --no-touch --route none
```

The first time, the phone refuses the unknown head unit and shows a notification "New head unit".
Tap **Trust** (or "Trust this head unit" on MyPlay's home screen). The head unit keeps retrying
and connects by itself. It remembers its identity in `%APPDATA%\aare\headunit`, so this is needed
once per PC.

The window now shows the car screen. With `--no-touch` the mouse does nothing, as in the Audi.

| Key on the PC | In the car |
|---|---|
| Arrow keys or mouse wheel | turning the knob |
| Enter | pressing the knob |
| Esc or Backspace | Back |
| H | Home |
| V | the voice button |
| K, `,` and `.` | play/pause, previous, next |
| N | switch day / night |
| P | switch parked / driving |

Keep the window in front while testing by hand: keys only reach it while it has focus.

### 5. The normal way: the bench script

`tools/tester/Run-AudiBench.ps1` (in the repository, from plan task B2) does a whole run in one
command: the adb forward, the head unit with the Audi profile and a named knob walk, stopping it
after a set time, a phone screenshot and the phone's log.

The script's first version ran on the owner's Windows PC without errors (2026-11-19, report
`test-reports/0.1.0/REPORT.md`). The version after plan task K7 (new walks, `-Tile`, UTF-8
output, the new results folder) was written in a cloud session and **has not been run yet**;
report what happens, with the error text. Steps 6 and 7 below do the same by hand and are the
fallback.

```powershell
cd <your checkout of WhiteRain3/MyPlay, branch claude/myplay>\tools\tester
powershell -NoProfile -ExecutionPolicy Bypass -File .\Run-AudiBench.ps1 -HeadUnitDir C:\myplay\aare-headunit -Walk home
```

`-NoProfile` keeps your own PowerShell start-up file out of the run; its output got mixed into
the results before.

Add `-ListWalks` to see the walks, `-Version <version>` to name the result folder.

The path of the head unit folder must not contain a space (`C:\myplay\aare-headunit` is fine,
`C:\My Files\aare-headunit` is not).

| Option | Meaning |
|---|---|
| `-HeadUnitDir <folder>` | the unpacked `aare-headunit` folder from the release zip. Needed |
| `-Walk <name>` | which knob walk to play; see the table below. Default `home` |
| `-Tile <n>` | for the walks `settings`, `maps`, `back-out`, `music` and `map-move`: the app's place in the app grid, counted from 1 in the order the ring visits the tiles. Run `grid-end` first; its pictures give the numbers. Default 1 |
| `-MapClicks <n>` | for the walk `map-move`: how many knob clicks forward from where the maps app starts it takes to reach the map, the app's last stop. Count them on the `maps` walk's pictures. Default 12 |
| `-Version <name>` | name of the result folder, normally the MyPlay version |
| `-Seconds <n>` | how long to let the head unit run. Default: the walk's last event plus 18 seconds |
| `-OutRoot <folder>` | where results go. Default: a folder `myplay-bench` next to the head unit folder (for `C:\myplay\aare-headunit` that is `C:\myplay\myplay-bench`), so not inside the checkout |
| `-NoMargins` | leave out the margin part of the profile |
| `-ListWalks` | list the walks and stop |

| Walk | What it plays | Checks plan task |
|---|---|---|
| `home` | 12 single turns on the home screen | K4, K5 |
| `open-back` | turn, press, then Back | |
| `drive-park` | the car reports moving, then parked | |
| `night-day` | the car's lights on, then off | |
| `grid-end` | opens the app grid and turns 30 times, past its end | K6; also gives the tile numbers for `-Tile` |
| `settings` | opens the grid, goes to tile `-Tile`, presses, then 20 turns inside | K1, K2, K6 (give MyPlay's car Settings tile) |
| `maps` | the same for a maps app's tile | K1, K2 (give Organic Maps' tile) |
| `back-out` | opens tile `-Tile`, then Back twice in quick succession | K3 |
| `music` | opens tile `-Tile` (give a music app with folders), 15 clicks forward through its top level, a press to open what is ringed, 10 clicks through that level, 10 back, then Back. The ring should reach every row and button, stay fully on screen, and Back should go up one level | R4 (the music browser) |
| `map-move` | opens tile `-Tile` (Organic Maps or Waze), turns `-MapClicks` clicks to the map (the app's last stop), pushes, pad right twice and down once, two turns forward and one back, a push to stop, then Back. Expect: the line "Moving the map…" between the two pushes; the map shows more of the right, then more of what is below; it zooms in twice, then out once; after the second push the line is gone, and Back leaves the app | R5, R5-8 |
| `gestures` | start a song first. Knob held (next song), wheel double click (previous), wheel click twice (pause, then play), knob held while turning 2 clicks (two songs on), Menu held (assistant; Back closes it), Back held (home). Expect: the line at the top shows for each song and assistant gesture ("Next song: …", "Paused: …", "Assistant"), and each does what it says. Needs the head unit from the release after 0.1.29 | R6, R7, K3 (hold Back) |
| `pad` | on the home screen: one knob click, then pad right, right, down, down, left, up, up, left. Expect: the ring moves that way each time, from the bar to the cards and back, and never gets stuck. Needs the head unit from the release after 0.1.29 | R4, R7 |

Holding a key (walk `gestures`, "holding Back goes home" of K3) needs the head unit from the
release after 0.1.29; older ones do not know the hold events. All text files the script writes
are UTF-8.

`adb`, `java` and `ffmpeg` must be on the PATH (step 1). Step 3 and the Trust tap in step 4 must
have been done once, by hand; the script does not do them.

Results are in `<OutRoot>\<version>\<walk>\`:

| File | What |
|---|---|
| `screen-000.png`, `screen-001.png`, … and `latest.png` | the car screen: at most one picture every 3 seconds, and only when a new video frame arrives. A screen on which nothing moves gives no new picture, so fewer pictures than expected is not by itself a fault |
| `phone.png` | the phone's own screen at the end |
| `phone-log.txt` | the phone's log lines about the app |
| `headunit-out.txt`, `headunit-err.txt` | what the head unit printed |
| `summary.txt` | what was run |

The profile inside the script is
`--res 1280x720 --dpi 213 --no-touch --route none --margins 0,0,80,0`: 80 pixels hidden on the
right. That margin comes from the owner's description of the car, not from the box, and is not
confirmed; it is corrected after the parked test. The `--margins` option exists from plan task
B1 on, so the head unit must come from a release that includes it; with an older one, use
`-NoMargins`.

### 6. By hand: run without a window and save pictures

```powershell
adb forward tcp:5277 localabstract:aare_hu
$shots = "C:\myplay\shots\<version>\walk1"
$p = Start-Process -PassThru -NoNewWindow C:\myplay\aare-headunit\bin\aare-headunit.bat -ArgumentList `
  '--res','1280x720','--dpi','213','--no-touch','--route','none', `
  '--headless',$shots, `
  '--script','"park@2 rotate:1@6 rotate:1@9 rotate:1@12 dpad:center@15 key:4@21 rotate:-1@24"'
Start-Sleep -Seconds 40
taskkill /PID $p.Id /T /F
```

- A picture of the car screen is saved every 3 seconds as `screen-000.png`, `screen-001.png`, …
  and the newest also as `latest.png`.
- The head unit does not stop by itself; the last line stops it.
- The script is a list of `event@seconds`, counted from the moment the phone connects:

| Event | Meaning |
|---|---|
| `rotate:N` | turn the knob N clicks (negative = the other way) |
| `dpad:center` | press the knob. Also `dpad:up`, `down`, `left`, `right` |
| `key:CODE` | an Android key code: 4 Back, 3 Home, 66 Enter, 85 play/pause |
| `park`, `drive` | the car reports parked or moving |
| `night`, `day` | the car's lights |
| `tap:X,Y` | a touch (not with `--no-touch`) |
| `hold:CODE,MS` | hold that key for MS milliseconds (head unit after 0.1.29) |
| `double:CODE` | two presses of that key 120 ms apart (head unit after 0.1.29) |
| `holdturn:CODE,N` | hold that key while the knob turns N clicks (head unit after 0.1.29) |

Key codes the bench uses for the car's controls until the real ones are known (plan task R8):
23 knob push, 85 wheel click, 82 Menu, 4 Back.

Leave at least 3 seconds between events so each one can have its own picture. A picture is saved
at most every 3 seconds and only when a new video frame arrives.

### 7. By hand: phone screenshots and the log

```powershell
adb shell screencap -p /sdcard/myplay-shot.png
adb pull /sdcard/myplay-shot.png C:\myplay\shots\<version>\phone-01.png
adb logcat -d | Select-String -Pattern 'aare|AareAap' | Out-File C:\myplay\shots\<version>\phone-log.txt
```

Do not use `adb exec-out screencap -p > file.png` in PowerShell: it damages the picture.

MyPlay also keeps its own connection log on the phone: *Advanced > Connection log*.

### 8. Report

As before: push to branch `claude/test-reports` of `WhiteRain3/MyPlay`, in
`test-reports/<version>/REPORT.md` with the pictures beside it. Say for each check what was done,
what was expected and what the picture shows. A head unit result says the app works; it does not
say the box or the Audi's knob behaves the same.

## What to check in 0.1.36 and 0.1.37

In the car
1. Phone, Layout: turn on "Try MyPlay's own car screen" and choose Design: DECK. Turning the knob swings the cards; a push opens the front one; Back comes home.
2. Push the turn arrow (top left): Waze fills the screen beside a thin strip with a Home button; Back comes home.
3. Have someone call you while Waze is open: answer with a knob push. After the call, turn the knob: it should still move around Waze.
4. A message with Reply arriving while an app is open: turn to Reply and push, or press Back to dismiss it. The knob works the app again straight after.
5. Speed limits (phone, Speed: Lithuania downloaded, speed source Auto): while driving, the limit shows under your speed. Note any place where it's wrong. If it doesn't show, the Car page's "Speed limit" line says why.
6. In a busy group chat while driving: say if messages take the knob too often.
7. DECK's Apps page: do your apps' icons show clearly in the tiles?

On the PC bench (the desktop head unit)
8. DECK: tap the visible corner of a card behind the front one. It should come to the front, not open the front card.
9. With no maps app chosen, push Navigate: the Apps page lists only maps apps.
10. Apps page: Settings, Car data, Phone and Notifications open MyPlay's own pages, and Back returns to Apps.
11. Switch ORBIT and DECK on the phone while connected: the car screen changes at once.
12. Phone, Speed: download Lithuania on Wi-Fi. It shows the file's date and size.

## The Audi profile

| Option | Why |
|---|---|
| `--res 1280x720` | the video size the car asks for |
| `--dpi 213` | the density Google's app reported for the owner's screen |
| `--no-touch` | knob and buttons only |
| `--route none` | no simulated drive (it starts a Paris to Marseille drive otherwise) |
| `--margins 0,0,80,0` | left, top, right, bottom pixels the car does not show (from plan task B1 on) |

The car shows about 1200 of the 1280 pixels across, with the unused part on the right, as Google's
app reported it. What the box itself reports is not known until the owner's parked test; the
margin is then corrected. With a head unit older than task B1, leave `--margins` out and ignore
what happens in the rightmost 80 pixels.

## Checks worth making on every build

1. It connects and the home screen appears.
2. Every knob click moves a visible ring, and only one ring is visible.
3. Pressing the knob opens the ringed thing; Back returns.
4. The same with the **phone locked** (a known open question on Android 14 and later). If the
   knob does nothing, look in the phone log for "Touch mode switch rejected".
5. `drive`, then `park`: what is hidden while moving comes back.

## About `headunit.sh` on Windows

The repository's `headunit.sh` is a Unix shell script and is not used on Windows. It does three
things, and the steps above do each of them directly:

| `headunit.sh` does | On Windows |
|---|---|
| `adb forward tcp:5277 localabstract:aare_hu` | the same command (step 4; the bench script does it too) |
| switches on the app's head unit server, which only debug builds allow from outside | step 3, by hand, once (releases are not debug builds) |
| builds the head unit with Gradle and starts `bin/aare-headunit` | the release zip already holds the built program with a Windows starter, `bin\aare-headunit.bat` (Gradle's `application` plugin makes both starters; seen in a build of this repository) |

Building the head unit from source on Windows (`gradlew.bat :headunit:installDist`) is possible
in principle but needs a JDK 17 to 21 and the Android SDK, because the project also contains the
app. It has not been tried. Use the release zip.

## What this cannot test

- Whether the owner's box accepts MyPlay (its certificate check and its wireless start-up).
- Which key codes the Audi's knob and buttons really send.
- Sound through the car.
