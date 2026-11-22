# MyPlay phone app: screen designs

These pictures are the visual design for the rebuilt phone app (no code). They follow the structure in
`docs/plans/phone-app.md` (rev. 3) and the design review in `review.md`. **This is the second pass:** all 14
review findings are fixed, and the dispatcher's overrides are applied (see below). Every option list, value
type and default comes from the code (`claude/myplay`, 0.1.47). Only cars, regions, apps and dates are
invented, for example "Sedan", "Northvale" and "TrailNav".

- The theme is dark only.
- Screens are 412 × 915 dp, rendered at 2×. Pages marked *whole scrolled page* are captured at their full height.
- The font is Roboto Flex (OFL), the platform's own font, so the phone will match these pictures. Icons are Lucide (ISC).

**Overview:** [`00-overview.png`](00-overview.png) · **Style sheet:** `77-style-sheet.png` · **Large screen:** `76-large-screen.png`

## The idea

The app is a sibling of ORBIT, not a copy: rounds, rings and calm near-black glass, set in plain lists you can scan.

- **World colours mark identity only.** Each tab keeps ORBIT's world colour: Home green, Screen violet, Car
  amber, Apps pink, More blue. That colour appears only on the selected tab's ring, the glyph by the root title
  and the hero ring.
- **One neutral colour (`int`, #DCE3EF) marks interaction on every tab.** It is used for switches,
  sliders, primary buttons, selection and focus.
- **Signals each have their own hue.** `caution` (#FF9A57, orange) means "needs attention": the tab badge,
  card dots and the "ready" tag. No tab uses orange. `warn` (#FF7A7A) is for errors and the warning lines on
  risky switches. `ok` is green.
- **"Changed from default" is a hollow ring**, and the changed value is set brighter. Filled dots mean
  "needs attention" only.
- **Rows have one shape.** Each row has an icon disc, a label, at most one help line, and a value or control
  on the right. A chevron opens a page; ↗ leaves the tab or the app.
  - Three-step choices are segmented controls.
  - Int settings with more values open a picker sheet.
  - Each screen has one primary button.

## Screens

<!-- screens -->
| # | File | What it shows |
|---|---|---|
| 01 | [`01-home-connected.png`](01-home-connected.png) | **Home — connected.** The status hero in the Home world colour, Details collapsed, the small car picture (tap → Screen), one primary action. |
| 02 | [`02-home-connecting.png`](02-home-connecting.png) | **Home — connecting.** The ring sweeps while MyPlay connects; nothing to do but wait or cancel. |
| 03 | [`03-home-not-connected.png`](03-home-not-connected.png) | **Home — not connected (Bluetooth off).** One reason, one action. The other reasons (Wi-Fi off, Wireless off in MyPlay, service stopped) use the same layout. |
| 04 | [`04-home-trust.png`](04-home-trust.png) | **Home — trust a new car.** The one security decision in the app. The fingerprint is in ⓘ; the warning is a single line. |
| 05 | [`05-home-update.png`](05-home-update.png) | **Home — update available.** The hero stays; the update card sits below it (dismissible for that version). Badge on More. |
| 06 | [`06-home-deck.png`](06-home-deck.png) | **Home — an archived design in use.** A quiet card when DECK (or an older layout) is on; it opens More › Archive. |
| 07 | [`07-home-error.png`](07-home-error.png) | **Home — an error.** Couldn’t connect: the reason in one line, Try again, and a “What happened” row (agreed in review). |
| 08 | [`08-screen.png`](08-screen.png) | **Screen — the tab root.** The large car picture, clean. Tapping a part opens its page; Design is the first row. In the build the picture is the live render (CarPicture), not this drawing. |
| 09 | [`09-screen-pressed.png`](09-screen-pressed.png) | **Screen — while the picture is pressed.** Part rings show only while a finger is down (and on the first visit). The hub’s speed opens Car › Speed; Design is the first row. |
| 10 | [`10-screen-design.png`](10-screen-design.png) | **Screen › Design.** ORBIT in use, the RACE slot, and ORBIT’s own settings. Panel time is an Int picker (3/5/8/15 s/Never). |
| 11 | [`11-screen-looks.png`](11-screen-looks.png) | **Screen › Looks — a saved look’s menu.** The real built-in looks. Each saved look has a menu (Rename, Share as a file, Delete with undo); no page-level Share. *(whole scrolled page)* |
| 12 | [`12-screen-looks-apply.png`](12-screen-looks-apply.png) | **Screen › Looks — the apply sheet.** Before a look is applied: what it changes, and a confirm because this saved look uses DECK. |
| 13 | [`13-screen-colours.png`](13-screen-colours.png) | **Screen › Colours.** The preview is an app screen inside ORBIT — what these colours change — labelled “App screens”, sticky while the page scrolls. The real 8 themes and accents. *(whole scrolled page)* |
| 14 | [`14-screen-own-colours.png`](14-screen-own-colours.png) | **Screen › Colours › Your own colours.** Night and day apart; seven parts, each “Yours” or “The look’s own”, with undo per part and a contrast warning. *(whole scrolled page)* |
| 15 | [`15-screen-wallpaper.png`](15-screen-wallpaper.png) | **Screen › Wallpaper.** None, three built-in gradients, or a photo; the photo has Edit and Choose another. Live preview at the top. |
| 16 | [`16-screen-photo-editor.png`](16-screen-photo-editor.png) | **Screen › Wallpaper › Edit the photo.** Full screen: drag and pinch on the car-shaped frame, Darker and Blur, then Use as wallpaper. |
| 17 | [`17-screen-map-colours.png`](17-screen-map-colours.png) | **Screen › Map colours.** Before/after is the page’s preview (sticky). Presets as chips, then the six sliders from the code. *(whole scrolled page)* |
| 18 | [`18-screen-size.png`](18-screen-size.png) | **Screen › Size.** Text and control size are three-step segmented controls (Default/Larger/Largest). How much fits: Auto or a fixed dpi from a list. |
| 19 | [`19-screen-size-fixed.png`](19-screen-size-fixed.png) | **Screen › Size — a fixed density.** Choosing Fixed opens the dpi list (an Int picker); the chosen value shows on its own row. |
| 20 | [`20-car.png`](20-car.png) | **Car — the tab root.** Every page with its value. Distinct icons: knob for Buttons, a limit sign for Speed. Passenger mode carries the warning line. |
| 21 | [`21-car-200.png`](21-car-200.png) | **Car — at 200 % font size.** Text grows, rows grow and values wrap under the label; nothing clips. Icons and targets keep their size. *(whole scrolled page)* |
| 22 | [`22-car-buttons.png`](22-car-buttons.png) | **Car › Buttons.** Dash knob and steering wheel in one drawing. Knob push has press, hold, double, triple and hold + turn; holds show disabled with the note, because this car sends none. *(whole scrolled page)* |
| 23 | [`23-car-buttons-action.png`](23-car-buttons-action.png) | **Car › Buttons — choosing an action.** An action list, not a switch. Choices that would leave no way out are not offered (the code’s rule). |
| 24 | [`24-car-pad-shortcuts.png`](24-car-pad-shortcuts.png) | **Car › Buttons › ORBIT home: pad shortcuts.** The code’s names. “Home: information only” on top; the four keys and their real actions (Music, Maps, The dial, Apps, Calls, Nothing). *(whole scrolled page)* |
| 25 | [`25-car-timing.png`](25-car-timing.png) | **Car › Buttons › Timing.** The two timing sliders. The hold slider is disabled with the reason, because this car sends no holds. |
| 26 | [`26-car-teach.png`](26-car-teach.png) | **Car › Buttons › Teach a car button.** Choose what it should do, then Teach it. Taught buttons list their action and key, each with Forget (undo). |
| 27 | [`27-car-teach-waiting.png`](27-car-teach-waiting.png) | **Car › Teach — waiting for the button.** A breathing ring counts down the 30 seconds; the car screen shows the same prompt. |
| 28 | [`28-car-teach-offline.png`](28-car-teach-offline.png) | **Car › Teach — not connected.** Teach (and the Car check) need the car: one line and nothing to tap until it connects. |
| 29 | [`29-car-maps.png`](29-car-maps.png) | **Car › Maps.** Map on home and “Home when maps untouched” are pickers (the latter an Int: Off/1/2/5/10 min). Map colours is a cross-tab link. |
| 30 | [`30-car-maps-idle.png`](30-car-maps-idle.png) | **Car › Maps — Home when maps untouched.** The Int picker for myplay_maps_idle_home. |
| 31 | [`31-car-speed.png`](31-car-speed.png) | **Car › Speed.** Source is three-way; the margin is an Int picker (Off/5/9/15). The limits file shows size and date, Look for a newer one and Delete. *(whole scrolled page)* |
| 32 | [`32-car-speed-download.png`](32-car-speed-download.png) | **Car › Speed — downloading.** The file row becomes a progress ring with Cancel; the rest of the page stays usable. |
| 33 | [`33-car-speed-offline.png`](33-car-speed-offline.png) | **State — offline (Car › Speed).** Offline: one line where the action failed, Try again; never a dialog. |
| 34 | [`34-car-sound.png`](34-car-sound.png) | **Car › Sound.** The busiest page: the switch, the method in use, the three methods best first with their states, and Play music when connected. *(whole scrolled page)* |
| 35 | [`35-car-sound-setup.png`](35-car-sound-setup.png) | **Car › Sound — nothing set up yet.** No method ready: the status says so, and the method that can work now is one tap. |
| 36 | [`36-car-messages.png`](36-car-messages.png) | **Car › Messages and calls.** The normal state. Do Not Disturb adds its access row when turned on; Auto-reply shows its text field when on. *(whole scrolled page)* |
| 37 | [`37-car-messages-permission.png`](37-car-messages-permission.png) | **State — permissions asked in context.** Two inline cards, each over only the rows that need it. Rows stay readable; their controls are disabled (dashed). *(whole scrolled page)* |
| 38 | [`38-car-voice.png`](38-car-voice.png) | **Car › Voice assistant.** Same as the phone (default) or a chosen app; each says where it opens. |
| 39 | [`39-apps.png`](39-apps.png) | **Apps — the whole list (scrolled).** The tab root is the app list: Phone apps on top, every group, the built-ins (Phone, Notifications…), a hidden app, Car data, and “Apps MyPlay can’t show” collapsed. *(whole scrolled page)* |
| 40 | [`40-apps-reorder.png`](40-apps-reorder.png) | **Apps — reorder.** Drag or the arrows. A hidden app keeps its place, greyed, so showing it again puts it back where it was. |
| 41 | [`41-apps-phone-apps.png`](41-apps-phone-apps.png) | **Apps › Phone apps.** The risky switch with its warning line (reason in ⓘ). Each allowed app chooses what knob turns move: Up and down, or Left and right. *(whole scrolled page)* |
| 42 | [`42-apps-phone-apps-no-root.png`](42-apps-phone-apps-no-root.png) | **Apps › Phone apps — without root or Shizuku.** The switch is disabled and one line says why, with a link to set it up on the Sound page. |
| 43 | [`43-more.png`](43-more.png) | **More — the tab root.** Rare things, each one tap away; values on the right. The update tag uses the attention colour, matching the tab badge. |
| 44 | [`44-more-updates.png`](44-more-updates.png) | **More › Updates — ready.** The one home for updates. The hero ring is in More’s world colour; Install is the primary action. |
| 45 | [`45-more-updates-installing.png`](45-more-updates-installing.png) | **More › Updates — installing.** Download progress in the ring and on the button; then Android’s own confirm window. |
| 46 | [`46-more-updates-failed.png`](46-more-updates-failed.png) | **More › Updates — failed, and “allow unknown apps”.** Each failure is one line with Try again. The first install needs Android’s “install unknown apps” permission: one card, one action. |
| 47 | [`47-more-updates-current.png`](47-more-updates-current.png) | **More › Updates — up to date.** Nothing to do: a calm full ring and the version. |
| 48 | [`48-more-whats-new.png`](48-more-whats-new.png) | **More › Updates › What’s new.** Release notes as short lines, newest first. Shown once after an update, never on a fresh install; the update question is asked here once after the rebuild. *(whole scrolled page)* |
| 49 | [`49-more-cars.png`](49-more-cars.png) | **More › Cars.** One list: each car with its last connection and how, and a Trusted tag (the pin). Forget lives on the car’s page. |
| 50 | [`50-more-car.png`](50-more-car.png) | **More › Cars › a car.** A car’s own page: its trust fingerprint, the per-car slot, and Forget (undo snackbar; the next connection asks for Trust again). |
| 51 | [`51-more-cars-undo.png`](51-more-cars-undo.png) | **More › Cars — after Forget (undo snackbar).** Forget acts at once and offers Undo for a few seconds: the rule for things that can be undone. |
| 52 | [`52-more-cars-empty.png`](52-more-cars-empty.png) | **State — an empty list (More › Cars).** Faint rings, one line. |
| 53 | [`53-more-connection.png`](53-more-connection.png) | **More › Connection.** Only how the phone reaches the car: Wireless and the optional Bluetooth address (video moved to Advanced). |
| 54 | [`54-more-backup.png`](54-more-backup.png) | **More › Backup.** Save and Load, the last save, and the one line about what is not in the file. |
| 55 | [`55-more-backup-confirm.png`](55-more-backup-confirm.png) | **More › Backup — load confirm.** Loading can’t be undone, so it asks (the confirm rule). |
| 56 | [`56-more-backup-loaded.png`](56-more-backup-loaded.png) | **More › Backup — after loading.** The result, skipped keys counted, and the after-loading list: what to set again on this phone. *(whole scrolled page)* |
| 57 | [`57-more-diagnostics.png`](57-more-diagnostics.png) | **More › Diagnostics.** Three tools, each with its state. |
| 58 | [`58-more-car-check.png`](58-more-car-check.png) | **More › Diagnostics › Car check — the 4-button step.** Step 5: the four buttons round the knob, in any order. The four pad steps are drawn merged into step 6 (12 steps, not 15). |
| 59 | [`59-more-car-check-share.png`](59-more-car-check-share.png) | **More › Car check — done, share.** The steps summary, then “Include alert texts (may contain street names)”, unticked each time, and Share the log. *(whole scrolled page)* |
| 60 | [`60-more-car-check-offline.png`](60-more-car-check-offline.png) | **More › Car check — not connected.** The check listens to the car, so it starts only when connected. |
| 61 | [`61-more-car-data.png`](61-more-car-data.png) | **More › Diagnostics › Car data.** The car-screen switch, the car’s identity, and live values (tabular numbers). *(whole scrolled page)* |
| 62 | [`62-more-log.png`](62-more-log.png) | **More › Diagnostics › Connection log.** The two log switches (Detailed protocol log lives only here), the last lines, Copy / Save / Clear. *(whole scrolled page)* |
| 63 | [`63-more-advanced.png`](63-more-advanced.png) | **More › Advanced.** Video: Maximum resolution is an Int picker (480p/720p/1080p/1440p), the others are switches. Certificates, car fixes, Developer. *(whole scrolled page)* |
| 64 | [`64-more-advanced-resolution.png`](64-more-advanced-resolution.png) | **More › Advanced — Maximum resolution.** The picker pattern for an Int setting: a sheet of the real values, the current one checked. |
| 65 | [`65-more-certificates.png`](65-more-certificates.png) | **More › Advanced › Certificates.** The list with expiry, Import and Download, and the automatic update switch. |
| 66 | [`66-more-certificates-password.png`](66-more-certificates-password.png) | **More › Certificates — PKCS#12 password.** The one text dialog in the app; it survives rotation. |
| 67 | [`67-more-developer.png`](67-more-developer.png) | **More › Advanced › Developer.** The real sub-switches. Risky ones carry the warning line in the warning colour, reason in ⓘ. *(whole scrolled page)* |
| 68 | [`68-more-archive.png`](68-more-archive.png) | **More › Archive.** DECK and the older layouts, kept as they were. |
| 69 | [`69-more-archive-layouts.png`](69-more-archive-layouts.png) | **More › Archive › Older layouts.** Everything that does nothing under ORBIT, grouped and working as before. Weather keeps its privacy line. *(whole scrolled page)* |
| 70 | [`70-more-about.png`](70-more-about.png) | **More › About.** Version, links, thanks, licences, map data. |
| 71 | [`71-search.png`](71-search.png) | **Search — results.** One search icon per tab. Results name their tab; a setting under an off switch says “Turn on X first”. |
| 72 | [`72-setup-1.png`](72-setup-1.png) | **Setup 1 of 4 — what connecting needs.** First run, no tab bar. Only nearby devices, location and notifications; the rest is asked in context later. |
| 73 | [`73-setup-2.png`](73-setup-2.png) | **Setup 2 of 4 — Sound.** The method this phone can use now, offered as one action; the others stay on Car › Sound. |
| 74 | [`74-setup-3.png`](74-setup-3.png) | **Setup 3 of 4 — the update question.** Asked once; the answer writes check_updates (the default stays off). |
| 75 | [`75-setup-4.png`](75-setup-4.png) | **Setup 4 of 4 — connect.** USB or wireless, one line each; Done goes to Home. |
| 76 | [`76-large-screen.png`](76-large-screen.png) | **Large screen — rail and list-detail.** Tablet or unfolded phone: a navigation rail, the tab root on the left, the page on the right. Same routes as the phone. |
| 77 | [`77-style-sheet.png`](77-style-sheet.png) | **Style sheet.** Colours with computed contrast (text and non-text), type, space and shape, components, motion. |
<!-- /screens -->

## The review's findings and what changed

| # | Finding | What changed |
|---|---|---|
| 1 | Two Int settings drawn as switches | *Home when maps untouched* (Off/1/2/5/10 min) and *Maximum resolution* (480p–1440p) are now rows that open picker sheets (`30`, `64`). Every other Int is also a picker: panel time, speed margin, density. |
| 2 | Option lists invented | Real lists throughout: the 8 themes and the accents (Theme's, 6 colours, Use the phone's colours), the 5 built-in looks, Default/Larger/Largest, density Auto or Fixed (120–240 dpi), knob turns Up and down / Left and right, the 4 developer sub-switches, wallpapers, map presets and sliders, sound methods, speed choices, car-check steps, teach actions. |
| 3 | Buttons page incomplete | The drawing has the dash knob, the pad, Menu, Back and the steering wheel. Knob push lists press, hold, double, **triple** (it shipped) and hold + turn. *Show what a button did*, Timing and Reset this page are added (`22`–`25`). |
| 4 | Pad shortcuts missing the info-only home | The page uses the code's names: *ORBIT home: pad shortcuts*, with *Home: information only* on top (`24`). The four real keys have their default actions. |
| 5 | Permission state dimmed the wrong rows | There are now two inline cards, each over only the rows that need it. Notification access covers banners, groups, read aloud and auto-reply. Calls and contacts covers calls on the phone speaker. The rows stay readable; only their controls are disabled (`37`). |
| 6 | Amber meant four things | World colours are for identity only, and one neutral interaction colour is used everywhere. Caution is orange. "Next connection" is a neutral tag with an icon. |
| 7 | "Changed" dot clashed with attention | "Changed" is a hollow ring plus a brighter value. Filled dots mean attention only. |
| 8 | Non-text contrast | Off switches have a `text3` outline. The build checks non-text contrast at ≥ 3:1: switch outline, slider rail, dashed rings, focus ring and the changed mark (see the style sheet). Disabled rows no longer fade their text. |
| 9 | Colours previewed the wrong surface | Colours previews an **app screen** inside ORBIT, labelled "App screens · live", and the preview stays at the top while the page scrolls (`13`). Wallpaper and Map colours have their own previews (`15`, `17`). |
| 10 | Picture labels and targets | The picture is clean, with one hint line. Part rings appear only while it is pressed (`09`). The hub's speed opens Car › Speed, and Design is reached from the root's first row. The Wallpaper value matches the picture. |
| 11 | Connected cue on the Home tab | Home is a house and Car is a car. The tab has no connected cue (critic's open point 1). |
| 12 | Missing screens | All are added: Trust (`04`), Sound and its not-set-up state (`34`, `35`), Developer with warnings (`67`), Wallpaper and the photo editor, Map colours, Your own colours, Messages, Voice, Timing, Cars and a car's page with undo, Connection, Backup (confirm and after loading), Diagnostics, Car data, Connection log, Certificates and the PKCS#12 password, Older layouts, What's new, search results, Speed's normal state, connecting, installing, failed with "allow unknown apps", up to date, offline, setup steps 1–4, the "Using DECK" card, Teach and Car check when not connected, Phone apps without root or Shizuku, 200 % font size (`21`), and a large-screen frame (`76`). |
| 13 | Risky switches unstyled | Passenger mode, phone apps, Head unit server and its risky sub-switches have a warning line in `warn` with an icon, and the reason in ⓘ. |
| 14 | Small things | **Card borders:** highlighted cards use the neutral border. **Home layout:** one status layout, with the update card below the hero. **Icons:** Buttons is the knob, Speed is a limit sign, Car is a car. **Reorder:** it is a text button. **Looks:** each saved look has a menu (Rename, Share as a file, Delete with undo), and the page-level Share is gone. **Looks value:** it reads "Midnight (changed)". **Teach:** each taught button has Forget. **Font:** Roboto Flex. |

## Decided overrides (dispatcher), where they differ from the critic

- **Triple press is kept.** It shipped in 0.1.46: on the knob and the wheel, triple press is the previous song.
- **Holds are shown, but disabled.** This car sends no holds, so *Press and hold* and *Hold and turn* appear
  disabled with "This car doesn't send holds" (`22`, `25`).
- **"Include alert texts" is kept.** It shipped as a check box, unticked each time the Car check share opens (`59`).
- **The four pad steps of the car check are drawn merged into one step**, so the check has 12 steps instead of
  15. The merge is a later code change.

## The critic's open points, as followed

1. **Tab icons:** Home is a house and Car is a car. There is no tab-level connected cue. §4 of the plan should say so.
3. **Merging the pad steps:** agreed. They are drawn merged. Keep one log line per button so old and new logs compare.
5. **Previews:** Size uses the car picture, and Colours uses an app screen. Wallpaper and Map colours have previews, and
   they stay at the top of their pages.
6. **The error detail:** the hero states use a "What happened" row. §10 of the plan should say so.
7. **The Apps list:** drawn as a whole scrolled page with Phone, Notifications, a hidden app and "Apps MyPlay
   can't show". In Reorder, hidden apps stay in place, greyed.
8. **The ORBIT pictures:** the car pictures are placeholders. In the build, the picture is the live render from
   the car's own code (`CarPicture`). The owner should check the drawn layout against the ORBIT sheet before any
   marketing image is made from these PNGs (the AA-Experiments sheets couldn't be opened from this session).

## Notes for the plan and the build

- **Names follow the code.** The plan's route `car/buttons/home-shortcuts` should become `car/buttons/pad-shortcuts`.
- **Some rows move from where the code has them today, as the plan says:**
  - Video goes to Advanced.
  - Resume music becomes *Play music when connected* in Car › Sound.
  - Calls on the speaker goes to Messages and calls.
  - Car data's switch is shown in the Apps list. Its home is still the Car data page.
- **Labels shortened to the 30-character limit (meaning kept):**
  - "Maps full screen: home when untouched" becomes "Home when maps untouched".
  - "Turn off network mode after a session" becomes "Off after one session".
  - "Allow localhost (webaa / paath)" becomes "Allow localhost", with the clients in ⓘ.
  - "Home: information only (no ring)" becomes "Home: information only", with "No ring on home" as its help line.

## Rules the build enforces

- Every label goes through `L()` (≤ 30 characters) and every help line through `H()` (≤ 60). The build stops on
  any overrun.
- **Contrast:** text must be ≥ 4.5:1 on every surface, and so must text on any fill. Non-text parts that show
  state must be ≥ 3:1. The build computes these in `stylesheet.cjs` and stops on any failure.
- Touch targets are ≥ 48 dp. Rows are 56–64 dp (72 with a help line). Icon-only buttons carry `aria-label`s in the
  sources, standing in for TalkBack labels.

## Sources and rebuilding

- `html/*.html`: each screen as HTML (generated).
- `src/style.css`: tokens and components.
- `src/parts.cjs`: rows, rings, the tab bar, and the ORBIT, app-screen and DECK pictures.
- `src/screens.cjs`: every screen.
- `src/tablet.cjs`: the large-screen frame.
- `src/stylesheet.cjs`: the style sheet and the contrast checks.
- `src/icons.json`: Lucide icons.
- `src/fonts/`: Roboto Flex and its OFL licence.
- Rebuild with `node docs/design/phone-app/src/build.cjs`. It needs Playwright and Chromium. It renumbers the
  PNGs and rewrites the screen table above.
