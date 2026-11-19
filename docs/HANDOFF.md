# MyPlay handoff (WhiteRain3)

Every session working on MyPlay reads this file first and updates it last. Sessions do not share
memory; this file is the shared memory. Keep it short and factual. If something here is wrong,
fix it in the same commit as the work that showed it was wrong.

State on 2026-11-19: **MyPlay 0.1.0 is released** (tag `myplay-v0.1.0` on commit `0520dae`),
and so is 0.1.1 (tag `myplay-v0.1.1` on `970cc4c`: the car's buttons and knob in the phone's own
log). 0.1.0
is AARE renamed to MyPlay (package `io.github.whiterain3.myplay`), with updates and weather off
by default, its own release chain, and a test bench for the Tester. Nothing about the look or the
knob has changed yet. Next is the owner's parked test against the box, which decides the order of
everything after it.

- Task list: `docs/plans/myplay.md`.
- Tester's guide: `docs/TESTER_SETUP.md`.
- What other forks of AARE have built, what to take and what to avoid:
  `docs/research/forks.md` on branch `claude/research-forks`. The plan already uses it.

## What this fork is

MyPlay is the owner's personal fork of AARE (https://github.com/prototux/AARE, GPL-3.0-or-later,
by prototux and contributors). AARE replaces the phone side of Android Auto: the phone speaks
Android Auto's protocol to the car itself and draws its own car screen. Google's Android Auto app
is not involved. So, unlike the owner's earlier project, nothing here hooks into Google's app:
the whole car screen is our own drawing code, and every look detail can be an ordinary setting.

The goal: an app made for one car. Knob-only control that works everywhere, a layout that fits
the Audi's small screen, the owner's dark design on the phone, plenty of settings, and ordinary
phone apps (video included) usable with the knob. Video is for when parked, or for a passenger.

- Repository: `WhiteRain3/MyPlay`.
- `main` stays an untouched copy of upstream AARE, so upstream can be merged in. Nobody commits
  to it.
- All MyPlay work is on `claude/myplay`.
- F-Auto (https://github.com/malashhab/F-Auto) is another AARE fork, used as reference only.
  The owner wants the work based on AARE.

What AARE consisted of when forked (about 27,000 lines of Kotlin in the app, 288 unit tests in
all; MyPlay's own commits have added to both):

| Folder | What |
|---|---|
| `aap/` | the protocol, plain Kotlin, with tests against a simulated head unit |
| `app/` | the Android app: car session, car screen, phone screens, sound, wireless |
| `headunit/` | a desktop head unit: a car screen on a PC, for testing (`docs/TESTER_SETUP.md`) |
| `tools/magisk/`, `xposed/` | optional extras for rooted phones; not released by MyPlay yet |
| `docs/` | AARE's notes (certificates, per-car settings, decisions) and ours |

## The owner's rules

These come from the owner and are not up for reinterpretation.

1. Small commits with clear messages. After each major step, a short summary and how to test it.
2. Where the effect of something is not known, say so. Never invent descriptions.
3. Plain English in the app and in messages to the owner. They are not an Android developer.
4. Keep upstream's credits and the licence (GPL-3.0-or-later).
5. The app must not pose as AARE or as Android Auto. Its name is MyPlay.
6. Network use only where a feature needs it. Today that is: the updater (reads
   `WhiteRain3/MyPlay` on GitHub), cover art for music, and the weather when it is turned on.

**Two rules were dropped on 2026-11-19.** "Nothing phones home" and "no third-party logos,
artwork or fonts" were the owner's rules for their earlier project (AA-Experiments). The
dispatcher carried them over; the owner never set them for MyPlay and has said they do not apply
here. So third-party artwork and fonts are allowed (with their licence text kept in the app, as
AARE does), and AARE's looks named after Android Auto and CarPlay, its Retro clock font and cover
art all stay. Earlier commits and messages that cite those rules are out of date on that point.

Places that still say AARE on purpose or by leftover (found in the review of 0.1.0, not a
question any more): the name inside the certificate the app makes for itself, which is left
alone until after the parked test because head units have accepted it as it is; the "make"
reported to car apps; one look called AARE; three texts. Credits to AARE stay in any case.

## The owner's setup

- Phone: Android 16, arm64, rooted.
- Car: Audi A4 2012-2015 with MMI 3G, no factory Android Auto. 7-inch screen, **no touch**,
  driven by a rotary knob.
- Car screen as Google's Android Auto sees it today: 1200 x 723 at 213 dpi; the video is
  1280 x 720 with about 80 pixels unused on the right.
- Android Auto comes from an aftermarket box, so **the box is the head unit MyPlay talks to**,
  not the Audi. Sold by carplay.lt (Kaunas) as "Apple CarPlay and Android Auto integration for
  Audi MMI 3G Basic / 3G High / 3G+", non-optical version, wireless Android Auto. A generic
  decoder box with no brand printed. Codes on it: `GP0A2020H-Q5H` (probably the board revision)
  and `EWMCP-N726` (probably its Bluetooth and Wi-Fi name). Box label Q5H, CAN cable Q5U, DIP
  switches 000000 (A4 2012-2015, HN+).
- Sound goes over the car's own Bluetooth.
- A Tester session on the owner's PC (Windows) drives a phone over adb and reports to a branch.
- The owner has not tried AARE or F-Auto in the car.

## The biggest unknown: does the box accept MyPlay?

Not known. Nobody has tried. AARE's own list of 30 cars has both kinds: head units that accept
its certificate and head units that only accept Google's. Nothing in it is this box. Everything
in the plan after phase A depends on the answer, which is why the first tasks only produce a
build the owner can try parked.

### Certificates: what this repository does by default

Read from `docs/certificates.md`, `app/build.gradle.kts` and the release workflow.

- When phone and head unit connect, each shows the other a certificate. Google's app always
  shows the same Google-issued one. Some head units check for it and refuse anything else.
- **This repository does not contain Google's certificate or key, and no build includes them.**
- A build can carry a "built-in" certificate of its own if the secrets `AARE_PHONE_CERT` and
  `AARE_PHONE_KEY` are set when it is built. Upstream uses that for its own self-signed AARE
  certificate. **MyPlay does not set them and the plan's release workflow does not pass them.**
- Without them, the app makes its own self-signed certificate on the phone the first time it
  starts, and offers that. Head units that do not check accept it. Head units that check refuse
  it.
- The app has a button, *Advanced > Phone certificates > Download certificate*. When tapped it
  downloads Google's certificate and key, which a third party extracted from the Android Auto
  app and publishes at github.com/iscle/android-auto-certificates. An automatic refresh of that
  download exists and is off by default. That certificate's own end date is 2026-12-31; AARE's
  notes expect Google to replace it.
- The app can also import a certificate file the user supplies.
- If a head unit refuses one certificate, the app offers its next one on the next connection,
  and the connection log says so.

**Using Google's certificate is the owner's decision and nothing here assumes it.** It is against
Google's terms. The first parked test is done without it, so we learn what the box does with
MyPlay's own certificate. Open question 1. The fork report's advice: add no code that extracts or
forces Google's certificate, and never commit a certificate or key.

## What has been seen so far

**On the PC bench (the Tester, 2026-11-19, MyPlay 0.1.0, Audi profile, the owner's phone). Not
the car, not the box, no sound.** Full report with pictures: `test-reports/0.1.0/REPORT.md` on
branch `claude/test-reports`.

- Works: connects in about a second every time with MyPlay's own certificate; no crashes; the
  knob works with the phone locked; nothing is drawn under the 80-pixel strip on the right; day
  and night switch; the ring moves on the home screen, the bar and the app grid; pressing opens
  things; Back returns from the grid, the call list and the voice bar. The bench script runs on
  Windows.
- Broken, most important first: inside apps and MyPlay's own car Settings the knob only moved
  along the bar, so Organic Maps' buttons and every Settings option were not reached; Back does
  not leave Fermata Mirror; one stop on the home screen shows no ring; the play button's ring
  can barely be seen; the grid's last entry is ringed below the screen's edge; the "Organic
  Maps" button's name is cut; six grid entries are all called "Android Auto".
- Also: the Music entry opens the full app grid; the first picture is always dark; opening a
  maps app from the home screen makes it the remembered maps app.
- Not tested on the bench: tilting the knob, sound, playing music or video, driving restrictions
  inside apps, a call.

What to do about each is in the plan under "Bench round 1" (K1 to K11).

**In the car: nothing yet.** The parked test has not been done.

## Build and test

Checked in a cloud session on 2026-11-18, on unchanged AARE:

```bash
ANDROID_HOME=/opt/android-sdk ./build.sh --no-daemon --console=plain :aap:test :headunit:test :app:testDebugUnitTest :app:assembleDebug :headunit:installDist
```

- It ended with BUILD SUCCESSFUL. 288 tests, none failed (protocol 74, head unit 6, app 208).
- The first run took about five minutes in two parts and downloaded Gradle 9.8.2, the libraries,
  Android SDK Platform 37 and Build-Tools 36 by itself. Later runs are faster.
- Needs a JDK 17 to 21 (`build.sh` finds one) and the Android SDK. The session had the SDK at
  `/opt/android-sdk` with platform 36; if a fresh session has no SDK at all, see the same section
  of the AA-Experiments handoff for how to install one.
- Results: `*/build/test-results/`. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
  Desktop head unit: `headunit/build/install/aare-headunit/`.
- This is the same task list as upstream's `.github/workflows/ci.yml`. Never commit when it fails.

APKs built in a session are signed with a throwaway key and cannot be installed over a released
build. Do not send them to the owner; ship through a release.

Nobody in the cloud can see the phone, the box or the car. Everything is verified by building,
by unit tests, by the Tester's head unit pictures, and by what the owner reports. Report
honestly: "builds and passes tests, not run on a device" until someone has seen it. A head unit
result does not prove the box or the Audi's knob behaves the same.

## Releases

### Released so far

| Version | Tag and commit | Date | Notes |
|---|---|---|---|
| 0.1.0 | `myplay-v0.1.0` = `0520dae` | 2026-11-19 | First build to try against the car. Released by the Worker after the owner said yes in its chat |
| 0.1.1 | `myplay-v0.1.1` = `970cc4c` | 2026-11-19 | The car's buttons and knob are written to the connection log on the phone (plan task A7). Same signing key, version code 101. The first release run failed on a download of the Paparazzi plugin from Maven and succeeded when the same commit was run again (plan task I5) |

Signing key: a new key made by the owner, certificate name `CN=MyPlay`. **SHA-256 fingerprint:
`5c39f23994ffbdb49e073d1c1393ea0af9768db3ba481b64b735c363ee7943c1`.** Every later release must
show the same fingerprint (`apksigner verify --print-certs`); whoever releases checks it. The key
and the package id `io.github.whiterain3.myplay` can never change now: the phone would refuse
updates until MyPlay is uninstalled, which loses its settings.

(Release state, the fingerprint and who released are as the dispatcher reported them; the
Planner did not download the APK.)

### How a release happens

- The version is the one line in `myplay-version.txt` on `claude/myplay`.
- `.github/workflows/myplay-release.yml` runs on every push to that branch. If the version has
  no release yet (and is not `0.0.0`), it tests, builds, signs with the owner's key and publishes
  release `myplay-v<version>` with `MyPlay-<version>.apk`, `myplay-headunit-<version>.zip` and
  `SHA256SUMS`. A push that does not change the version publishes nothing. Its signing and
  publishing steps have now run once, successfully.
- `.github/workflows/myplay-ci.yml` builds and tests every push to the branch.
- It signs with the repository secrets `MYPLAY_KEYSTORE_BASE64`, `MYPLAY_KEYSTORE_PASSWORD`,
  `MYPLAY_KEY_ALIAS`, `MYPLAY_KEY_PASSWORD`. If the keystore secret is missing the release fails;
  there is no debug-signed fallback.
- No phone-certificate secrets are set or passed (see "Certificates").
- Upstream's own workflows are still in the repository. Its `release.yml` (on `v*` tags) has
  been limited to `prototux/AARE` by commit `807f2fe`, so it does nothing here. Upstream's `v*`
  tags should still never be pushed to this repository.
- MyPlay very likely cannot be installed next to AARE or F-Auto. All three define the same
  permission (`android.car.permission.TEMPLATE_RENDERER`), and Android refuses a second app that
  defines a permission already defined by an app with a different signature. Read from the
  manifest and Android's rules; not tried on a phone.

### Release rules, each learned the hard way

- Only the Release step changes `myplay-version.txt`. Everyone else never touches it.
- A release is made only from a commit the Reviewer called CLEAN, and only if the branch tip is
  exactly that commit.
- **After pushing a version change, push nothing else to the branch until the release is
  confirmed** at `https://github.com/WhiteRain3/MyPlay/releases/latest`. This includes documents.
- So far the owner has been asked before each release (open question 8).

### Possible later, not decided

The owner is considering a private repository with updates served from their own Raspberry Pi
web server, as their other app does. To be looked at again after the parked test (plan task I3).

## What the owner has done, and what is left

Done:

- Let Claude push to this repository (2026-11-19). If a push is ever refused again with "Claude
  doesn't have GitHub access", the fix is at
  https://github.com/apps/claude/installations/select_target.
- Enabled Actions on the fork (2026-11-18).
- Made a new signing key and added the four `MYPLAY_...` secrets. **Keep a copy of the key file
  and its password somewhere safe** (a password manager, a USB stick). GitHub never shows a
  secret again, and without the file no update can ever be installed over the current app.

Left:

- The parked test, below.
- The open questions further down.

## The parked test (the gate)

Car parked. Google's Android Auto stays installed as the daily driver.

1. If AARE or F-Auto is installed, uninstall it: MyPlay cannot be installed next to them.
2. Download `MyPlay-0.1.0.apk` from https://github.com/WhiteRain3/MyPlay/releases and install it.
3. Open MyPlay and go through "Finish setting up".
4. *Advanced*: turn on "Verbose protocol log".
5. In Google's Android Auto settings, turn off wireless Android Auto, so the two apps do not
   both answer the box. Turn it back on after the test.
6. Start the car, wait for the box, and let the phone connect to it as usual. If nothing happens
   after a minute, forget the box's Bluetooth pairing on the phone and pair again.
7. If the phone asks to **Trust** the head unit, tap it and reconnect.
8. Whatever happens, open *Advanced > Connection log*, save it and send it. The line where it
   stops tells us which stage failed.
9. Note what the connection log says about the screen size and margins the box reports (the
   lines after `head unit:`), or simply send the whole log.
10. If MyPlay's screen appears on the car, also answer:
    - Photo of the screen: is anything cut off at the edges?
    - Turn the knob: does a ring move? Press it: does the ringed thing open?
    - Tilt the knob in each direction: does anything happen?
    - Same again with the phone locked.
    - Does music from the phone still play through the car? Does a navigation voice?
11. Do **not** tap "Download certificate" unless you have decided open question 1.

Three things worth knowing beforehand:

- **Buttons.** 0.1.0 does not write the car's button presses into the log on the phone, only into
  the developer log that needs the PC. So "which buttons does the box pass on" waits for the next
  build (plan task A7), unless the Tester's PC is connected during the test.
- **If it connects and then drops, or Google's app appears instead.** Other people's forks hit
  this: with Google's app enabled, Android may hand it the car too. Say so in the report; the
  stronger fix is plan task A8.
- **If nothing happens at all after pairing.** Other people's adapters got stuck until the
  pairing was forgotten and made again; switching the adapter off and on did not help.

What each result leads to is in the table under task A4 of the plan.

## Roles

The session that created the others is the **dispatcher**. It hands every piece of work to the
right session and looks after the chain: routing and relaying, checking results, the release step
after a clean review, keeping this file current. It does not write code, plans or designs. If the
right session cannot do a task, it tells the owner instead of quietly doing the task.

- **Planner**: turns an idea into a short ordered task list with what each task touches and how
  it is verified. Reads the code first. Writes no app code.
- **Car screen worker**: what is drawn on the car and how the knob moves through it
  (`app/.../ui/car/`, `ui/theme/`, `host/`, `media/`, `mirror/`, `launch/`).
- **Phone app worker**: the phone's screens and settings (`ui/MainActivity.kt` and the new
  design's files, `Settings.kt`, the updater).
- **Connection worker**: protocol, wireless, the desktop head unit, build and release files
  (`aap/`, `app/.../transport/`, `service/`, `car/`, `headunit/`, `.github/`, `tools/`). Only
  needed when a task is there.
- **Reviewer**: reads a diff for bugs and rule violations, builds and tests, ends with CLEAN or
  HOLD. Changes nothing.
- **Release**: changes the version, pushes, confirms the release and the signing fingerprint.
- **Design reviewer**: looks at screenshots (phone and head unit) and says concretely what to
  change. Changes nothing.
- **Tester**: local on the owner's PC. Runs the desktop head unit and the phone
  (`docs/TESTER_SETUP.md`), reports with pictures to branch `claude/test-reports`. Cannot be
  messaged; read its branch or ask the owner.

One worker can hold two areas while the work is small. `ui/MainActivity.kt` (1,500 lines) and
`Settings.kt` are shared by many tasks: one writer at a time.

## Working agreement between sessions

- One writer per area at a time. Before starting, `git fetch origin claude/myplay` and work on
  top of it. Before pushing, fetch and merge again. Never force-push.
- A worker's task is one small change. It ends with: build and tests pass, a commit, a push, this
  file updated if a fact changed, and a short note of what was done and how it can be tested.
  Then it stops pushing and tells the dispatcher. "Stop when I say" does not work: sessions only
  read messages between tasks.
- Keep upstream mergeable: new MyPlay code goes in new files; edits to AARE's files are small; no
  renaming of packages or files, no reformatting. `main` is never committed to.
- Network use only where a feature needs it; a task that adds one says so in its note. A new
  library is named in the note too. Third-party artwork and fonts are allowed, with their
  licence text kept in the app.
- Before building something, look in `docs/research/forks.md`: take an unmerged branch of
  upstream AARE first, then a fork's version, then write our own. Keep the author of any commit
  taken and credit them on the About page. Do not copy what that report's "what not to do" lists.
- Workers do not change the version.
- Whether releases go out automatically after a clean review, as in the earlier project, is open
  question 8. Until answered, the owner is asked before each release.
- Documents are pushed like code: fetch first, build on the tip, never while a release is waiting
  to be confirmed, and never touching `myplay-version.txt`.
- Hand-offs are carried by the session that created them, with the Claude Code Remote
  `send_message` tool. Do not assume a hand-off happened: send it and say so, or say plainly that
  you cannot.
- If a task needs a decision from the owner, stop and ask through the dispatcher. Do not guess.
- Do not open pull requests unless the owner asks.

## Open questions for the owner

1. **Google's certificate.** If the box refuses MyPlay's own certificate, do you want to use
   Google's (the "Download certificate" button)? And whatever the answer: should that button
   stay as it is, be hidden, or be removed from MyPlay?
7. **Google's Android Auto.** Does it stay installed as the daily driver while MyPlay is built?
   (The plan assumes yes.)
8. **Releases.** Automatic after a clean review, as in the earlier project, or ask you each time?
9. **Languages.** AARE is translated into 20 languages. Is English only fine for everything new?
13. **Private repository and your own update server.** You are considering it; to be looked at
    again after the parked test.

The numbers are kept from earlier messages so they still match.

### Answered

| No. | Question | Answer |
|---|---|---|
| 2, 11 | Weather | Stays as in 0.1.0: off by default, works when turned on, the dashboard card unchanged (2026-11-19) |
| 3 | Looks named after other products, and the Retro clock font | They stay; the rule behind the question was dropped (2026-11-19) |
| 4 | Signing key | A new key was made; fingerprint under "Releases" |
| 5 | Package id | `io.github.whiterain3.myplay`, used in 0.1.0 and now fixed for good |
| 6 | The Tester's PC | Windows |
| 10 | Cover art | Stays on (2026-11-19) |
| 12 | Places that still say AARE | No longer a question; listed under "The owner's rules" |
| - | Where updates come from | `WhiteRain3/MyPlay` on GitHub (confirmed 2026-11-19) |

## Not known yet (answered by the parked test, not by the owner)

- Whether the box accepts MyPlay at all, and with which certificate.
- Whether the wireless start-up works with this box, or needs the adapter compatibility setting
  (plan task A5).
- What screen size, density and margins the box reports.
- Whether the knob moves focus while the phone is locked (Android 16).
- Which of the Audi's buttons the box passes on, and whether it passes on the knob's tilt.
- Where sound goes while MyPlay is connected.
