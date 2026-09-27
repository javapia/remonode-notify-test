# remonode-notify-test

Scratch repository for exercising remonode against a real build: every push to `main` produces an
installable APK, publishes it at a URL that never changes, and pokes remonode's **CI Build Succeeded**
trigger.

## What a remonode workflow needs from this repo

| | |
|---|---|
| APK URL (Install App) | `https://github.com/javapia/remonode-notify-test/releases/latest/download/app-release.apk` |
| Package / Bundle ID | `io.remonode.notifytest` |
| Text on the launched screen | `CANARY OK` |
| Test login (see below) | `qa@example.com` / `canary-password`, 2FA key `REMONODECANARY23` |

The URL is a permalink: `releases/latest` follows the newest non-prerelease release, and CI moves the
`nightly` release onto every new commit with the asset name unchanged. So a scheduled workflow pasted with
that URL once installs today's build every morning, with nothing to edit and no token to supply — the
worker fetches `appUrl` with a plain unauthenticated GET, which is also why Actions *artifacts* can't be
used here.

`CANARY OK` is drawn only after the activity finishes starting, which is what makes it worth waiting for:
the app's label and its icon are on screen before the app has done anything. Under it the screen prints the
CI build number and commit the APK came from, so a screenshot mailed back on success says *which* build
passed.

## The app

One activity, built in code — no layout XML, no AppCompat, no Compose ([MainActivity.java](app/src/main/java/io/remonode/notifytest/MainActivity.java)).
It exists to be launched and asserted on, not to do anything.

## Test login with 2FA

Under the `CANARY OK` header is a pretend sign-in with a real second factor, for remonode's
**Login still works** template: email + password → a 6-digit authenticator code → a signed-in screen.

| | |
|---|---|
| Email | `qa@example.com` (the template's Set Values default) |
| Password | `canary-password` |
| 2FA setup key | `REMONODECANARY23` — TOTP, SHA1, 6 digits, 30 s (the 2FA Code node's defaults) |

These are public on purpose: this is a canary on test devices, not an account anyone can take over. The
screen prints them too, so a person driving a live session can sign in by hand; add the key to any
authenticator app to get codes on your phone.

Filling in the template (only the empty fields):

| Step | Field | Value |
|---|---|---|
| Launch / Activate | Package / Bundle ID | `io.remonode.notifytest` |
| Input Text (1st) | Selector | `id=io.remonode.notifytest:id/email` |
| Input Text (2nd) | Selector | `id=io.remonode.notifytest:id/password` |
| | Text | `canary-password` |
| | Press Enter after typing | **on** — this is what signs in; the template has no Tap on the button |
| 2FA Code (TOTP) | Setup key | `REMONODECANARY23` |
| Assert / Verify | Selector | `id=io.remonode.notifytest:id/signed_in` (text `SIGNED IN`) |

Why the screen behaves the way it does — all of it follows from how remonode types:

- **Enter anywhere on the sign-in screen signs in.** A field picked by selector is filled with `setValue`,
  which does not reliably move focus to it, so a per-field Enter handler would miss the key.
- **The code field is focused the moment the code screen opens**, and keys arriving elsewhere are routed
  into it: the template types the code into *whatever has focus right now*.
- The code is checked at the sixth digit and on Enter, and one 30-second step either side is accepted —
  the code is generated on remonode's worker and typed a moment later on a phone with its own clock.
- **Leaving the app signs out.** Launch App resumes a running app where it was, and a canary that came back
  already signed in would skip the screens the test exists for.

Other ids on the screen: `sign_in` (button), `code`, `verify`, `error` (the red line under the fields:
wrong password, wrong code), `account` (`as qa@example.com`), `sign_out`.

## Rotate & resume

remonode's **Rotate & resume** template takes a screenshot in landscape and another after a background →
foreground round trip. A screenshot only proves something if the screen says what happened, so under the
launch time the app prints a lifecycle line (`id=io.remonode.notifytest:id/lifecycle`):

    landscape · created 2× · resumed 2×

- **created** counts how many times Android built the activity. The app does not handle rotation itself
  (most apps don't), so each rotation rebuilds it: launched = 1×, after the landscape step = 2×.
- **resumed** counts every return to the foreground.
- The launch time above it is per process, so it stays put through all of this; a new time means the app
  was killed and started cold.

Expect more rebuilds than rotations after Background App: the home screen is portrait-only, so the stopped
app is rotated underneath it and rotated back on return. Replayed on a Pixel 9 emulator (API 35): launched
1×, landscape 2×, background + back 4×, portrait 5×.

The only fields to fill in the template are Install App's (the APK URL above, package `io.remonode.notifytest`,
Launch after install on) and the trigger's device.

## Onboarding walkthrough

remonode's **Onboarding walkthrough** template captures a screen, scrolls down, captures, scrolls, captures.
On a screen that fits the phone both scrolls move nothing and the three captures are identical — a green
run that tested nothing — so below the fold the app has a three-panel tour, each panel a full screen tall
and labelled `TOUR n OF 3` (`id=io.remonode.notifytest:id/tour_1` … `tour_3`).

A fling snaps to the next panel. The template's Scroll (70%, 300 ms) is a fling, and a plain ScrollView
would coast a device-dependent distance; snapping makes every run capture the same three things:

| Capture | Shows |
|---|---|
| 1 (after launch) | the usual screen — header, sign-in, Stability test buttons |
| 2 (after the first Scroll) | TOUR 1 OF 3 — Install once |
| 3 (after the second Scroll) | TOUR 2 OF 3 — Test unattended |

Add a third Scroll + Screenshot to reach TOUR 3. A slow drag still scrolls freely. The tour is below
everything else, so the launch screen the other templates wait for and tap on is unchanged. Replayed on a
Pixel 9 emulator (API 35) with the Scroll node's exact default gesture.

Only Install App needs filling in (APK URL, package `io.remonode.notifytest`; Launch after install is on by
default), plus the trigger's device.

## Deep link check

remonode's **Deep link check** template opens `https://example.com/product/123` and asserts it landed on the
right screen rather than a web page. The app answers to exactly that link (and to
`remonode-canary://product/<id>`) with a product screen: `PRODUCT 123` (`id=io.remonode.notifytest:id/product`)
and the link it was opened from (`…:id/deep_link`). The id comes from the link, so `/product/42` shows 42.

Both branches can be run on purpose:

| Open URL / Deep Link | Lands on | Assert `id=io.remonode.notifytest:id/product` |
|---|---|---|
| Target app `io.remonode.notifytest` | the product screen | **true** → Screenshot |
| Target app empty | the browser — nothing has verified example.com | **false** → Fail the Run |
| `remonode-canary://product/42`, no target | the product screen (`PRODUCT 42`) | **true** |

The second row is the real-world failure the template exists for: an https link the app claims but the OS
won't give it without domain verification (`autoVerify` + `assetlinks.json`), which we can't have for a
domain we don't own. Checked on a Pixel 9 emulator (API 35) with the same `am start -a VIEW -d` that
Appium's `mobile: deepLink` sends.

Install App must come first with **Launch after install off**, so the link is what opens the app.

## Crash & ANR on demand

remonode's **Crash & ANR watch** template records the device log and greps it for
`FATAL EXCEPTION|ANR in|Force finishing`. An app that never crashes only ever takes its "nothing found"
branch, so two buttons under the card break the app on purpose:

| Button | Selector | What the log gets |
|---|---|---|
| Crash now | `id=io.remonode.notifytest:id/crash` | `FATAL EXCEPTION: main` + `Force finishing activity`, at once |
| Freeze (ANR) | `id=io.remonode.notifytest:id/freeze` | `ANR in io.remonode.notifytest`, ~10 s after the tap |

Where the Tap goes in the template matters: **after Background App, before Read Logs (Stop).** Before it, a
crashed app leaves the launcher in front, Background App reports "stayed in the foreground" and the run stops
before the log is ever checked. For the freeze, add a **Wait** of 15 s after the Tap — Android takes 10 s to
declare the ANR, and a Stop that comes sooner saves a log without it.

The freeze is a foreground broadcast whose receiver blocks for 30 s, because it has to become an ANR with
nobody touching the phone: a blocked click handler needs a second input event to count, and a blocked
service's start timeout never fired on API 35. Checked on a Pixel 9 emulator (API 35).

Release builds are signed with the debug keystore on purpose: an unsigned APK cannot be installed, so a
canary pointed at one would download a file it can never run. Nothing here is destined for Play.

No Gradle wrapper is committed — the only binary this tree would carry is `gradle-wrapper.jar`, and CI pins
the Gradle version instead. Locally: install Gradle 8.7+ and run `gradle :app:assembleRelease`.

## The hook

`REMONODE_HOOK_URL` / `REMONODE_HOOK_SECRET` repository secrets point at a CI Build Succeeded trigger's
endpoint. The notify step is skipped when they are absent, and `workflow_dispatch` can send a
`conclusion: failure` payload to exercise the failure path without breaking a build.
