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
    4. saves the phone's log lines that mention aare or AareAap
  Result: <OutRoot>\<version>\<walk>\  with screen-000.png ..., latest.png, phone.png,
  phone-log.txt, headunit-out.txt, headunit-err.txt and summary.txt.

.EXAMPLE
  .\Run-AudiBench.ps1 -HeadUnitDir C:\myplay\aare-headunit -Walk home

.EXAMPLE
  .\Run-AudiBench.ps1 -ListWalks
#>
[CmdletBinding()]
param(
    # The unpacked "aare-headunit" folder from myplay-headunit-<version>.zip (it holds bin\aare-headunit.bat).
    [string]$HeadUnitDir,
    # Which walk to play; see $Walks below or -ListWalks.
    [string]$Walk = 'home',
    # Name of the result folder. Default: MyPlay's version as the phone reports it.
    [string]$Version,
    # How long to let the head unit run, in seconds. Default: the walk's last event plus 8.
    [int]$Seconds = 0,
    # Where result folders go.
    [string]$OutRoot = (Join-Path (Get-Location) 'myplay-bench'),
    # Leave out the margin option of the profile.
    [switch]$NoMargins,
    # Only print the walks and stop.
    [switch]$ListWalks
)

$ErrorActionPreference = 'Stop'

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
# A picture is saved every 3 seconds, so leave at least 3 seconds between events.
# Add your own walks here.
# ----------------------------------------------------------------------------------------------
$Walks = [ordered]@{
    # Parked, then 12 single clicks forward: the ring should move to each element of the home screen.
    'home'       = 'park@2 rotate:1@5 rotate:1@8 rotate:1@11 rotate:1@14 rotate:1@17 rotate:1@20 rotate:1@23 rotate:1@26 rotate:1@29 rotate:1@32 rotate:1@35 rotate:1@38'
    # One click, press the knob to open what is ringed, wait, then Back.
    'open-back'  = 'park@2 rotate:1@5 dpad:center@8 key:4@17'
    # Driving, wait, parked again: what is restricted while moving should hide and come back.
    'drive-park' = 'park@2 drive@5 park@14'
    # Night colours, then day colours.
    'night-day'  = 'park@2 night@5 day@11'
}

$Package = 'io.github.whiterain3.myplay'

if ($ListWalks) {
    foreach ($name in $Walks.Keys) { Write-Host ("{0,-12} {1}" -f $name, $Walks[$name]) }
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
$Bat = Join-Path (Join-Path $HeadUnitDir 'bin') 'aare-headunit.bat'
if (-not (Test-Path -LiteralPath $Bat)) { Stop-WithMessage "No head unit at $Bat" }

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
    # Connecting takes a few seconds before the walk's clock starts.
    $Seconds = [int][math]::Ceiling($last) + 8 + 10
}

$OutDir = Join-Path (Join-Path $OutRoot $Version) $Walk
if (Test-Path -LiteralPath $OutDir) {
    # Keep an earlier run: move it aside under its own time.
    $stamp = (Get-Item -LiteralPath $OutDir).LastWriteTime.ToString('yyyyMMdd-HHmmss')
    Rename-Item -LiteralPath $OutDir -NewName "$Walk-$stamp"
}
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$OutDir = (Resolve-Path -LiteralPath $OutDir).Path

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

# The phone's log for this run, only the lines about MyPlay's connection.
& adb logcat -d -T "$phoneStart" | Select-String -Pattern 'aare|AareAap' |
    ForEach-Object { $_.Line } | Out-File -FilePath (Join-Path $OutDir 'phone-log.txt') -Encoding utf8

# --- what happened -----------------------------------------------------------------------------
$headUnitText = ''
if (Test-Path -LiteralPath $outFile) { $headUnitText = Get-Content -LiteralPath $outFile -Raw }
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
    "Taken: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
)
$summary | Out-File -FilePath (Join-Path $OutDir 'summary.txt') -Encoding utf8
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
