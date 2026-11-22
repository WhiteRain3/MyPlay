# MyPlay phone app: screen designs

Visual design for the rebuilt phone app, drawn to the structure in `docs/plans/phone-app.md`
(revision 2) and its review. These are pictures only, with no app code. Dark theme only.
Screens are 412 × 915 dp, rendered at 2×. The font is Inter (OFL), the icons are Lucide (ISC), and all
sample data is invented (car "Sedan", region "Northvale", app "TrailNav").

**Overview:** [`00-overview.png`](00-overview.png) · **Style sheet:** [`31-style-sheet.png`](31-style-sheet.png)

## The idea

The app is a sibling of ORBIT, not a copy. It borrows ORBIT's rounds, rings, calm near-black glass
and its **world colours**. Each tab has one accent, the way ORBIT tints the rings of the world
you're in: Home is green (Navigate), Screen violet (Music), Car amber (Car), Apps pink (Apps) and
More blue (Settings). The accent marks only selection, the primary action, switches and rings; body text is never in
the accent. Everything else is plain lists that you can scan: an icon disc, a label, at most one help
line, and the value on the right. Each screen has one filled (primary) button. Rings carry state:
the connection, progress, waiting, an error, the slider thumb and the selected tab. A small accent dot
means "changed from default".

## Screens

| # | File | What it shows |
|---|---|---|
| 01 | `01-home-connected.png` | Connected. A big green ring with the filled car, "Sedan · USB", Details (collapsed), the small car picture, **Preview** and Disconnect. The Home tab icon is the filled car. |
| 02 | `02-home-not-connected.png` | Not connected because Bluetooth is off. A dashed grey ring and the outline car, one reason and one action ("Turn on Bluetooth"), a "Finish setup · 2 left" card and a dimmed picture. |
| 03 | `03-home-update.png` | Update available. A dismissible card with **Install** and What's new, a compact status card, and an amber badge on More. Preview drops to ghost so Install is the one primary. |
| 04 | `04-screen.png` | Screen root. The large car picture with its tappable parts ringed and labelled (Maps, Speed, Design, Sound, per §3.5 for ORBIT), then every page with its value and changed dots. |
| 05 | `05-screen-looks.png` | Looks. Built-in and saved looks as swatch tiles, the one in use ringed, "Save this look", Open a file and Share. |
| 06 | `06-screen-looks-apply.png` | The apply sheet (§6.1). It lists what the look changes and asks first, because this look switches to DECK. |
| 07 | `07-screen-colours.png` | Colours. A live preview, then App screens: theme discs, accent discs and Your own colours. Below that, Day and night (segmented, "Also on the car") and Reset this page. |
| 08 | `08-screen-size.png` | Size. A live preview and three stepped sliders: Text size, Control size, and How much fits (tagged "Next connection"). |
| 09 | `09-screen-design.png` | Design. ORBIT in use, a RACE slot ("Coming later"), Panels close after, and a pointer to Archive. |
| 10 | `10-car.png` | Car root. Every page with its value, and Passenger mode as a switch with one warning line. |
| 11 | `11-car-buttons.png` | Buttons. A drawing of the pad, knob, Menu and Back with the knob selected. Its gestures: press, hold, double and **triple** press. Then Pad shortcuts, Teach a car button and Timing. |
| 12 | `12-car-pad-shortcuts.png` | Pad shortcuts. A round pad diagram (up, centre, down, side) with the picker sheet open for "Pad up". |
| 13 | `13-car-teach.png` | Teach a car button, while waiting. A breathing ring with a countdown, Cancel, and the buttons already taught. |
| 14 | `14-car-speed.png` | Speed. Source and region, the **region download in progress** as a ring at 64 % with Cancel, the "Warn when over by" slider, the privacy line, and the Waze slot (disabled). |
| 15 | `15-car-maps.png` | Maps. Map on home, idle → home, Follow the car's lights, the car's GPS, a cross-tab link to Map colours (↗ Screen), and the Maps apps list. |
| 16 | `16-apps.png` | Apps root. This page *is* the car's app list: Phone apps at the top, a switch on every app, ⚙ on Phone screen, Browser and **YouTube**, and Car data. The list icon opens Reorder. |
| 17 | `17-apps-reorder.png` | Reorder mode. Drag handles plus up/down buttons as the non-drag alternative. One row is lifted. |
| 18 | `18-apps-phone-apps.png` | Phone apps. The main switch with its warning line, the allowed apps with their knob-turn setting, Add apps, and Keep the phone awake. |
| 19 | `19-more.png` | More root. Updates ("2.4 ready" tag), Cars, Connection, Backup, Diagnostics, then Advanced, Archive and About. |
| 20 | `20-more-updates.png` | Updates, the single home for updates. A ring, "Version 2.4 is ready", **Install 2.4**, What's new, Check now, and Check automatically ("Once a day, from GitHub"). |
| 21 | `21-more-car-check.png` | Car check, the **4-button step**. A step bar (5 of 11) and four rings for the pad buttons: two heard, one waiting (dashed and breathing), one to go. "Heard from the car", then Nothing happened and **Next**. |
| 22 | `22-more-car-check-share.png` | Car check finished, with the **share sheet**. "Phone numbers and names are removed", the **Include alert texts** switch (off by default; "Map alerts can name streets"), See what is shared, and Share. |
| 23 | `23-more-advanced.png` | Advanced. Video switches (tagged "Next connection"), Certificates, Car fixes, and a Developer row. |
| 24 | `24-more-developer.png` | Developer. Head unit server and its four sub-switches, map areas, maps text, the phone parts sample, and a ↗ link to the protocol log, which lives in Connection log. |
| 25 | `25-more-archive.png` | Archive. "Kept as it was. Not updated any more.", DECK (a picture and its switch), and Older layouts. |
| 26 | `26-more-about.png` | About. The mark, the version, and the links. |
| 27 | `27-state-setup.png` | First run (Setup step 1 of 4, no tab bar). Only what connecting needs: Nearby devices, Location and Notifications, then **Allow** and Skip for now. |
| 28 | `28-state-permission.png` | A permission asked in context. Messages and calls opens with an inline "Allow contacts and calls" card; the rows that depend on it are dimmed until it's allowed. |
| 29 | `29-state-error.png` | An error. A red partial ring, "Couldn't connect", the reason in one line, **Try again**, What happened, and Run a car check. |
| 30 | `30-state-empty.png` | An empty list (Cars). Faint rings, "No cars yet", and one line of help. |
| 31 | `31-style-sheet.png` | The style sheet: colours with computed contrast, the type scale, spacing and shape, components (row, switch, slider, segmented control, buttons, tags, rings, card, sheet, snackbar, tab bar), and motion notes. |

## Rules applied

- **Text limits are enforced by the build.** Every label goes through `L()` (≤ 30 characters) and
  every help line through `H()` (≤ 60). The build stops if one goes over.
- **Contrast is enforced by the build.** `stylesheet.cjs` computes WCAG ratios and stops if any text
  colour drops under 4.5:1 on bg, surface or surface2, or if text on an accent fill does. The
  lowest ratio is text3 on surface2, at 5.2:1.
- Touch targets are ≥ 48 dp. Rows are 56–64 dp (72 with a help line). Icon-only controls (search, ⚙,
  arrows, close) sit in 48 dp circles.

## Sources and rebuilding

- `html/*.html`: each screen as a self-contained HTML file (generated).
- `src/style.css`: tokens and components. `src/parts.cjs`: shared parts (rows, rings, tab bar, the
  ORBIT and DECK pictures). `src/screens.cjs`: every screen. `src/stylesheet.cjs`: the style sheet.
  `src/icons.json`: the Lucide icons used (ISC licence).
- To rebuild: `node docs/design/phone-app/src/build.cjs` (all screens), or pass a prefix to build only
  matching ones, e.g. `… build.cjs 14`. It needs Playwright and Chromium.

## Disagreements with the plan, and open points

1. **Two car icons.** The plan makes the Home tab icon a car (outline → filled when connected), and there is also
   a tab called **Car**. Two car icons in one bar would be confusing. I kept the car for Home, as the
   plan says, and gave the Car tab a gauge icon. If the owner prefers a house for Home, the
   "connected" signal needs another non-dot cue.
2. **Triple press isn't in the code.** `ButtonsScreen.gesturesFor` offers press, hold, double and
   hold+turn only. I drew "Triple press" with a "New" tag because the brief asks for it. It needs
   gesture-engine support, or it should be dropped.
3. **The car check's "4-button step".** The code has four separate pad steps (`PAD_UP` … `PAD_RIGHT`,
   14 steps in all). I drew them as one step, "Press each pad button, in any order" (11 steps).
   I recommend merging them. The plan doesn't cover this.
4. **"Include alert texts" isn't in the plan.** I drew it off by default, with the reason as its help
   line, because alert texts can name places. `CarCheckLog` already redacts navigation text, so the
   switch decides whether to keep that redaction.
5. **Live preview placement.** The plan (§11) asks for a live preview on Screen pages but doesn't say where. I put a
   small one at the top of Colours and Size, and the Screen root's large picture is itself the preview.
6. **The error detail is a row, not ⓘ.** The plan puts the full last error behind ⓘ in the status
   card. I used a "What happened" row (opening the text), because ⓘ on a big error hero is easy to
   miss.
7. **The Apps list is cut.** Phone and Notifications (built-ins in §3.2), and "Apps that can't run",
   are below the fold in `16-apps.png`. They're not dropped.
8. **ORBIT reference pictures.** I couldn't open the `1-orbit*.png` sheets in AA-Experiments from
   this session (access to that repository was denied). The sibling language is taken from ORBIT as
   built in this repository (`ui/car/myplay/orbit/OrbitTokens.kt` for colours and glass, `OrbitSkin.kt`
   for the layout: turn bubble, clock, hub with the trip ring, the speed sign and the song disc).
   The car picture is my own drawing of that layout. It should be checked against the sheet.
