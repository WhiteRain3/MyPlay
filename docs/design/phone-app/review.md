# Review of the phone app screen designs

Independent review, 2026-11-22, of `docs/design/phone-app/` at commit 7b9eb70 (31 PNGs, index.md,
sources). Checked against `docs/plans/phone-app.md` revision 3, my earlier plan review
(`docs/plans/phone-app-review.md`) and the code. Designs only; no app code changed.

Severity: **blocker** = would ship something wrong or unsafe; **major** = fix before the build
starts; **minor** = fix while building.

---

## What works

- **The overall language is right.** Calm near-black glass, a lot of space, one card style, and
  rings that carry state (connected, waiting, progress, error) make it a believable sibling of ORBIT
  without copying it. The Home hero (01, 02, 29) answers "is it working?" at a glance.
- **The IA is followed.** There are five tabs. Every root lists all its pages with values (04, 10,
  19). Apps *is* the list (16). Updates has one home (20) and a badge on More. The Map colours
  cross-tab link (15) is there. Developer links to the protocol log (24). Archive works as decided
  (25).
- **The text is short.** The build enforces the 30- and 60-character limits, and it shows: no
  `HintLine` stacks anywhere.
- **Good state work.** Not connected, with one reason and one action (02). Error, with Try again
  and What happened (29). Empty (30). Permission asked in context (28). The Looks apply sheet that
  warns about DECK (06) is exactly review finding 6.
- Reorder has up/down buttons besides drag (17). Touch targets are 48 dp. The motion notes include
  predictive back and "Remove animations".

---

## Findings

### Settings drawn with the wrong values or controls (these change what is stored)

**1. Blocker: two settings are drawn as switches but are multi-value.**
- *Go home when idle* (15) is `myplay_maps_idle_home`, an Int: 0 = off, or 1, 2, 5 or 10 minutes.
- *Highest resolution* (23) is `max_video_height`, an Int: 480–1440, default 1080.

A switch can't store these values, and building the screens as drawn would break plan rule 6
("storage never changes").
*Fix:* make both rows open a choice, with the value shown on the right ("2 min", "1080p").

**2. Major: the option lists don't match the code.**
- **Themes** (07): the code's themes are AARE, CarPlay, Android Auto, Android Auto 2019, High
  contrast, Material You, Night and Retro. "Slate, Graphite, Ocean, Plum, Forest" don't exist.
- **Accents** (07): the design has no "Theme's own" or "Phone's" option. The code has `theme`
  (the default) and `phone`, then amber, red, green, cyan, blue and white.
- **Built-in looks** (05): the code has MyPlay, Midnight, Forest, Ocean and High contrast. "Ember",
  "Glacier" and "Moss" don't exist.
- **Size** (08): text size and control size are each Default / Larger / Largest. There is no
  "Small". "How much fits" is Auto or a fixed dpi, so Auto can't sit in the middle of a slider.
- **Knob turn in phone apps** (18): the code has *Up and down* or *Left and right*. There is no
  "scroll" or "volume".
- **Developer sub-switches** (24): the names are made up. The real four are: *Accept from the
  network*, *Turn off network mode after a session*, *Phone screen on the server* and *Allow localhost* (shorten each
  to the 30-character limit, but keep what it means).

*Fix:* draw the real choices. Use segmented controls for the three-step sizes. For density, use a
segmented "Auto / Fixed", with the fixed value opening a list. Invented sample data is fine for
cars, regions and apps, not for the setting choices themselves.

**3. Major: the Buttons page drops existing features and adds a missing one (open point 2).**
- *Hold + turn* is missing; it exists in the code (`Gesture.HOLD_TURN`, default "skip songs").
- *Triple press* doesn't exist in the code.
- The *wheel* controls are missing from the drawing, though the plan lists them.
- *Show what a button did* (`myplay_gesture_line`) and *Reset this page* are missing; the plan
  requires both.

*Fix:* draw press, hold, double and hold + turn. Add the wheel to the drawing. Add those two rows.
Leave triple press out (see open point 2).

**4. Major: Pad shortcuts is missing a setting and uses a different name.**
"Info-only home" (`myplay_info_home`) belongs on this page (plan §3.2) but isn't drawn. The plan
calls the page *Home shortcuts* (route `car/buttons/home-shortcuts`); the design calls it *Pad
shortcuts*. *Fix:* add the switch at the top ("Hide the ring on home"; the pad keys apply only
while it's on). Pick one name and use it in the plan and the routes too. I suggest "Home
shortcuts", because it says where they work.

**5. Major: the permission state dims the wrong rows (28).**
Banners, Group chats, Read aloud and Do not disturb depend on **notification access**, which is a
separate system screen, not on contacts and calls. Calls on phone speaker depends on the call
permission but isn't dimmed. *Fix:* draw two separate inline cards, each dimming only the rows that
depend on it: "Allow notification access" (banners, groups, read aloud, auto-reply) and "Allow
calls and contacts" (names, auto-reply to calls). Do not disturb has its own policy-access row,
which appears when the switch is turned on.

### Colour system

**6. Major: amber means four different things.**
`caution` (#FFC857) and the Car accent (#FFC24A) are 1.04:1 apart, so they look the same. Amber is
used for:
- the Car tab's identity, switches and primary buttons (10–15, 28);
- the "needs attention" badge and dot (03, 19);
- the "Next connection" tag (08, 23, 28);
- the "changed from default" dots on Car pages (10, 11).

On the Car tab, a "changed" dot and an "attention" badge can't be told apart. On 28 the Allow
button, the switches and the tag are all the same amber.
*Fix:* keep the five world colours for **identity only**: the selected tab ring, the root title
glyph and the hero ring. Use **one neutral interaction accent** for switches, sliders and primary
buttons across all tabs. Give caution a hue no tab uses, or make "Next connection" a neutral tag
with an icon. A design meant to last shouldn't change its switch colour five times.

**7. Major: the "changed" dot contradicts the plan.**
Plan §4 says "dots mean 'needs attention' only". The design uses a dot in the accent colour for
"changed from default" (04, 10, 11, 14). Combined with finding 6, a user sees dots everywhere and
can't tell which ones matter. *Fix:* show "changed" as text style (the value in the accent, or a
medium weight), not a dot. Or use a small hollow ring, which reads as a mark rather than an alert.
Keep filled dots for attention only. Then update the style sheet's "The dot = changed" line.

**8. Major: contrast checks miss non-text elements and dimmed rows.**
- The off-switch track (#2A3540) is 1.4:1 on surface and 1.5:1 on bg. WCAG 1.4.11 asks for 3:1
  for the parts of a control that show its state, so an off switch is barely visible (14, 15,
  18, 23, 24).
- `.row.dim` uses opacity 0.42 (28). That brings label text to 3.7:1 and help text to 2.1:1, and
  dimmed switches that are *on* still read as active.

*Fix:* give the off switch a 1.5 dp outline in `text3` (≥ 3:1). Add non-text contrast (≥ 3:1)
to `stylesheet.cjs`: switch track or outline, slider track, dashed rings, the focus ring. For
"needs permission", instead of fading the rows, keep them readable, disable the controls, and show
one reason in the card above.

### The car pictures and previews

**9. Major: the Colours preview shows the part of the screen those colours don't change.**
Review finding 1 (now in the plan) established that theme, accent and your own colours change
**app screens inside ORBIT**, not ORBIT's own rings. Page 07 previews the ORBIT home, so changing
a theme would show no change. *Fix:* on Colours, preview an app screen (a list template or now
playing, drawn by the real car code). Label it "App screens", the same as the section.

**10. Major: the picture labels and their targets surprise the user (04).**
Under the plan's mapping, tapping the big speed dial (the ORBIT hub) opens **Design**, and the
small sign opens Speed. A user who taps the speedometer expects Speed. The dashed purple rings and
four chips also clutter the one picture that should look like the car. *Fix:*
- Map the hub's speed area to Car › Speed, and give Design its own way in: the root's first row
  is enough. Update plan §3.5 to match.
- Show the part rings only while the picture is pressed, or after the first visit. Keep one hint
  line.
- The Wallpaper row says "Photo", but the picture shows none. In the build the picture must be the
  live render from the real car code (`CarPicture`), never a drawing (see open point 8).

**11. Minor: the Home tab's "connected" signal doesn't show (open point 1).**
When you look at the status, Home is selected, so its icon sits in a filled ring whether connected
or not (01, 02, 29 look the same). The outline/filled difference is about 2 px at tab size. See my
recommendation on open point 1.

### Missing screens and states

**12. Major: screens and states needed before building are missing.**

*Security and trust:*
- **Trust needed** on Home: "New car — Trust?", with the fingerprint in ⓘ. This is the one
  security decision in the app.
- **Developer warning lines** (plan rule 6). The head unit server opens an unauthenticated port.
  Page 24 shows only "For testing with a computer".

*The hardest page:*
- **Car › Sound**: the method in use, setup for Shizuku, root and screen capture, and "Play music
  when connected". It has the most states in the app and isn't drawn.

*Pages in the plan that aren't drawn:*
- Wallpaper and the photo editor.
- Map colours (preset, sliders, before/after).
- Your own colours (night/day per part).
- Messages and calls in its normal state.
- Voice, and Timing.
- Cars with entries, and a car's own page (Forget, undo).
- Connection, and Backup (including the load confirm and the after-loading list).
- Diagnostics, Car data, Connection log.
- Certificates (import, PKCS#12 password, download).
- Archive › Older layouts.
- What's new, and search results ("Turn on X first").
- Speed's normal state (file date and size, Look for newer, Delete), not only the download.

*States:*
- Connecting.
- Install in progress, failed, and "allow unknown apps".
- Up to date.
- Offline.
- Setup steps 2–4: Sound, the update question, Connect.
- The "Using DECK" card.
- The undo snackbar in use.
- Teach and Car check while not connected ("Connect first").
- Phone apps without root or Shizuku.

*Layout:*
- 200 % font size on one dense page (10 or 16).
- One large-screen frame (rail plus list-detail), because plan §11 commits to it.

*Fix:* do these as a second batch, Trust, Sound and Developer first.

**13. Minor: risky switches carry no warning style.**
Passenger mode (10), Show phone apps (18) and Head unit server (24) use a plain grey help line.
The plan asks for one warning line in the warning style. *Fix:* use the `warn` colour with a small
icon, and put the reason in ⓘ.

**14. Minor: smaller inconsistencies.**
- On 14 and 16, the highlighted cards have a **green** border on Car and Apps pages. Use the tab's
  own colour, or none.
- On 03, Home drops its hero ring for a compact card when an update is shown, so the status jumps
  between two layouts. *Fix:* keep one status layout. Put the update card *below* the hero, or
  shrink the hero in every state.
- The Speed, Buttons and Car tab icons are all gauges (10, 11). Give each a distinct icon: Speed =
  a speed-limit sign, Buttons = the knob.
- The Reorder entry (16) is an icon-only list glyph. Make it a text button "Reorder", or give it a
  TalkBack label and a tooltip.
- Looks (05): there is no way to delete a saved look, and nothing says what Share shares. *Fix:*
  add an overflow menu on each saved look (Rename, Share, Delete with undo). Drop the page-level
  Share.
- Looks root value: the Screen root says "Midnight" while Colours shows "Slate + own". Once a look
  is modified, the value should read "Midnight (changed)" or "Not saved" (plan §9.3).
- Teach (13): taught buttons have no Forget. Add it to the row's page or as a swipe with undo.
- Inter is a bundled font (APK size, a second licence). Consider the system font (Roboto Flex,
  which the platform supplies) unless the owner wants Inter specifically. Rendering on the phone
  will differ from these PNGs either way.

---

## The designer's open points: recommendations

1. **Two car icons.** Make **Home a house and Car a car**, and drop the tab-level "connected" cue.
   The status is the first thing on Home, and the system notification already shows the
   connection, so the cue adds little and costs an icon. If the owner wants a cue anyway, use a
   thin green arc under the Home icon *only while another tab is selected*. Record this as a small
   plan change to §4.
2. **Triple press.** **Drop it.** It isn't in the gesture engine, it adds a third timing window
   (slower double press), and nobody asked for it in the plan. Put back **hold + turn**, which
   exists (finding 3). No "New" tags for features that don't exist.
3. **Merging the four pad steps in the car check.** **Agree.** It is faster and the results read
   better ("Left: nothing"). It is a code change, though: the `CheckStep` list and the shared log
   change. Keep one result line per button in the log so old and new logs compare. Raise it as a
   build task.
4. **"Include alert texts."** **Drop the switch.** `CarCheckLog` already hides navigation text and
   keeps only the speeds, which is what a Waze limit check needs. A switch that removes that
   protection on a share sheet is easy to turn on by mistake, and the plan never asked for it.
   "See what is shared" is enough. If the owner ever needs the full text, add it as a Developer
   switch.
5. **Live preview placement.** **Agree for Size, change Colours** (finding 9: preview an app
   screen). Add the preview to **Wallpaper** and **Map colours** too (the before/after preview
   *is* that page). Make it sticky at the top while the page scrolls, so the result stays in view.
6. **The error detail as a row.** **Agree.** Update plan §10: the hero states use a "What happened"
   row; the compact status card (03) keeps ⓘ.
7. **The Apps list is cut.** **Add a scrolled frame**, or a tall capture, showing Phone,
   Notifications, a hidden app and "Apps that can't run" (collapsed). Hidden apps should stay in
   Reorder, greyed out, so their place is kept.
8. **ORBIT reference pictures.** **This blocks sign-off of the pictures only.** In the build the
   picture is the **live render** from the car's own code (`CarPicture`), so the drawing is only a
   placeholder. Say so in index.md. Have the owner check the drawn layout against the ORBIT sheet
   before marketing screenshots are made from these PNGs.

---

## Verdict

The visual language is strong and fits the brief: calm, round, sparing with text. It follows the
IA closely. It is **not ready to build from yet**:
- Two controls would change what is stored (finding 1, blocker).
- Several option lists are invented rather than taken from the code (2–4).
- The permission state dims the wrong rows (5).
- One amber means four things, and the "changed" dot clashes with the attention dot (6–7).
- Switch and dimmed-row contrast fail WCAG's non-text rule (8).
- The Colours preview shows the wrong surface (9).

Several key screens are also missing, Trust, Sound and Developer warnings first (12). The second
pass should fix those, use one neutral interaction accent with world colours for identity only,
and add the missing batch. After that, this can be a polished design that will last.
