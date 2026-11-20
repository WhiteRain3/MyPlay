# MyPlay host shims (Xposed module)

A separate, optional APK for **rooted phones** with an LSPosed-type framework. MyPlay itself never
depends on it. It comes from upstream AARE's "AARE host shims" module (by prototux and
contributors, GPL-3.0-or-later), changed for MyPlay.

Each MyPlay release has it as `MyPlay-hostshims-<version>.apk`, signed with the same key as the app.

> **Status:** builds and passes its tests. **Not yet tried on a device.**

## What this version does

One thing only: it lets **Waze** accept MyPlay as the app that shows it on the car screen.

Waze is built with Google's Car App Library. Before it shows anything on a car screen, it asks
`androidx.car.app.validation.HostValidator.isValidHost(HostInfo)` whether the app asking (the
"host") is one it trusts, by name and signing key. MyPlay is not on its list, so Waze refuses
("Waze only accepts Google's Android Auto" in MyPlay).

The module makes that check answer **yes when the host is MyPlay** (`io.github.whiterain3.myplay`).
For any other host, Google's Android Auto included, Waze's own check runs unchanged.

It fails open: if Waze does not have that class or method under that name (an app can shorten
its internal names when it is built), or anything else goes wrong, nothing is changed and Waze
behaves as without the module.

The phone's log says what happened, under the tag `MyPlayHostShims`:

| Line | Meaning |
| --- | --- |
| `com.waze: host check hooked` | the hook is in place |
| `com.waze: host check not hooked: …` | it is not, with the reason (for example a renamed class) |
| `com.waze: MyPlay accepted as host` | Waze asked about MyPlay and got yes (once per start of Waze) |

```sh
adb logcat -s MyPlayHostShims
```

## Using it

1. Install `MyPlay-hostshims-<version>.apk` on the phone.
2. In the LSPosed-type manager, enable **MyPlay host shims** and tick **only Waze**.
3. Force-stop Waze (or restart the phone).
4. Connect to the car (or the test bench) and open Waze from MyPlay.

## What is in this folder but not used

Upstream's other hooks are still here and are **not installed** by this version:

- `AppProcessHooks.kt`: Google Maps (a permission check) and the same Car App Library check.
- `MediaHooks.kt`: music apps that only serve Google's Android Auto.
- `SystemHooks.kt`: hooks into Android's own system process. Left out on purpose: a wrong one can
  stop the phone from starting.

They come back one at a time, each only after the one before is proven on a device.

## Form

`src/main/resources/META-INF/xposed/` holds the module's settings in the form the owner's
framework is known to load (`minApiVersion=101`, `targetApiVersion=101`, `staticScope=true`), and
the module is built against version 101 of the hooking library (`io.github.libxposed:api`,
compile-only: the framework provides it). Its scope list holds only `com.waze`.

## Build

```sh
./gradlew :xposed:testDebugUnitTest :xposed:assembleRelease
```

Without the release key in the environment (`AARE_KEYSTORE` and the others, see
`app/build.gradle.kts`) the release APK is unsigned; `:xposed:assembleDebug` gives a debug-signed
one for trying.

The tests check that the class and method the hook names exist in the Car App Library this
project builds with, that only MyPlay is accepted, and that the settings files are in the form
above.
