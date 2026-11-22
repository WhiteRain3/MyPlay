# Review of `phone-app.md` (phone app information architecture)

Independent review, 2026-11-22, of `docs/plans/phone-app.md` at commit 42d0488. I checked the plan
against the code in `ui/MainActivity.kt`, `ui/phone/*`, `Settings.kt`, `SettingsBackup.kt`,
`MyPlayBackupKeys.kt`, the `MyPlay*Settings` extension files, `host/CarRuntime.kt`,
`ui/car/CarShell.kt`, `ui/car/myplay/**` (ORBIT, DECK and the app frame), `car/CarSession.kt` and
the manifest. No app code was changed.

Severity: **blocker** = building the plan as written would break something or ship a wrong
structure; **major** = must be settled before the build starts; **minor** = fix while building.

---

## Part 1. Is the plan true to the code?

### What holds up

- **The key inventory is complete.** I listed every key read or written through
  `getX`/`putX`/`remove`/`contains` and every `KEY_*`/`const val` key in `app/src/main/java`, then
  checked each one against the plan. None is missing. The other prefs files (`driving_mode`,
  `head_unit_pins`) and the files on disk (`wallpaper.jpg`, `vehicles.json`, speed-limit files,
  certificate store) are all listed.
- **The backup column is correct.** Every key marked A is in `SettingsBackup.ALLOWED` or
  `MyPlayBackupKeys.ALLOWED`, and every key marked B is in `SettingsBackup.BLOCKED`. The rule
  "the three sets don't change, and a test checks that" is the right safeguard.
- **`car_key_map`: confirmed.** Nothing in the app writes it. The setter `Settings.carKeyMap` is
  never called, and the only assignments are to `CarSession`'s private copy. `CarSession` still
  reads it (`resolveCarKeyAction`, ahead of the button table), and it isn't in any backup set.
  Giving it no page is right.
- **`myplay_button_codes`: confirmed, and the "verify" can be closed.** There is no writer on the
  phone side or the car side. The only reader is `Settings.buttonTable`
  (`car/MyPlayGestureSettings.kt`). Because it is in `MyPlayBackupKeys.ALLOWED`, the only way it can
  get a value is by loading a backup file. Keep it as "internal, no page" and keep it in ALLOWED.
- **Clock, status icons, bar items, dashboard cards, layout spec and dark bar: confirmed.** Under
  ORBIT none of these change anything. ORBIT draws its own battery and clock (`OrbitTokens`), and
  `LocalStatusStyle`, `RailWidgets` and `DashCard` are used only by the older layouts.
- The diagnosis in §1 is accurate. Back resets the scroll (`AnimatedContent(page)`), there is a
  single `page` instead of a back stack, updates are split three ways, `CategoryRow`/`Header` are
  dead code, and `Page.CAR_SCREEN` can only be reached by tapping the whole picture.

### Findings

**1. Blocker: theme, accent, your own colours and text size *do* change what you see under ORBIT.**
§1, §6 and open question 2 suggest these might move to Archive. They can't.
`CarRuntime.Root()` wraps the whole car screen, ORBIT included, in
`CarTheme(theme = CarThemes.byId(carThemeId).withAccent(...).withCustomColours(...))`.
It also scales `LocalDensity.fontScale` by `car_text_size`. ORBIT's frame (`MyPlayAppFrame` →
`ContentArea`) draws every Android Auto app inside that theme: all of `host/templates/*`
(list, grid, map, navigation, message and search templates), `media/*` (now playing, browse,
search), messaging, the browser and the phone screen. All of these read `MaterialTheme.colorScheme`.
ORBIT also has about 50 `.sp` text sizes, so text size scales ORBIT itself. `car_density` sets the
codec dpi in `CarSession`, which changes everything on the car screen.
*Fix:* keep Theme, Accent and Your own colours in **Screen › Colours**, with the scope written into
the label: "App screens: colours of apps opened on the car screen". Keep Text size and How much
fits in **Screen › Size**. `car_control_size` goes through `LocalCarControlScale`, which is read
in `ui/theme/CarTheme.kt` and so probably applies to app screens too. Check it; don't archive it on
the assumption that it is unused. Update §1, §6, §11 and open question 2 to match.

**2. Major: under ORBIT, `rail_position` (bar side) does nothing, so it doesn't belong in Screen.**
§11 says "Screen › Clock and status, if ORBIT's frame reads it (`MyPlayAppFrame` uses
`railOnRight`)". The frame does read it, but only to place the skin's app rail, and
`OrbitSkin.appRail` is `false`. Only DECK, which has a rail, and the older layouts use the setting.
The "bottom" choice can't apply to the frame at all.
*Fix:* move "Bar side" to **Archive** (it applies to DECK and the older layouts).

**3. Major (privacy): the weather setting is in the wrong place, and the weather is fetched even when nothing shows it.**
ORBIT shows no weather; nothing under `ui/car/myplay` reads it. The plan still places Weather in
**Screen › Clock and status**. Worse, `MyPlayWeather.wanted()` looks only at `ui.layout`
(the older layout kind) and ignores `enginePreview`. Take an owner who had weather on in the bar or
on a card before ORBIT became the default. With ORBIT on, the app still sends the phone's
approximate position to Open-Meteo every 30 minutes, for a weather display that never appears.
*Fix:* move Weather to **Archive › Older layouts**, together with its privacy line. Separately,
raise a small code fix: `wanted()` should return false whenever MyPlay's own screen is on and its
design shows no weather. This is not part of the rebuild, but the rebuild should not leave the
setting in Screen, where it looks as if ORBIT uses it.

**4. Major: the plan says "verify" where the code already answers.**
The plan leaves Clock and status, the status icons and `rail_widgets` as "verify ORBIT". Findings
1–3 settle every one of them. Under ORBIT, the whole **Screen › Clock and status** page has no
effect. *Fix:* remove that page from Screen and move its contents into **Archive › Older
layouts**. Screen then holds Design, Looks, Colours (app screens + day/night), Wallpaper, Map
colours and Size. That is shorter and true to what the car shows. If a later ORBIT version draws
the clock or status from these settings, the page comes back to Screen.

**5. Major: the car-picture mapping sends ORBIT parts to the wrong pages.**
§3.3 maps "cards → Archive layouts (ORBIT has none; verify)". But `CarPicture.picturePartOf`
maps `CarPart.OrbitSong` to `CARDS` today, and `CarPart.OrbitTurn` to `WHOLE`. Under the plan, an
ORBIT owner who taps the song card would land in Archive.
*Fix:* write the mapping per design:

| Part | ORBIT | DECK / older layouts |
|---|---|---|
| hub | Screen › Design | Archive |
| song | Drive › Sound | Archive › Cards |
| speed | Drive › Speed | Drive › Speed |
| turn | Drive › Maps | — |
| wallpaper | Screen › Wallpaper | Screen › Wallpaper |
| bar, clock, status, cards | — | Archive › Older layouts |
| apps button | Apps | Apps |
| whole picture | Screen | Screen |

**6. Major: applying a Look changes settings the plan puts in Archive, including the design.**
`MyPlayLooks.KEYS` includes `myplay_engine_language`, `style_layout`, `myplay_layout_preset`,
`myplay_layout_spec`, `rail_position`, `dark_bar`, `rail_widgets`, `show_weather` and every
status and clock key. So one tap in **Screen › Looks** can switch the car to DECK, or quietly
change Archive settings the owner can no longer see.
*Fix:* keep the same keys in a look (storage must not change, and looks must stay compatible).
Add two rules to the plan. First, the Looks page lists what a look changes, in user words, before
it is applied. Second, a look that changes the design asks for confirmation ("This look uses
DECK"); afterwards the Home "Using DECK" card appears.

**7. Major: open question 3 contradicts migration rule 1 (and has a privacy cost).**
§11 says "No key is … given a new default". Open question 3 recommends turning
`check_updates` on by default. Changing the getter's default would quietly start daily
GitHub requests for every existing user who never touched the switch.
*Fix:* keep the default `false`. Ask once in the first-run Setup (and once in an update's "What's
new"), with "Check for updates automatically? (once a day, GitHub)". The answer writes the key
explicitly.

**8. Major: car-check progress lives inside the page, so the plan's promises can't be kept.**
`CarCheckPage` keeps its recorded results in `remember { mutableStateMapOf() }`. Only the step
index is in `rememberSaveable`, so the results are lost whenever the page is disposed. The plan
promises a Home card for an unfinished car check (§4, §10) and that half-done work is restored
(§3.4). Neither can work while the state lives inside the page.
*Fix:* hold the car-check run in a process-scoped holder, next to `CarInputEvents`. State plainly
that a run survives moving between pages and tabs but not the process being killed. If the process
dies, the Home card should say "Car check stopped — start again".

**9. Major: the notification deep links have no mechanism.**
Today every notification uses one `PendingIntent` that opens `MainActivity` with no extras
(`ProjectionService.kt:588`). The activity is `singleTask`, and `onNewIntent` handles only
`phone_parts` and the debug `hu_server` extra, which calls `recreate()`. §3.3 lists target routes
but doesn't say how a notification reaches them.
*Fix:* define one extra per destination (`route`), read in both `onCreate` and `onNewIntent`. It
switches to the tab that owns the route and pushes the page. Give each notification its own
request code, so the `PendingIntent`s don't collapse into one. State that `recreate()` (debug)
and rotation keep every tab's stack.

**10. Minor: backups restore the speed-limit region but not the file.**
`myplay_speed_limit_region` is in ALLOWED, but the region's file isn't in the backup. After
loading a backup on a new phone, Speed shows "Lithuania" with no data. The same gap exists for
the wallpaper photo; the import already skips `wallpaper=custom`.
*Fix:* after loading, show an "After loading" list on the Backup result: photo, speed-limit file,
certificates, trusted cars. Also give Speed an inline "Download Lithuania limits" row while the
file is missing.

**11. Minor: things the inventory misses.**
- `SpeedLimitDownload.checkIfDue` runs every time the app is opened (a network request to GitHub
  releases) and has no switch. Speed needs a one-line privacy note: "Looks for a newer file
  monthly".
- The debug-only adb extra `hu_server` (used by `./headunit.sh`) is missing. It isn't in the
  UI, but it must keep working.
- **vehicles.json Import** overwrites the file without asking. Add a confirm, or say in the plan
  that it doesn't need one.
- Backup **Load** reports skipped keys (`SettingsBackup.Imported.skipped`). The plan's states
  don't say where that message goes.
- Permission requests for **calendar** and **contacts/call log**: the plan doesn't say which
  feature needs each one under ORBIT. See finding 17.
- **Translations:** there are 19 `values-*` folders, holding upstream's strings only (for
  example, `values-de` has 453 of 951 strings). The rewrite will drop those translations or mix
  languages. See finding 22.

**12. Minor: `verbose_protocol_log` is put in two places.**
§11 puts it in Connection log and "also in Developer". That breaks principle 1 (one home per
thing). *Fix:* put it in Connection log only. Developer links to it.

---

## Part 2. The structure, critiqued

**13. Major: Apps doesn't yet earn a tab, and Drive is a vague bucket.**
- In the plan, the **Apps** root is a list of four rows that open pages ("On the car screen",
  Phone apps, Phone screen, Browser) and a YouTube row. A tab whose root is just a menu is a
  wasted door. *Fix:* make the Apps root *be* the car screen's app list: show/hide inline, a
  reorder mode, and Phone screen, Browser and YouTube as items in that list, each with a trailing
  ⚙. Phone apps (mirrored) becomes one entry at the top of the list. Then the tab answers "what
  is on my car screen" in one look, and new apps (more maps apps, a real YouTube) are just more
  rows.
- **Drive** holds Sound, Messages and Voice, which aren't about driving behaviour. The owner
  looking for "Sound" has a 1-in-3 chance of guessing the tab. *Fix:* call the tab **Car**
  (Buttons, Maps, Speed, Sound, Messages and calls, Voice, Passenger mode: everything the car
  *does*), or test both names with the owner before building. Whatever the name, the root of each
  tab must show every one of its pages with its current value. No "More options" at root level.
- Five tabs is the right count; don't add a sixth.

**14. Major: maps settings are spread over three tabs.**
"Map apps follow car lights" is in Screen › Colours; map colours are in Screen; the home map and
idle → home are in Drive › Maps; future maps apps go in Drive › Maps; maps apps are also in the
Apps list.
*Fix:* move "Maps follow car lights" to **Drive › Maps** (it is about the maps app, not the
car-screen look). Give Drive › Maps a "Map colours ›" row that links to Screen › Map colours.
Map colours stays in Screen because a look includes it.

**15. Major: "Your cars" and "Trusted cars" are two lists of the same cars.**
Two lists on one page (`known_cars` and the `head_unit_pins` file), each with its own Forget
button, make the owner work out which list means what. *Fix:* show one **Cars** list. Each car
has its last connection, how it connected and a "Trusted" tag. Forget clears both entries, after
a confirm. The storage stays exactly as it is. This is also the natural slot for per-car settings
later.

**16. Major: the Back, scroll and state rules leave four cases open.**
- **Pages opened from another tab.** §3.4 pushes "Home picture → Clock and status" onto
  *Home's* stack, so the same route can sit in two stacks with two scroll states, and the tab bar
  highlights Home while showing a Screen page. *Fix:* each page belongs to exactly one tab.
  A link from another tab switches to that tab and pushes the page there. Back from a page reached
  that way goes back to the tab it came from, once, the way search results do. Say this once and
  apply it to Home cards, search, notifications and the picture.
- **Predictive back** (Android 14+, the default from targetSdk 36) isn't mentioned. *Fix:* require
  `PredictiveBackHandler`/Navigation's predictive back for page pops and sheets.
- **Re-tapping the current tab** returns to its root and discards the stack. That is fine only if
  nothing can be lost. *Fix:* text fields (auto-reply text, browser start page, Bluetooth address)
  save as you type (or when you leave the page), so there is no such thing as a "half-filled" field.
  A search target's "scroll to row" takes priority over the restored scroll.
- **One source of truth.** Pages today copy settings into `remember { mutableStateOf(settings.x) }`.
  That is why `settingsGeneration` and `rememberSettingsChanges` exist. *Fix:* the builder
  observes settings as state, one flow per key or a prefs listener, instead of copying them.
  Loading a backup, changes made on the car and looks then refresh every page without rebuilding
  anything, and the navigation stack can't be reset by accident. Also state which dialogs survive
  rotation (colour picker, wallpaper editor: yes).

**17. Major: first run asks for too much up front and doesn't treat "not connected" as several states.**
- One button for about 12 runtime permissions (calls, contacts, call log, calendar, microphone,
  location, Bluetooth, nearby Wi-Fi, notifications) produces a string of system dialogs, and the
  owner can't tell what each one is for. *Fix:* first run asks only what connecting needs:
  Bluetooth/nearby devices, location and notifications. Ask for calls, contacts and call log when
  calls are first used or Messages is switched on. Ask for the microphone with Voice or Sound,
  and for calendar only when a layout that shows the calendar card is on (an Archive layout today).
  "Finish setup (N left)" counts only what is required. Optional items appear on their own pages,
  and the card can be dismissed.
- "Not connected" has more than one cause. *Fix:* add these states to §10: Bluetooth off; Wi-Fi off
  (wireless); "Wireless" switched off in MyPlay; the service stopped by the system, if it can be
  detected; offline, for Check now and downloads. Each gets one line and one action ("Turn on
  Bluetooth").
- **Empty states** aren't specified: no cars yet, no saved looks, no phone apps chosen, no speed
  file, an empty log. One line each.
- **Destructive actions:** Forget car, Delete look, Load backup, Reset buttons and Remove
  certificate need a single, consistent rule. *Fix:* use an undo snackbar where the action can be
  undone (delete look, forget car, reset buttons) and a confirm where it can't (load backup, remove
  certificate).
- **Dismissing cards.** `update_dismissed_version` and `cert_expiry_dismissed` exist, but the
  plan's attention cards don't say they can be dismissed. *Fix:* keep today's rules: an update
  card dismissed for that version, a certificate card dismissed until the next stage. A
  certificate that has expired can't be dismissed.

**18. Minor: the connection dot on the Home tab would look like an alert badge.**
§4 puts an optional dot on the Home tab while connected, and a badge on More for an update.
Two dots that mean different things will be misread. *Fix:* show "connected" by changing the
Home tab icon (outline car → filled car). Keep dots for "needs attention" only.

**19. Minor: search is offered in two places.**
§7 has a search field at the top of More *and* a search icon in every tab's top bar. *Fix:* use the
top-bar icon only, in every tab. Synonyms are string resources, so they can be translated.
Settings that only appear when their parent switch is on must still be found. The result opens
the parent and says "Turn on X first".

**20. Minor: Car data is in Diagnostics, but it has a car-screen app switch.**
"Show in the car" (`car_data_app`) adds an app to the car screen. *Fix:* its switch belongs in the
**Apps** list as a "Car data" item. The live sensor page stays in Diagnostics.

**21. Major: the content rules need numbers and a test, or the text will creep back.**
The rules are right, but nothing enforces them, and ⓘ sheets can easily become the new place for
paragraphs. *Fix:*
- Set limits: label ≤ 30 characters, help line ≤ 60, ⓘ sheet ≤ 3 sentences (about 300
  characters). Add a unit test over `strings_phone.xml` and `myplay_strings.xml` that fails when
  a phone string goes over its limit (classify strings by name prefix: `_label`, `_help`, `_info`).
- Add an appendix that lists each of today's 45 strings over 150 characters → its new label, help
  line, ⓘ text, or "deleted".
- Write the full table of root-row values (§4 gives three examples). Every root row needs one,
  for example: "Design — ORBIT", "Wallpaper — Photo", "Messages — Banners on", "Updates — Up to
  date".
- "From the next connection" is a small tag in one style, never a sentence.

**22. Minor: no translation policy.**
*Fix:* decide whether the phone app is English-only for now. If it is, remove the stale upstream
translations of the strings being replaced, so a German phone never shows a half-German page.
If not, plan a translation pass. Either way, the new strings get new IDs. Don't reuse old IDs,
because stale translations would then show the wrong text.

**23. Minor: the future slots are fine; add two.**
RACE, more maps apps, the Waze hook, the 4-button teach, regions and YouTube all fit without
restructuring. Add:
- **per-car settings** (Cars list → a car → its fixes, Bluetooth address, trusted state), since the
  owner has more than one head unit;
- a **"also on the car"** marker for settings the car-side screens also write (day/night, home
  maps, speed source and margin, panel timeout, banners, read aloud, sound, passenger mode), so
  the owner knows where else a value can change.

**24. Major: what "polished and timeless" needs that the plan doesn't cover.**
None of these is visual design, but each one shapes the structure:
- **"Changed from default" markers:** a small dot on each row whose value differs from the
  default, plus **Reset this page** at the bottom of each Screen and Car page. The owner tweaks a
  lot, and this answers "what did I change?" without reading anything.
- **Live feedback:** while a Screen page is open, show a small live car-screen preview (the
  picture already renders the car's own UI), so a change is seen and doesn't need explaining.
- **Accessibility:** 48 dp touch targets, TalkBack labels for every icon-only control (ⓘ, ⚙,
  reorder handles), layouts that work at 200 % font size, and a non-drag alternative for reorder.
- **Large screens:** on a tablet or unfolded phone, a navigation rail and list-detail layout. The
  per-tab stacks map onto this directly; say so now so routes are designed for two panes.
- **Theme:** today the phone app is always dark (an owner decision in `MainActivity`). Record that
  decision in the plan, so the design session doesn't reopen it.
- **Motion and haptics:** one rule (shared-axis between pages, fade between tabs, a light
  haptic on switches), written once.

---

## Verdict

The plan is a sound starting point. Its diagnosis is accurate, its key inventory is complete,
the backups are protected, and five tabs with their own back stacks fix the owner's main
complaints (Back losing the place, updates hidden away). It is **not ready to build** yet. The
claim about what ORBIT reads is half wrong (finding 1, blocker): theme, accent, your own colours
and text size still shape every app screen inside ORBIT and must stay in Screen. Clock and status,
bar side and weather do nothing under ORBIT and should go to Archive outright (findings 2–4). Also
needed before building: the picture mapping and Looks rules (5–6), a single cross-tab navigation
rule with deep links and hoisted state (8, 9, 16), a stronger Apps tab and one Cars list (13, 15),
and content rules that have numbers and a test behind them (21). With those changes it is a
good, durable plan.
