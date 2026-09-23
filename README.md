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

Release builds are signed with the debug keystore on purpose: an unsigned APK cannot be installed, so a
canary pointed at one would download a file it can never run. Nothing here is destined for Play.

No Gradle wrapper is committed — the only binary this tree would carry is `gradle-wrapper.jar`, and CI pins
the Gradle version instead. Locally: install Gradle 8.7+ and run `gradle :app:assembleRelease`.

## The hook

`REMONODE_HOOK_URL` / `REMONODE_HOOK_SECRET` repository secrets point at a CI Build Succeeded trigger's
endpoint. The notify step is skipped when they are absent, and `workflow_dispatch` can send a
`conclusion: failure` payload to exercise the failure path without breaking a build.
