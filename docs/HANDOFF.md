# MyPlay handoff (WhiteRain3)

Every session working on MyPlay reads this file first and updates it last. Sessions do not share
memory; this file is the shared memory. Keep it short and factual. If something here is wrong,
fix it in the same commit as the work that showed it was wrong.

State when written (Planner, 2026-11-18): the repository is still unchanged AARE at `37f8236`.
App name AARE, package `net.prototux.aare`. Only documents have been added. The task list is
`docs/plans/myplay.md`; the Tester's guide is `docs/TESTER_SETUP.md`.

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

What AARE consists of (about 27,000 lines of Kotlin in the app, 288 unit tests in all):

| Folder | What |
|---|---|
| `aap/` | the protocol, plain Kotlin, with tests against a simulated head unit |
| `app/` | the Android app: car session, car screen, phone screens, sound, wireless |
| `headunit/` | a desktop head unit: a car screen on a PC, for testing (`docs/TESTER_SETUP.md`) |
| `tools/magisk/`, `xposed/` | optional extras for rooted phones; not released by MyPlay yet |
| `docs/` | AARE's notes (certificates, per-car settings, decisions) and ours |

## The owner's rules

These come from the owner and are not up for reinterpretation.

1. No analytics and nothing that phones home. The only allowed network use is an updater that
   reads this fork's GitHub releases when the owner asks.
2. Small commits with clear messages. After each major step, a short summary and how to test it.
3. Where the effect of something is not known, say so. Never invent descriptions.
4. No third-party logos, artwork or fonts.
5. Plain English in the app and in messages to the owner. They are not an Android developer.
6. Keep upstream's credits and the licence (GPL-3.0-or-later). Every released APK has its source
   public in this repository.
7. The app must not pose as AARE or as Android Auto. Its name is MyPlay.

Where unchanged AARE does not yet meet these rules (plan tasks A1, A2 and F7 deal with them):

- Rule 1: the update check is on by default and asks about upstream's releases; the weather on
  the car screen is on by default and sends an approximate position to Open-Meteo; a "Download
  certificate" button fetches files from a third party's GitHub repository when tapped.
- Rule 4: three built-in styles are named after and modelled on Android Auto and CarPlay, and one
  style bundles a third-party clock font. Open question 3.
- Rule 7: everything is still called AARE.

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
- A Tester session on the owner's PC drives a phone over adb and reports to a branch. The PC's
  operating system is not known to us.
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
Google's terms, and it is a network download beyond the updater. The first parked test is done
without it, so we learn what the box does with MyPlay's own certificate. Open question 1.

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

### How this repository releases today (upstream's way)

- `.github/workflows/release.yml` runs when a tag `v1.2.3` is pushed. It publishes "AARE
  <version>" with the APK, a Magisk module, the desktop head unit and checksums.
- It signs with the secrets `AARE_KEYSTORE_BASE64`, `AARE_KEYSTORE_PASSWORD`, `AARE_KEY_ALIAS`,
  `AARE_KEY_PASSWORD`. **Without them it publishes a debug-signed APK**, whose key is new on
  every run, so one such release cannot be installed over another.
- `.github/workflows/ci.yml` builds and tests pushes to `main` and pull requests only.
- The fork has no tags and no releases. GitHub turns Actions off on a new fork until the owner
  enables them.

### What changes for MyPlay (plan task A3; not built yet)

- New workflow files, so upstream's stay untouched: `myplay-ci.yml` and `myplay-release.yml`.
- The version lives in `myplay-version.txt` on `claude/myplay`. A push that changes it to a
  version with no release yet tests, builds, signs and publishes release `myplay-v<version>`.
  A push that does not change it publishes nothing. This is the pattern of the AA-Experiments
  fork.
- Secrets, to be added by the owner: `MYPLAY_KEYSTORE_BASE64`, `MYPLAY_KEYSTORE_PASSWORD`,
  `MYPLAY_KEY_ALIAS`, `MYPLAY_KEY_PASSWORD`. A release fails if the keystore is missing; there is
  no debug-signed fallback.
- No phone-certificate secrets.
- Signing key: the owner reuses the key of their AA-Experiments fork if they still have the
  keystore file (GitHub never shows a secret again, so the file itself is needed), otherwise a
  new one. **The key must never change after the first release**, or the phone will refuse
  updates until MyPlay is uninstalled, which loses its settings. The same goes for the package
  id `io.github.whiterain3.myplay`.
- Signing key fingerprint (SHA-256): not recorded yet. The Release session writes it here after
  the first release and checks every later release against it.
- Upstream's `v*` tags must never be pushed to this repository: they would run upstream's
  release workflow here. The owner can also switch that workflow off (owner's list, step 3).

Release rules carried over from the earlier project, each learned the hard way:

- Only the Release step changes the version. Workers leave `myplay-version.txt` alone.
- A release is made only from a commit the Reviewer called CLEAN, and only if the branch tip is
  exactly that commit.
- After pushing a version change, push nothing else to the branch until the release is confirmed
  at `https://github.com/WhiteRain3/MyPlay/releases/latest`.

## What the owner must do, in order

On GitHub (repository `WhiteRain3/MyPlay`):

1. **Actions tab**: press the green button "I understand my workflows, go ahead and enable them".
2. **Settings > Secrets and variables > Actions > New repository secret**, four times:

   | Name | Value |
   |---|---|
   | `MYPLAY_KEYSTORE_BASE64` | the keystore file, as base64 text |
   | `MYPLAY_KEYSTORE_PASSWORD` | the keystore's password |
   | `MYPLAY_KEY_ALIAS` | the key's name inside the keystore |
   | `MYPLAY_KEY_PASSWORD` | the key's password (often the same as the keystore's) |

   These are the same four values as `ANDROID_KEYSTORE_B64`, `ANDROID_KEYSTORE_PASSWORD`,
   `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` in the AA-Experiments repository, if the
   keystore file still exists. If it does not, say so and a session will give the one command
   that makes a new key. Keep a copy of the file and passwords somewhere safe either way.

   Do **not** add `AARE_PHONE_CERT` or `AARE_PHONE_KEY`.
3. **Actions tab > "Release" (upstream's workflow) > the "..." menu > Disable workflow.** Optional,
   a safety catch.
4. Tell the dispatcher it is done. Nothing is published until tasks A1 to A3 are built and
   reviewed.

On the phone and in the car, once the dispatcher says the first release is out: see the next
section.

## The parked test (the gate)

Car parked. Google's Android Auto stays installed as the daily driver.

1. If AARE or F-Auto is installed, uninstall it: MyPlay cannot be installed next to them.
2. Download `MyPlay-<version>.apk` from the release page and install it.
3. Open MyPlay and go through "Finish setting up".
4. *Advanced*: turn on "Verbose protocol log".
5. In Google's Android Auto settings, turn off wireless Android Auto, so the two apps do not
   both answer the box. Turn it back on after the test.
6. Start the car, wait for the box, and let the phone connect to it as usual. If nothing happens
   after a minute, forget the box's Bluetooth pairing on the phone and pair again.
7. If the phone asks to **Trust** the head unit, tap it and reconnect.
8. Whatever happens, open *Advanced > Connection log*, save it and send it. The line where it
   stops tells us which stage failed.
9. If MyPlay's screen appears on the car, also answer:
   - Photo of the screen: is anything cut off at the edges?
   - Turn the knob: does a ring move? Press it: does the ringed thing open?
   - Same again with the phone locked.
   - Press each MMI button and steering-wheel key once (they are written to the log).
   - Does music from the phone still play through the car? Does a navigation voice?
10. Do **not** tap "Download certificate" unless you have decided open question 1.

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
- No new network use, no new library, no font or artwork from outside without the owner's yes.
- Workers do not change the version.
- Whether releases go out automatically after a clean review, as in the earlier project, is open
  question 8. Until answered, the dispatcher asks before each release.
- Hand-offs are carried by the session that created them, with the Claude Code Remote
  `send_message` tool. Do not assume a hand-off happened: send it and say so, or say plainly that
  you cannot.
- If a task needs a decision from the owner, stop and ask through the dispatcher. Do not guess.
- Do not open pull requests unless the owner asks.

## Open questions for the owner

1. **Google's certificate.** If the box refuses MyPlay's own certificate, do you want to use
   Google's (the "Download certificate" button)? And whatever the answer: should that button
   stay as it is, be hidden, or be removed from MyPlay?
2. **Weather on the car screen.** It needs to send an approximate position to a weather service
   (Open-Meteo). The plan turns it off by default. Keep it as something you can switch on, or
   remove it?
3. **Styles named after other products.** AARE includes looks called "Android Auto", "Android
   Auto (2019)" and "CarPlay", and its "Retro" look uses a third-party clock font. Remove them
   under your rules, or keep any?
4. **Signing key.** Do you still have the keystore file from the AA-Experiments fork?
5. **Package id.** `io.github.whiterain3.myplay` is proposed. It can never change later. Say if
   you want another.
6. **The Tester's PC.** Windows, macOS or Linux?
7. **Google's Android Auto.** Does it stay installed as the daily driver while MyPlay is built?
   (The plan assumes yes.)
8. **Releases.** Automatic after a clean review, as before, or ask you each time?
9. **Languages.** AARE is translated into 20 languages. Is English only fine for everything new?

## Not known yet (answered by the parked test, not by the owner)

- Whether the box accepts MyPlay at all, and with which certificate.
- Whether the wireless start-up works with this box, or needs F-Auto's compatibility mode.
- What screen size, density and margins the box reports.
- Whether the knob moves focus while the phone is locked (Android 16).
- Which of the Audi's buttons the box passes on.
- Where sound goes while MyPlay is connected.
