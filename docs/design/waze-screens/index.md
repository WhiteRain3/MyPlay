# Waze screens and MyPlay's worlds in ORBIT: look C, the orbit wheel

Designs for every Waze screen the owner meets in the Audi, and for MyPlay's own Settings, notices, calls and apps, in one ORBIT look at the MMI's 1280 × 720. Waze is a car-app-library app: it sends templates, and MyPlay's host (`host/templates`) draws everything except Waze's map surface.

**Chosen: look C, the orbit wheel**, with the alert countdown ring, the letter-ring keyboard and the lane guidance. While navigating there are no map buttons and no action strip on screen. Waze's actions are one press away, in a wheel. Screens 07–34 are all drawn in C.

![All screens](overview.png)

**Sample data only.** Places, times, labels and icons are made up; Waze chooses its own words and icons. The map is a stand-in street map, not Waze's, and the app icons are made-up coloured discs, not real logos. Each screen uses only fields its template or data class carries (listed below). Words that are not Waze's are MyPlay's own: the knob hints, "N more", "Typing works when you're parked", "Move map".

## The orbit wheel

- **One wheel for every list.** Rows sit on an arc round a centre off the left of the screen, the same way ORBIT's dial turns. The focused row is always in the middle and biggest, with one row either side, smaller and faded. The knob turns the wheel one row per detent, so the focus never travels down the screen.
- **Where you are.** A thin arc left of the rows, with an accent dot at the focused row's place in the whole list. "N more" above and below say how far it goes either way.
- **Focus.** ORBIT's ring: a 5 px halo gap, 7 px of accent and a soft halo. The primary action holds the focus when a screen opens: Go now, the first place, the first result, the first setting, an alert's first action, a message's primary action, or Answer. A banner nobody asked for opens on Dismiss. With nothing to act on, Back is focused.
- **Free area** (MC-1): content stays inside x 288–992, y 43–677. ORBIT's corners stay where they are: the turn and lanes top left, the clock top right, the hub with the trip ring bottom left, and the song bottom right. While navigating, a speed disc sits between the turn and the hub.
- **Glanceable.** The middle row's title is 32 px extra bold and other rows are 27 px. ORBIT's night and day glass keeps the accent at least 7:1 by night and 4.9:1 by day.

## Navigating: only what's needed, actions on one press

- On screen while navigating: the turn disc with distance and road, the **lanes** (Waze's `Step.lanes`, the lanes to take in Navigate's green), the arrival in the hub's trip ring, the speed, and any alert, banner or call.
- Not on screen: Waze's `actionStrip` and `mapActionStrip` (zoom, pan, report, mute, end route and so on). **Back or the pad's centre** opens them as a wheel, the action strip first and the map actions after. A push runs an action and closes the wheel; Back closes it. Action.PAN is labelled "Move map" (ours) and opens pan mode, where the d-pad moves the map, turning zooms and Back finishes. Other actions show Waze's `title`; one with only an icon shows its icon alone.

## The three looks (C chosen)

| PNG | Look |
|---|---|
| [01-option-a-column-list](01-option-a-column-list.png) | Option A · Side column — destinations: A glass column on the driver’s side of the free area (x 288–708); Waze’s map stays visible to its right. Rows are ORBIT pills, the focused row carries the focus ring. The first recommendation; not chosen. |
| [02-option-a-column-route](02-option-a-column-route.png) | Option A · Side column — route preview: Routes as radio rows; “Go now” is a full-width accent pill at the column’s foot and holds the focus when the screen opens. The routes stay visible on the map beside the column. |
| [03-option-b-stage-list](03-option-b-stage-list.png) | Option B · Centre stage — destinations: One wide glass panel fills the whole free area. The biggest type and the longest names, but the map is covered while a list is up. |
| [04-option-b-stage-route](04-option-b-stage-route.png) | Option B · Centre stage — route preview: Each route is an ORBIT ring with its time inside; the chosen one is lit. “Go now” sits under them, focused. Clear, but the routes on the map are hidden behind the panel. |
| [05-option-c-wheel-list](05-option-c-wheel-list.png) | Option C · Orbit wheel — destinations (chosen): The list turns like the dial: the focused place is always in the middle and biggest, one place either side, a position arc on the left. Chosen by the owner. |
| [06-option-c-wheel-route](06-option-c-wheel-route.png) | Option C · Orbit wheel — route preview (chosen): Routes on the arc; “Go now” is a big accent disc to their right and holds the focus. Chosen by the owner. |

A (side column) kept the routes visible beside the list. B (centre stage) had the biggest type. The owner chose C.

## Waze screens in look C

| PNG | Screen | Template | Fields drawn | Notes |
|---|---|---|---|---|
| [07-destinations](07-destinations.png) | Destinations (Waze’s first screen) | `MapWithContentTemplate › ListTemplate (or PlaceListNavigationTemplate)` | header: Action.APP_ICON + title · header actionStrip (search) · itemList rows: image, title, texts (address, DistanceSpan), browsable · onItemVisibilityChangedDelegate | Opens on the first place, in the middle of the wheel. Turning moves the next place into the middle; the arc on the left shows where in the list the focus is. Search sits top right of the free area (the d-pad’s up reaches it). |
| [08-destinations-turned](08-destinations-turned.png) | Destinations, turned further | `MapWithContentTemplate › ListTemplate` | itemList (as above) | Four turns on: the dot has moved down the arc, and “3 more” above / “2 more” below say how far the list goes either way. |
| [09-search-driving](09-search-driving.png) | Search while driving | `SearchTemplate` | headerAction (BACK) · searchHint · initialSearchText · itemList (rows: image, title, texts, DistanceSpan) · isLoading · showKeyboardByDefault (refused while driving) | Typing is refused while driving, so the search text Waze sent sits in a locked field where the title goes, and the focus is on the first result in the wheel. |
| [10-search-parked-keyboard](10-search-parked-keyboard.png) | Search while parked: the letter ring | `SearchTemplate (our keyboard)` | searchHint · the text we own (sendSearchTextChanged on each letter, sendSearchSubmitted on Search) · itemList (its size, as you type) | Parked, the knob types on a ring of letters that fills the free area: turn to a letter, push to type it. Space, delete and Search close the ring. The text and the number of places found so far sit in the middle; Search (or the d-pad down) opens the results wheel. |
| [11-search-no-results](11-search-no-results.png) | Search with no results | `SearchTemplate` | itemList empty → itemList.noItemsMessage (else our “No results”) | Where the wheel would be, one glass disc with Waze’s message for an empty list. The focus falls back to Back, so one push leaves. |
| [12-route-preview](12-route-preview.png) | Route preview | `RoutePreviewNavigationTemplate` | header / title + headerAction (BACK) · itemList rows (title = time, texts = via, distance) with onSelectedDelegate + selectedIndex · navigateAction (FLAG_PRIMARY) · panModeDelegate (not shown: no map buttons) | “Go now” holds the focus when the screen opens: one push starts the drive. The chosen route is lit in the middle of the wheel; turning the knob back moves the ring onto the routes. Waze draws the routes in the map’s free middle. |
| [13-route-preview-choosing](13-route-preview-choosing.png) | Route preview, turning to another route | `RoutePreviewNavigationTemplate` | itemList (onSelectedDelegate.sendSelected on push) · navigateAction | The second route turned into the middle with the focus ring; the radio dot still on the chosen one. A push picks it, Waze redraws, and the focus jumps back to “Go now”. |
| [14-route-preview-loading](14-route-preview-loading.png) | Route preview, loading | `RoutePreviewNavigationTemplate` | isLoading · header / title · navigateAction (not drawn until routes come) | Only ORBIT’s spinning ring in the wheel’s middle: Waze sends no words while loading, so we add none. Back is focused. |
| [15-navigating](15-navigating.png) | Navigating: the turn, lanes, arrival, speed — nothing else | `NavigationTemplate` | navigationInfo (RoutingInfo: current step, maneuver, distance, lanes → ORBIT’s turn disc and lane pill) · destinationTravelEstimate (→ the hub: arrival, time and distance left, remainingTimeColor) · actionStrip and mapActionStrip not drawn: they are in the actions wheel | The owner’s pick: no map buttons and no action strip on screen. The turn with its lanes (the lanes to take in Navigate’s green), the arrival in the hub’s trip ring, and the speed. Back or the pad’s centre opens Waze’s actions. |
| [16-navigating-actions](16-navigating-actions.png) | Waze’s actions, on one press | `NavigationTemplate (actionStrip + mapActionStrip, in a wheel)` | actionStrip actions then mapActionStrip actions: icon · title (when Waze sends one) · Action.PAN (→ “Move map”, ours) · each push sends its onClickDelegate | Back or the pad’s centre while navigating opens Waze’s action strip and map buttons as a wheel: the same arc as the lists, the first action in the middle and focused. A push runs it and closes the wheel; Back closes it. Labels are the actions’ titles; an action with only an icon shows its icon alone. |
| [17-navigating-pan](17-navigating-pan.png) | Moving the map (from the actions wheel) | `NavigationTemplate` | Action.PAN from the wheel · panModeDelegate.sendPanModeChanged · SurfaceCallback onScroll / onScale | Picking “Move map” in the wheel: the turn goes (as Android Auto), the d-pad moves the map, turning zooms, Back finishes. No buttons appear on the map. |
| [18-navigating-message](18-navigating-message.png) | A navigation message (arrived) | `NavigationTemplate (navigationInfo = MessageInfo)` | MessageInfo: title, text, image · destinationTravelEstimate (if any) | A MessageInfo takes the turn’s place: its image in the accent disc, its title and text floating beside it like the distance and road. |
| [19-navigating-rerouting](19-navigating-rerouting.png) | A navigation message (loading) | `NavigationTemplate (RoutingInfo.isLoading, or a MessageInfo with no image)` | RoutingInfo.isLoading · or MessageInfo title / text | While Waze works out the way, the turn disc becomes ORBIT’s spinning ring; any words it sends float beside it. |
| [20-alert](20-alert.png) | An alert while driving | `Alert (AppManager.showAlert)` | title · subtitle · icon · actions (first two) · durationMillis (→ the ring round the icon) · callbackDelegate (timeout / cancel) | ORBIT’s alert pill, between the hub and the song, clear of the turn and lanes. The alert’s time left runs as a ring round its icon, so the driver sees it will go by itself. The first action is focused; Back dismisses. |
| [21-message](21-message.png) | A message screen | `MessageTemplate (alone, or in MapWithContentTemplate)` | header / title + headerAction · icon · message · isLoading · actions (up to two; FLAG_PRIMARY filled) · debugMessage (never shown in the car) | A message is not a list, so no wheel: its icon in a ring, the words, and its buttons, the primary one focused. Back is the title’s. |
| [22-message-loading](22-message-loading.png) | A message screen, loading | `MessageTemplate (isLoading)` | isLoading · message (if any) | ORBIT’s ring spinner in place of the icon; no buttons, so Back is focused. |
| [23-route-preview-day](23-route-preview-day.png) | Route preview by day | `RoutePreviewNavigationTemplate` | as Route preview | ORBIT’s day glass and darkened accent (each at least 4.9:1); the same wheel. |
| [24-navigating-day](24-navigating-day.png) | Navigating by day | `NavigationTemplate` | as Navigating | The day glass over a day map; still only the turn, lanes, arrival and speed. |

## MyPlay's own worlds, in the same look

These are MyPlay's own screens, not templates. Their fields are the skin's data classes (`ui/car/myplay/MyPlaySkin.kt`, `ui/car/CarModel.kt`), and the setting titles are the real ones from `EngineSettings.kt`. Lists use the same wheel; banners and calls over the map use the alert's pill and buttons; the Apps grid becomes a ring like ORBIT's dial.

### Settings

| PNG | Screen | Drawn by | Fields drawn | Notes |
|---|---|---|---|---|
| [25-settings](25-settings.png) | Settings world | `MyPlay · SettingsWorld (SettingsPage)` | SettingRow: title · value (a choice’s label) · switch + on · note | The settings on the same wheel: a choice shows its value and a chevron (a push steps it on), a switch shows ORBIT’s accent switch. Opens on the first setting. |
| [26-settings-switch](26-settings-switch.png) | Settings world, on a switch with a note | `MyPlay · SettingsWorld (SettingsPage)` | SettingRow: title · switch + on · note | Turned to the end: the focus on a switch, a push flips it. The note (it can only be turned on while stopped) shows under the title in the middle row. |

### Notices and banners

| PNG | Screen | Drawn by | Fields drawn | Notes |
|---|---|---|---|---|
| [27-notice-message](27-notice-message.png) | A message banner while driving | `MyPlay · Notice (CarNotice, message = true)` | CarNotice: icon · title · text (hidden while driving for a message) · actions (labels) · asked = false → the ring on Dismiss | The alert’s glass and buttons, at the top of the free area, 500 px wide so it clears the turn’s distance and the clock. Nobody asked for it, so the ring is on Dismiss: a stray push never acts. A message’s text is hidden while driving. |
| [28-notice-asked](28-notice-asked.png) | A banner the driver asked for | `MyPlay · Notice (CarNotice, asked = true)` | CarNotice: icon · title · text · actions · asked = true → the ring on the first action | After “Call back” the driver expects to act: the ring is on the first button, so one push calls. |
| [29-notifications](29-notifications.png) | Notifications list | `MyPlay · NoticeList (NoticeListPage)` | NoticeRow: notice (icon, title) · text · time · action (the label a push runs) · a null notice = Clear all | The notices on the wheel: the app’s icon, the title, the text, the time at the end and, when there is one, the action a push runs as a small pill. Clear all is the wheel’s last row. |

### Calls

| PNG | Screen | Drawn by | Fields drawn | Notes |
|---|---|---|---|---|
| [30-call-ringing](30-call-ringing.png) | A call ringing | `MyPlay · Ringing (CarCall, ringing = true)` | CarCall: name · ringing · canEnd → Answer / Decline (CALL_ANSWER, CALL_DECLINE) | Centred in the free area: the caller’s initials in Calls’ green ring, the name, and two big discs. Answer holds the focus; Decline is a turn away. While navigating, the turn, lanes and hub stay. |
| [31-call-in-call](31-call-in-call.png) | In a call | `MyPlay · InCall (CarCall, ringing = false)` | CarCall: name · sinceMs (the time) · canEnd → the red hang-up disc | The call shrinks to a pill at the bottom of the free area, like the alert, so the turn and lanes come back. The ring is not on it until the driver turns there; hang-up is red and one turn on. |
| [32-calls-world](32-calls-world.png) | Calls world | `MyPlay · PhoneWorld (PhonePage)` | PhoneRow: title · subtitle · kind (RECENT, MISSED, FAVOURITE, PHONE_APP — last) · loading · permitted | Recent calls, then favourites, then the phone app, on the wheel. A missed call is in the warning red; the first row is in the middle, so one push calls back. |

### Apps

| PNG | Screen | Drawn by | Fields drawn | Notes |
|---|---|---|---|---|
| [33-apps-ring](33-apps-ring.png) | Apps: a ring of apps | `MyPlay · AppsWorld (CarAppEntry list, page / pages)` | CarAppEntry: label · icon · kind · ringed tile · page / pages | The grid becomes ORBIT’s dial: eight apps round a ring, the focused one bigger with the focus ring, its name in the middle. Turning goes round the ring; past the last app the next page turns in (the dots). |
| [34-phone-apps](34-phone-apps.png) | Phone apps list | `MyPlay · AppsWorld, phone apps (CarAppEntry, kind PHONE_SCREEN / STANDALONE)` | CarAppEntry: label · icon · kind | The phone’s own apps that the owner allowed on the car, on the wheel: there can be many, and a name reads quicker than an icon. Each opens on the phone’s mirrored screen. (Confirmed by the owner: the same CarAppEntry list as the ring.) |

## The status column: four options (35-status-*)

The owner finds today's top-right column out of place in ORBIT. It shows the clock, the outside temperature (red with the snowflake at 4 °C or less) and the phone battery with its percentage. Four options, all on the C navigating home and all outside the free area. The arrival time stays in the hub.

| PNG | Option | Notes |
|---|---|---|
| [35-status-a-speed-ring](35-status-a-speed-ring.png) | Status option a · the speed ring | The speed disc grows a ring, like the hub and the song. The time is a badge set into the top of the ring, and the temperature with its ice sign a badge at the bottom. The battery is a thin arc on the ring’s right side that empties as the phone does, with no number until ≤ 20 % or charging. The top-right corner is map again. |
| [35-status-b-satellite](35-status-b-satellite.png) | Status option b · a satellite of the speed disc | A small upright glass pill sits on a faint orbit round the speed disc, outside the free area. From the top: the time (big), the temperature with the ice sign, and the battery as a tiny bar with its number. Everything still reads in one glance, in one place, but no longer in a corner of its own. |
| [35-status-c-song-ring](35-status-c-song-ring.png) | Status option c · the time on the song’s ring | The time is a badge set into the top of the song’s ring; the temperature and ice sign a badge on its left side. The battery is hidden until it matters: drawn here at 18 %, a red badge on the ring’s lower left. The top-right corner is map again, and the right side keeps one part, the song. |
| [35-status-d-corner-orb](35-status-d-corner-orb.png) | Status option d · a corner orb (mine) | My idea: the top-right corner becomes a planet rising out of the screen’s edge, a quarter disc of glass, so every corner is now round (turn, orb, hub, song). The time is big inside it. Its rim is the battery ring, emptying from the bottom like the trip ring fills. The temperature and ice sign sit on the rim’s low end. It stays right of the free area (x ≥ 1010). |

The data is the same in every option: clock (phone time) · outside temperature (car, when reported) + ice sign at ≤ 4 °C · phone battery level + charging (when the phone tells it) · arrival stays in the hub (TravelEstimate).

**The owner's pick: (c), the time on the song's ring**, drawn as in [35-status-c-song-ring](35-status-c-song-ring.png). (My earlier recommendation was (a).) The owner asked to see one variant, c2, with the time over the album art:

| PNG | Option | Notes |
|---|---|---|
| [35-status-c2-scrim](35-status-c2-scrim.png) | Status c2 · the time over the art, on a soft scrim | The time is centred on the album art, white and extra bold, over a soft dark scrim that fades out before the disc’s edge, so the art still shows round it. Drawn on a pale, busy cover, the hard case. The temperature badge stays on the ring’s left; the battery appears only when low. |
| [35-status-c2-badge](35-status-c2-badge.png) | Status c2 · the time over the art, on a glass badge | The same place, but the time sits on a small pill of ORBIT’s glass in the middle of the art: legible on any cover, at the cost of hiding a strip of it. Same pale cover. |
| [35-status-c-nomusic](35-status-c-nomusic.png) | Status · no music (for c and c2) | Nothing playing: the music circle stays where it is, empty glass with just its track ring, and holds the time, big and centred. With c2 the time does not move at all when music stops or starts; with c it drops from the ring’s top badge into the middle. The temperature badge keeps its place. |

- **Of the two c2 variants I recommend the glass badge.** It stays legible on any cover, even a white one. The scrim shows more of the art but goes grey on pale covers.
- **No music:** the music circle keeps its place as empty glass and holds just the time, big and centred. With c2 the time never moves when music starts or stops. With (c) it drops from the badge on the ring's top into the middle.
- **Fields:** the time, the outside temperature with the ice sign, and the battery (shown only when low), from the status data, plus the song (SongSlot). The arrival time stays in the hub.

Labels are straight glass badges set into the rings, not text bent along them: on a ring this small, bent text was hard to read.

## Placement, in the car's pixels

| Part | Where | Size |
|---|---|---|
| Wheel title (+ Back, app or world icon) | x 300, y 46 | title 34 px; discs 64 |
| Wheel rows | arc centred at (−322, cy), radius 640, 17° per row; cy 370 (400 under a search field) | middle row 112 high, 460–600 wide; others 84 high, 90 narrower, 72 % opacity |
| Position arc | radius 622 on the same centre | 5 px track, 8 px accent dot |
| Go now (navigateAction) | x 800–990, centred on the wheel | 190 disc, accent fill |
| Search field (SearchTemplate) | in the title's place | 420 × 64 |
| Letter ring (parked search) | centred in the free area | 620 ring, letters on radius 252 |
| Apps ring | centred in the free area | 600 ring, 8 apps on radius 220 (96, focused 120) |
| Speed (navigating) | x 76–196, y 340–460 | 120 disc |
| Alert | bottom of the free area, x 300–920, y 556 | pill; icon in an 84 ring that counts down `durationMillis` |
| Notice banner | top of the free area, x 470–970 | 500 wide, clear of the turn's distance and the clock |
| Ringing call | centred in the free area | caller ring 200; Answer / Decline discs 112 |
| In-call pill | bottom of the free area | 76 high + red hang-up disc 76 |

## What the host would change

- A shared `OrbitWheel` composable (rows, position arc, "N more") used by `ItemListView` in map templates and search, and by ORBIT's SettingsWorld, NoticeList, PhoneWorld and phone apps list. Knob turns move the wheel's focus; `onItemVisibilityChangedDelegate` gets the three rows shown.
- `MapScaffold` under ORBIT: draw neither `actionStrip` nor `mapActionStrip`. Back or the pad's centre while navigating opens them in the wheel. Action.PAN goes into pan mode as today (`MapMoveMode`). The visible area told to Waze no longer loses the strips' space.
- `RoutePreviewNavigationTemplateView`: the Go now disc (`navigateAction`) is focused at the start; after `onSelectedDelegate` the focus returns to it.
- `SearchTemplateView`: while driving, the locked field with focus on the first result; parked, the letter ring replaces `CarKeyboard`, using the same `KeyboardSink` (type, backspace, submit, hide).
- ORBIT's `MapAlertCard`: a countdown ring from `durationMillis`.
- `NavigationTemplateView`: `MessageInfo` and `RoutingInfo.isLoading` in ORBIT's turn disc. Lanes stay as today (`LanesToSkin`).
- ORBIT's `AppsWorld`: the ring of eight with the name in the middle, pages as dots.

## Open questions for the owner

- The letter ring: is turning through 29 keys quick enough, or should the d-pad jump a quarter of the ring?
- The actions wheel: is the pad's centre free while navigating, or should only Back open it?

## Rebuilding

`node docs/design/waze-screens/src/build.cjs` renders every screen with Playwright at 1280 × 720, 1×: the HTML goes in `html/` and the PNGs and `overview.png` here. Parts are in `src/parts.cjs` (ORBIT's tokens from `OrbitTokens.kt`); screens and sample data are in `src/screens.cjs`. Icons are the phone-app design's (`src/icons.json`, Lucide, ISC licence). The font is `../phone-app/src/fonts/RobotoFlex`.
