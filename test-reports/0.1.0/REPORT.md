# Tester report: MyPlay 0.1.0 on the PC bench

- Date: 2026-11-19, Tester session on the owner's Windows 11 PC.
- Phone: the owner's phone (Android 16, rooted), over USB/adb. MyPlay **0.1.0** installed
  (versionCode 100, installed 2026-11-19 15:11). The newest release on GitHub is also 0.1.0
  (`myplay-v0.1.0`), so nothing newer was available. Commit `8d07c0c` on `claude/myplay` (button
  and knob lines in the connection log) is newer than 0.1.0 but has no release, so it was **not
  tested**.
- Head unit: `aare-headunit` from `myplay-headunit-0.1.0.zip`, in `C:\myplay\aare-headunit`.
- Profile (from the script): `--res 1280x720 --dpi 213 --no-touch --route none --margins 0,0,80,0`.

**This bench says nothing about the real car, the aftermarket box or sound.** The head unit is
AARE's own desktop program, which accepts MyPlay's own certificate; the box may not. Key codes
come from the head unit, not from the Audi's knob. Sound was off (`--route none`, no audio
checked).

## What was run

| Run | How | Result |
|---|---|---|
| `home` | `Run-AudiBench.ps1 -Walk home` | connected, 18 pictures |
| `open-back` | `Run-AudiBench.ps1 -Walk open-back` | connected, 11 pictures |
| `drive-park` | `Run-AudiBench.ps1 -Walk drive-park` | connected, 10 pictures |
| `night-day` | `Run-AudiBench.ps1 -Walk night-day` | connected, 10 pictures |
| by hand: mic, app grid, music, grid to the end, Organic Maps, car Settings | the head unit started directly, headless, with my own `--script` event lists (same profile) | all connected |

"By hand" here means scripted knob events, not keys typed into a window: I could not type into
the head unit's window from this session. The events are the same ones the keys send.

Pictures were chosen to leave out personal data (the repository is public): the phone's own
screenshots, the call log, message notifications and the map are not included, only described.

## Results

### Connection

- MyPlay's car screen appeared in every run (8 runs plus one check). No connection failed.
- Time to first picture: about 1 to 2 seconds after the head unit started. In the phone log the
  connection took about 0.4 s from "Connecting to adb" to the first video frame
  (`15:22:51.724` to `15:22:52.140`).
- The head unit printed `Authenticated (TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256)` and the phone
  `connecting adb with certificate "Generated (self-signed)"`: MyPlay's own certificate, as
  intended.

### The 80-pixel strip on the right

- Nothing of MyPlay sits under the dark strip. The layout ends at about x = 1185 of 1200 in every
  picture, including the app grid, Organic Maps, the call list and the Settings page.
- Cut off, but not by the strip:
  - The "Organic Maps" chip on the home screen is too narrow: its name wraps to "Organ / ic / M…"
    and the third line is cut (`02`, `19`).
  - A third maps chip seems to be hidden (see Knob, home screen).
  - The car Settings page: the "Style" and "Theme" rows end in a cut-off "Mater…" chip, and the
    page continues below the bottom edge ("Layout") (`17`). Whether that part can be scrolled to
    is not known: the knob never got into the page.
  - The app grid's last entry, "Exit to car", is half below the bottom edge when ringed (`15`).

### Knob, screen by screen

| Screen | Visible ring? | Turning | Pressing | Back |
|---|---|---|---|---|
| Home | Yes, mostly (dark outline). **Two stops with nothing visible** | moves through: maps chips, (invisible stop), music card, previous, play, next, bell, rail items; wraps around | opens the ringed thing | n/a |
| Home, play button | **Barely**: the button only gets slightly darker (`05`) | | not tested | |
| "Car audio" message at the top | the knob never landed on its ✕ in any walk | | | |
| Mic (rail) | yes | | opens Google's voice bar on the car screen (`13`) | Back closed it |
| App grid | **a grey filled tile, not a ring** (`14`). Visible but faint | moves along rows, scrolls down | opens the app | returns home |
| App grid, last entry | **not visible**: "Exit to car" is ringed while below the edge (`15`) | after the end, the ring jumps to the rail and back into the grid | | |
| FS Fermata Mirror (opened from a maps chip) | no ring on its page (`07`) | not tested | | **Back did nothing**: the page stayed (`08`). Key 4 reached MyPlay (`AareSession: input key: code=4`) |
| Organic Maps (from the grid) | its own buttons (Search, Categories, zoom) **never got a ring** | the ring went straight to the left rail | | not tested directly: I pressed the rail's Phone entry from there, and Back from Phone returned home |
| Phone (rail) | not checked closely (personal data) | | opens the call list | returned home |
| Music (rail) | yes on the rail | | **opened a grid that looks the same as the app grid** (all apps, not only music). Whether it is meant to be a media list is not known | returned home |
| "Android Auto" entry in the grid | | | "Could not start Android Auto" with a Try again button (`16`) | returned home |
| Car Settings (grey "Settings" in the grid) | **no ring on any option** | **the ring jumps straight to the rail** (`18`); none of the options can be reached | pressing the rail's Home entry returned home | Back not tested on this page |

The invisible stop on the home screen (`03`, between the Organic Maps chip and the music card):
the picture differs from the next one only at the bell and slightly at the play button, so where
the focus is cannot be seen. **Probably** a third maps chip clipped by the card: the code shows up
to four (`CarShell.kt:494`, `navApps.take(4)`), and Waze is installed on the phone. Not confirmed.
Turning back once from the start of the home screen also gives one stop with nothing visible.

Screens where the knob **moves nothing visible or gets stuck**:
1. Home: one stop with no visible ring (probably a hidden chip), and the play button barely
   changes.
2. App grid: the last entry is ringed off-screen.
3. Fermata Mirror's page: no ring, and Back does not leave it.
4. Organic Maps: its own controls cannot be reached; turning only moves on the rail.
5. Car Settings: no option can be reached; turning only moves on the rail.

**Phone locked:** the phone's screen went off and locked during the by-hand runs
(`isKeyguardShowing=true`, `mWakefulness=Dozing`). The knob still moved the ring and opened
things on the car screen. The phone log did have `Touch mode switch rejected, caller (pid=…,
uid=10451) doesn't own the focused window` (15:27:35), with no visible effect in that run.
Fermata Mirror shows "Unlock the phone and grant the requested permissions" while locked, which
is Fermata's own text.

### Sizes

- 213 dpi at 1280x720: the rail icons, the clock and the card titles look readable. The Settings
  page text is small but readable. Not measured against any rule.
- Too small: the "Organic Maps" chip (name wraps and is cut).
- Too big: nothing found. Fermata's "Unlock the phone…" text fills its card, which is Fermata's
  drawing.
- Six grid entries are all called "Android Auto" (`14`). Three are called "Phone", two "Google",
  two "Settings", plus "SETTINGS". With the knob you cannot tell them apart.

### Day and night

- Yes, the look changes: `night` gives dark cards and a bright teal play button (`11`), `day`
  light cards (`12`). Both directions worked.
- **The first picture of every run is dark** (`01`), before the script sends `park`. The screen
  then turns light. Whether the head unit reports "night" at the start or MyPlay starts dark by
  default is not known.

### Driving and parked

- `drive` then `park` (`09`, `10`): **no visible difference** on the home screen. Whether
  anything on that screen is supposed to hide while driving is not known. Not checked inside an
  app such as video.

### Things that changed by themselves

- Opening a maps app from the home screen (Fermata, later Organic Maps) makes it the remembered
  maps app. The "Where to?" chips are then replaced by that app's own picture for every later
  connection. After Fermata, that card was an "Unlock the phone…" panel and later a black
  rectangle (`13`, `16`). Saved as `last_nav` in MyPlay's settings.
- Opening Fermata Mirror made Fermata open the phone's "Display over other apps" settings page on
  the phone. Nothing was changed there.

### Crashes, freezes, errors

- No crash, no freeze. No `FATAL EXCEPTION` or `ANR` in any log.
- Every run ends with `W AareAap : session failed: read failed … java.io.EOFException`. That is
  the script killing the head unit, so expected.
- One error line per connection, from the app's process, with no visible effect:
  `E iterain3.myplay: Failed to query component interface for required system resources: 6`
  (right after `MediaCodec: configure`).
- Repeated `I Aare : video: no frame from the encoder for 14xx ms; redrawing the car screen`
  while the screen is still. No visible effect.

### The bench script on Windows

It worked in all four walks, with no errors. Oddities:

1. **Characters garbled in `headunit-out.txt`**: `status: Connected, negotiating�`,
   `phone: ? 62%`, `nav=�`. The head unit's non-ASCII characters (probably "…" and symbols) are
   lost when written to the file on Windows. Cosmetic.
2. The script's `phone.png` is taken whatever the phone shows. In my later runs the phone was
   locked, so it was black (20 KB). In `open-back` it showed the phone's "Display over other
   apps" page, opened by Fermata. Worth knowing; not a script bug.
3. Starting it with `powershell -ExecutionPolicy Bypass -File …` loads the user's PowerShell
   profile, which on this PC prints a list of custom commands before the script's output.
   Adding `-NoProfile` to the documented command avoids that. Cosmetic.
4. Results go to `.\myplay-bench` under the current folder by default, i.e. inside the
   repository checkout (`tools\tester\myplay-bench`). I used `-OutRoot C:\myplay\bench`.
   `.gitignore` does not list `myplay-bench`. Worth adding, or changing the default.
5. Its log filter (`myplay`) also matches system lines that merely mention the package (power
   manager, game mode, adbd), about half the lines. Harmless, but noisy.
6. ffmpeg (used only by me for contact sheets) prints `Fontconfig error` on this PC. Not the
   script's problem.

## Not tested

- The real car, the box, the Audi's knob and buttons, sound (see the top).
- Tilting the knob (dpad left/right/up/down) anywhere.
- Media playback: nothing was played, and no media app was opened from the Music entry.
- Video apps, driving restrictions inside apps.
- A phone call, voice assistant beyond opening its bar.
- MyPlay's phone screens (settings, connection log) on the phone itself.
- The build at `8d07c0c` (not released).

## Put back

- MyPlay's remembered maps app (`last_nav`, set to Organic Maps by my runs) was removed. The
  home screen shows the "Where to?" chips again (`19`). A copy of the settings file from before
  that edit is at `/data/local/tmp/myplay-settings-backup.xml` on the phone.
- "Head unit server" was already on before this round and was left on.
- Nothing was changed outside MyPlay. The phone is left locked. Fermata may still show its
  "Display over other apps" request when next opened.
- "Download certificate" was not touched.

## Pictures

`01`–`19` beside this file, named after what they show. All are the car screen at 1280x720. The
dark band on the right is the head unit's 80-pixel margin and the pink line its border.

## Summary for the managing session

- Tested 0.1.0 (the latest release) on the PC bench: all 4 walks plus scripted knob runs for mic, app grid, music, Organic Maps, car Settings; connects in about 1 s each time, no crashes, works with the phone locked.
- Fix first: inside apps and MyPlay's own car Settings the knob only moves on the left rail. Organic Maps' buttons and every Settings option are unreachable, and Back does not leave Fermata Mirror.
- Also broken: on home, one ring stop is invisible (probably a clipped third maps chip) and play focus is barely visible; the grid's last item is ringed off-screen; "Organic Maps" chip text is cut; Music opens the full app grid; first frame always dark; opening a maps chip silently becomes the remembered maps app.
- Works: nothing under the 80-px strip, day/night switches, ring moves on home, rail and app grid, Press opens things and Back returns home from the grid, the Phone list and the voice bar; the bench script runs on Windows (output text garbled, cosmetic).
- Not tested: real car, box, Audi knob, sound, knob tilt, media playback, video, driving restrictions inside apps, build 8d07c0c.
