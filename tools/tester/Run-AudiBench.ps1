<#
.SYNOPSIS
  MyPlay test bench: runs the desktop test head unit as an Audi-like screen against a phone,
  plays a named knob walk, then collects the pictures and the phone's log into one folder.

.DESCRIPTION
  For the Tester's Windows PC. Needs, on the PATH: adb, java (17 or newer) and ffmpeg.
  The phone must be plugged in with USB debugging on, with MyPlay installed.

  On the phone, by hand, before the first run:
    - MyPlay > Advanced > Head unit server: on (release builds cannot be switched on from here);
    - on the first connection, tap Trust on the phone.

  What it does:
    1. adb forward tcp:5277 localabstract:aare_hu
    2. starts aare-headunit.bat with the Audi profile, --headless <folder> and --script "<events>"
    3. waits, takes a phone screenshot, then stops the head unit and its Java process
    4. saves the phone's log lines written by MyPlay's own process, and the system's lines
       about that process starting, dying or crashing
  Result: <OutRoot>\<version>\<walk>\  with screen-000.png ..., latest.png, phone.png,
  phone-log.txt, headunit-out.txt, headunit-err.txt and summary.txt. All text files are UTF-8.
  <OutRoot> is, unless you give -OutRoot, a folder "myplay-bench" next to the head unit folder
  (for C:\myplay\aare-headunit that is C:\myplay\myplay-bench), so not inside the checkout.

  Windows does not run downloaded scripts by default, so start it like this. -NoProfile keeps
  your own PowerShell start-up file out of it (its output got mixed into the results):
    powershell -NoProfile -ExecutionPolicy Bypass -File .\Run-AudiBench.ps1 -HeadUnitDir C:\myplay\aare-headunit -Walk home

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .\Run-AudiBench.ps1 -HeadUnitDir C:\myplay\aare-headunit -Walk home

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .\Run-AudiBench.ps1 -ListWalks
#>
[CmdletBinding()]
param(
    # The unpacked "aare-headunit" folder from myplay-headunit-<version>.zip (it holds bin\aare-headunit.bat).
    # Its path must not contain a space.
    [string]$HeadUnitDir,
    # Which walk to play; see $Walks below or -ListWalks.
    [string]$Walk = 'home',
    # Name of the result folder. Default: MyPlay's version as the phone reports it.
    [string]$Version,
    # How long to let the head unit run, in seconds. Default: the walk's last event plus 18
    # (10 for connecting, 8 after the last event).
    [int]$Seconds = 0,
    # Where result folders go. Default: a folder "myplay-bench" next to the head unit folder.
    [string]$OutRoot,
    # For the walks settings, maps, map-move, music, back-out and waze-check: the place of the app to open in the app grid,
    # counted from 1 in the order the ring moves through the grid (see the grid-end pictures).
    [int]$Tile = 1,
    # For the walk map-move: how many clicks forward from where the maps app starts the ring reach
    # the map itself (its last stop, after the app's buttons; count them on the maps walk's pictures).
    [int]$MapClicks = 12,
    # Leave out the margin option of the profile.
    [switch]$NoMargins,
    # Only print the walks and stop.
    [switch]$ListWalks
)

$ErrorActionPreference = 'Stop'

# Text as UTF-8 everywhere: what adb prints is read as UTF-8, and Java (the head unit) is told to
# write UTF-8. Without this, letters outside plain English came out garbled in headunit-out.txt.
$Utf8 = New-Object System.Text.UTF8Encoding($false)
try { [Console]::OutputEncoding = $Utf8 } catch { }
$OutputEncoding = $Utf8
$env:JAVA_OPTS = ("$env:JAVA_OPTS -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8").Trim()

# ----------------------------------------------------------------------------------------------
# The Audi profile. Change it here, in one place.
#
# Known from the owner: the car's screen takes 1280x720 video at 213 dpi, has no touch, only a knob.
# NOT known yet: what margins the owner's box reports. Google's app is said to use 1200 of the 1280
# pixels, with about 80 unused on the right, so that is what $AudiMargins imitates for now. Correct
# it after the parked test, from what the box really reported. Other forms the head unit accepts:
#   @('--width-margin', '80')        80 pixels, split by the phone into 40 left and 40 right
#   @()                              no margins
# ----------------------------------------------------------------------------------------------
$AudiProfile = @('--res', '1280x720', '--dpi', '213', '--no-touch', '--route', 'none')
$AudiMargins = @('--margins', '0,0,80,0')   # left,top,right,bottom

# ----------------------------------------------------------------------------------------------
# The walks: name = events. Each event is what@seconds, counted from the moment the phone connects.
#   rotate:N (knob clicks, negative = back)   dpad:up|down|left|right|center (center = press the knob)
#   key:CODE (4 Back, 3 Home, 66 Enter)       park   drive   night   day   reverse:SECS   tap:X,Y
#   hold:CODE,MS (held MS ms)   double:CODE (two quick presses)   holdturn:CODE,N (held while N knob clicks)
#   (codes: 23 knob push, 85 wheel click / play-pause, 82 Menu, 4 Back; the car's real codes come with R8)
# A picture is saved at most every 3 seconds, and only when a new video frame arrives, so leave at
# least 3 seconds between events.
# Add your own walks here.
# ----------------------------------------------------------------------------------------------
if ($Tile -lt 1) { $Tile = 1 }

# Opens the app grid from the home screen with the knob alone. It assumes what the code says and
# the first bench round saw, but nobody has checked the count: the first click puts the ring on
# the first thing of the home screen; two clicks back from there is the bar's Apps button (one
# click back is the voice button). If the pictures show otherwise, correct the -2 here.
$OpenGrid = 'park@2 rotate:1@5 rotate:-2@8 dpad:center@11'

# From the open grid: move to tile number $Tile and press. Ready at 17 seconds.
$ToTile = $(if ($Tile -gt 1) { "rotate:$($Tile - 1)@14 " } else { '' }) + 'dpad:center@17'

# N single clicks forward, 3 seconds apart, the first at $From seconds.
function Get-Turns([int]$From, [int]$Count) {
    $list = @()
    for ($i = 0; $i -lt $Count; $i++) { $list += "rotate:1@$($From + 3 * $i)" }
    return ($list -join ' ')
}

# N single clicks back, 3 seconds apart, the first at $From seconds.
function Get-BackTurns([int]$From, [int]$Count) {
    $list = @()
    for ($i = 0; $i -lt $Count; $i++) { $list += "rotate:-1@$($From + 3 * $i)" }
    return ($list -join ' ')
}

$Walks = [ordered]@{
    # Parked, then 12 single clicks forward: the ring should move to each element of the home screen.
    'home'       = 'park@2 ' + (Get-Turns 5 12)
    # One click, press the knob to open what is ringed, wait, then Back.
    'open-back'  = 'park@2 rotate:1@5 dpad:center@8 key:4@17'
    # Driving, wait, parked again: what is restricted while moving should hide and come back.
    'drive-park' = 'park@2 drive@5 park@14'
    # Night colours, then day colours.
    'night-day'  = 'park@2 night@5 day@11'
    # Open the app grid, then 30 single clicks forward: the ring should stay fully on screen down
    # to the grid's last tile, then go to the bar. The pictures also give each app's -Tile number.
    'grid-end'   = "$OpenGrid " + (Get-Turns 14 30)
    # Open the app grid, open tile -Tile (give the place of Settings), then 20 single clicks
    # forward: the ring should start on the first option and reach every option in order.
    'settings'   = "$OpenGrid $ToTile " + (Get-Turns 23 20)
    # The same for a maps app (give the place of Organic Maps as -Tile): the ring should start
    # inside the app and reach its buttons.
    'maps'       = "$OpenGrid $ToTile " + (Get-Turns 23 20)
    # Each button gesture once, with music playing (start a song first), plan R2, R3, R6. A line
    # should show at the top for each one that acts away from the ring:
    # knob held: next song; wheel double click: previous song; wheel click: pause, then play;
    # knob held and turned two clicks: two songs on; Menu held: the assistant (Back closes it);
    # Back held: home.
    'gestures'   = 'park@2 hold:23,900@5 double:85@10 key:85@15 key:85@19 holdturn:23,2@23 hold:82,900@29 key:4@35 hold:4,900@39'
    # The pad like a TV remote on the home screen (plan R4): one click to put the ring on the
    # screen, then right, right, down, down, left, up, up, left. The ring should move that way
    # each time, from the bar to the cards and back, and never get stuck.
    'pad'        = 'park@2 rotate:1@5 dpad:right@8 dpad:right@11 dpad:down@14 dpad:down@17 dpad:left@20 dpad:up@23 dpad:up@26 dpad:left@29'
    # The music browser (give the place of a music app as -Tile; one with folders, such as the
    # phone's own music player or a podcast app): 15 clicks forward through its top level, press
    # the knob to open what is ringed, 10 clicks through that level, 10 clicks back, then Back.
    # The ring should reach every row and button, stay fully on screen, and Back should go up one
    # level (plan R4-2: the music browser on the bench, as the unit tests cannot host a media app).
    # Moving a hosted map with the knob (plan R5, R5-8; give the place of Organic Maps or Waze as
    # -Tile and the clicks to the map as -MapClicks): push on the map, the pad right twice and down
    # once (the map should show more of the right, then of below), two turns forward (zoom in) and
    # one back (zoom out), a push to stop, then Back. The line "Moving the map" should show between
    # the two pushes.
    'map-move'   = "$OpenGrid $ToTile " + (Get-Turns 23 $MapClicks) + " dpad:center@$(26 + 3 * $MapClicks) dpad:right@$(29 + 3 * $MapClicks) dpad:right@$(32 + 3 * $MapClicks) dpad:down@$(35 + 3 * $MapClicks) rotate:1@$(38 + 3 * $MapClicks) rotate:1@$(41 + 3 * $MapClicks) rotate:-1@$(44 + 3 * $MapClicks) dpad:center@$(47 + 3 * $MapClicks) key:4@$(50 + 3 * $MapClicks)"
    'music'      = "$OpenGrid $ToTile " + (Get-Turns 23 15) + ' dpad:center@68 ' + (Get-Turns 71 10) + ' ' + (Get-BackTurns 101 10) + ' key:4@131'
    # The Waze check (plan N3; give the place of Waze as -Tile and a long -Seconds, such as 600,
    # while driving a route or with Waze's own demo drive): opens Waze and leaves it, so the head
    # unit saves a picture every 3 seconds. With MyPlay's "maps app text" check switched on
    # (setting myplay_debug_nav_text), phone-log.txt gets every text Waze sends ("nav text" lines,
    # marked "[looks like a speed]" when one does). Look in the pictures for: police and hazard
    # reports (as alerts at the top, or only on Waze's own map), Waze's own speed read-out and
    # speed limit sign (and where), and whether Waze keeps its icons out of MyPlay's corners.
    'waze-check' = "$OpenGrid $ToTile"
    # Open the app grid, open tile -Tile, then Back twice within a second: the home screen should
    # show, also for an app that ignores Back. (Holding Back cannot be played from here.)
    'back-out'   = "$OpenGrid $ToTile key:4@26 key:4@27"
}

$Package = 'io.github.whiterain3.myplay'

if ($ListWalks) {
    foreach ($name in $Walks.Keys) { Write-Host ("{0,-12} {1}" -f $name, $Walks[$name]) }
    Write-Host ''
    Write-Host "settings, maps, map-move, music, back-out and waze-check open tile number -Tile of the app grid (now $Tile)."
    Write-Host "map-move turns -MapClicks times (now $MapClicks) to reach the map before pushing."
    exit 0
}

function Stop-WithMessage([string]$Message) {
    Write-Host "ERROR: $Message" -ForegroundColor Red
    exit 1
}

# --- checks before anything is started ---------------------------------------------------------
if (-not $Walks.Contains($Walk)) { Stop-WithMessage "No walk named '$Walk'. Known walks: $($Walks.Keys -join ', ')" }
$Events = $Walks[$Walk]

if (-not $HeadUnitDir) { Stop-WithMessage 'Give -HeadUnitDir: the unpacked aare-headunit folder from myplay-headunit-<version>.zip' }
if ($HeadUnitDir.Contains(' ')) { Stop-WithMessage "The path '$HeadUnitDir' contains a space, which the head unit's start file cannot take. Move the aare-headunit folder to a path without spaces, for example C:\myplay\aare-headunit" }
$Bat = Join-Path (Join-Path $HeadUnitDir 'bin') 'aare-headunit.bat'
if (-not (Test-Path -LiteralPath $Bat)) { Stop-WithMessage "No head unit at $Bat" }

if (-not $OutRoot) {
    # Next to the head unit folder: outside the checkout, and a path without spaces.
    $OutRoot = Join-Path (Split-Path -Parent ((Resolve-Path -LiteralPath $HeadUnitDir).Path)) 'myplay-bench'
}

foreach ($tool in @('adb', 'java', 'ffmpeg')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { Stop-WithMessage "$tool was not found on the PATH" }
}

$state = (& adb get-state | Out-String).Trim()
if ($state -ne 'device') { Stop-WithMessage "adb sees no phone ready to use (adb get-state says: $state). Plug the phone in and allow USB debugging." }

$installed = (& adb shell pm path $Package | Out-String).Trim()
if (-not $installed.StartsWith('package:')) { Stop-WithMessage "MyPlay ($Package) is not installed on the phone" }

if (-not $Version) {
    $line = & adb shell dumpsys package $Package | Select-String -Pattern 'versionName=' | Select-Object -First 1
    if ($line) { $Version = ($line.ToString() -replace '.*versionName=', '').Trim() }
    if (-not $Version) { $Version = 'unknown-version' }
}

if ($Seconds -le 0) {
    $last = 0.0
    foreach ($ev in $Events.Split(' ')) {
        $at = 0.0
        $parts = $ev.Split('@')
        if ($parts.Length -eq 2 -and [double]::TryParse($parts[1], [System.Globalization.NumberStyles]::Float, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$at)) {
            if ($at -gt $last) { $last = $at }
        }
    }
    # 10 seconds for connecting, before the walk's clock starts, and 8 after the last event.
    $Seconds = [int][math]::Ceiling($last) + 10 + 8
}

$OutDir = Join-Path (Join-Path $OutRoot $Version) $Walk
if (Test-Path -LiteralPath $OutDir) {
    # Keep an earlier run: move it aside under its own time. If that cannot be done (a file in it
    # is open, or that name is taken), this run goes into a folder of its own instead.
    try {
        $stamp = (Get-Item -LiteralPath $OutDir).LastWriteTime.ToString('yyyyMMdd-HHmmss')
        Rename-Item -LiteralPath $OutDir -NewName "$Walk-$stamp" -ErrorAction Stop
    } catch {
        $OutDir = Join-Path (Join-Path $OutRoot $Version) ("$Walk-run-" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        Write-Host "The earlier result folder could not be renamed; this run goes to $OutDir"
    }
}
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$OutDir = (Resolve-Path -LiteralPath $OutDir).Path

# MyPlay's process numbers on the phone, for picking its lines out of the phone's log.
function Get-MyPlayPids {
    $text = (& adb shell pidof $Package | Out-String).Trim()
    if ($text) { return @($text -split '\s+') }
    return @()
}
$appPids = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($one in (Get-MyPlayPids)) { [void]$appPids.Add($one) }

# --- run ---------------------------------------------------------------------------------------
Write-Host "MyPlay $Version, walk '$Walk', $Seconds seconds. Results: $OutDir"

# The phone's clock, so that only this run's log lines are collected afterwards.
$phoneStart = (& adb shell "date +'%m-%d %H:%M:%S.000'" | Out-String).Trim()

& adb forward tcp:5277 localabstract:aare_hu | Out-Null
if ($LASTEXITCODE -ne 0) { Stop-WithMessage 'adb forward failed' }

$profileArgs = @() + $AudiProfile
if (-not $NoMargins) { $profileArgs += $AudiMargins }

# One command line; values with spaces are put in quotes.
$argLine = ($profileArgs -join ' ') + " --headless `"$OutDir`" --script `"$Events`""
$outFile = Join-Path $OutDir 'headunit-out.txt'
$errFile = Join-Path $OutDir 'headunit-err.txt'

$proc = Start-Process -FilePath $Bat -ArgumentList $argLine -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput $outFile -RedirectStandardError $errFile

$deadline = (Get-Date).AddSeconds($Seconds)
while ((Get-Date) -lt $deadline) {
    if ($proc.HasExited) { break }
    Start-Sleep -Seconds 1
}
$endedEarly = $proc.HasExited

# The phone's own screen, while the head unit is still connected.
# (Never "adb exec-out screencap > file" in PowerShell: it damages the picture.)
& adb shell screencap -p /sdcard/myplay-bench.png | Out-Null
& adb pull /sdcard/myplay-bench.png (Join-Path $OutDir 'phone.png') | Out-Null
& adb shell rm /sdcard/myplay-bench.png | Out-Null

# The head unit never exits by itself: stop it together with its Java process.
if (-not $proc.HasExited) { & taskkill /PID $proc.Id /T /F | Out-Null }

# The phone's log for this run. Kept: every line written by MyPlay's own process (also one that
# started or crashed during the run), and the system's lines about that process starting, dying
# or not answering. Lines of other apps that only mention a similar word are left out.
foreach ($one in (Get-MyPlayPids)) { [void]$appPids.Add($one) }
$allLines = @(& adb logcat -d -v threadtime -T "$phoneStart")
$startProc = 'Start proc (\d+):' + [regex]::Escape($Package) + '[/:]'
foreach ($line in $allLines) {
    if ($line -match $startProc) { [void]$appPids.Add($Matches[1]) }
}
# A log line: date, time, process number, thread number, level, tag, text.
$lineForm = '^\d\d-\d\d \d\d:\d\d:\d\d\.\d+\s+(\d+)\s+\d+\s+[A-Z]\s'
$aboutProcess = 'Start proc|has died|Killing|ANR in|Force stopping|FATAL EXCEPTION|Process: '
$kept = New-Object 'System.Collections.Generic.List[string]'
if ($appPids.Count -gt 0) {
    $logFilter = "lines of MyPlay's process (" + (@($appPids) -join ', ') + ') and system lines about it'
    foreach ($line in $allLines) {
        if (($line -match $lineForm) -and $appPids.Contains($Matches[1])) { $kept.Add($line) }
        elseif ($line.Contains($Package) -and ($line -match $aboutProcess)) { $kept.Add($line) }
    }
} else {
    # MyPlay's process was not found: fall back to picking lines by name.
    $logFilter = "by name (MyPlay's process was not found on the phone)"
    foreach ($line in $allLines) {
        if ($line -match 'aare|AareAap|myplay|AndroidRuntime') { $kept.Add($line) }
    }
}
[System.IO.File]::WriteAllLines((Join-Path $OutDir 'phone-log.txt'), $kept, $Utf8)

# --- what happened -----------------------------------------------------------------------------
$headUnitText = ''
if (Test-Path -LiteralPath $outFile) { $headUnitText = Get-Content -LiteralPath $outFile -Raw -Encoding UTF8 }
if ($null -eq $headUnitText) { $headUnitText = '' }
$connected = $headUnitText.Contains('video started')
$pictures = @(Get-ChildItem -LiteralPath $OutDir -Filter 'screen-*.png').Count

$summary = @(
    "MyPlay version: $Version"
    "Walk: $Walk"
    "Events: $Events"
    "Head unit options: $argLine"
    "Ran for: $Seconds seconds" + $(if ($endedEarly) { ' (the head unit stopped by itself before that)' } else { '' })
    "Phone connected and sent video: $connected"
    "Pictures saved: $pictures"
    "Phone log: $($kept.Count) of $($allLines.Count) lines kept, $logFilter"
    "Taken: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
)
[System.IO.File]::WriteAllLines((Join-Path $OutDir 'summary.txt'), [string[]]$summary, $Utf8)
$summary | ForEach-Object { Write-Host $_ }

if (-not $connected) {
    Write-Host ''
    Write-Host 'The phone did not connect. Check on the phone, by hand:' -ForegroundColor Yellow
    Write-Host '  - MyPlay > Advanced > Head unit server is on;'
    Write-Host '  - on the first connection, tap Trust (in the notification or in MyPlay), then run this again.'
    Write-Host "What the head unit said is in $outFile and $errFile"
    exit 1
}
exit 0
