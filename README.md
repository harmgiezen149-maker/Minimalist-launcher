# Minimalist Launcher

A dumb-phone style Android launcher with an app blocker and screen time
control. Turn your phone into a distraction-free device: a text-only home
screen, blocked apps, daily time limits, blocking schedules, notification
filtering and grayscale themes.

Everything runs on-device. No accounts, no backend, no analytics. The
only network request the app ever makes is the optional temperature
fetch to open-meteo.com (coarse coordinates only). The accessibility
service is used only for in-app reminders and blocking; nothing from
accessibility events is collected or stored.

## Features

- **Minimalist launcher** — text-only favorites list with clock, date and
  outdoor temperature (tap it once to grant coarse location; data comes
  from open-meteo.com and nothing else leaves the device), app drawer
  with search that slides up from the bottom, and configurable
  left/center/right list alignment. No icons, no dopamine.
- **Configurable corner shortcuts** — Phone bottom-left, Gmail
  bottom-right by default; long-press either to pick any app.
- **Gestures** — swipe up for the app drawer, swipe down for the
  notification shade (needs the blocker service).
- **Hide & rename apps** — long-press any app in the drawer.
- **Folders** — group similar apps in the drawer: long-press an app →
  "Add to folder". Tap a folder to open it; search always spans every
  app so nothing is buried.
- **Mindful pause** — put apps behind a short breathing pause that asks
  "how long do you want to use it?" (1–15 min); when the time is up the
  pause returns.
- **App blocker** — put distracting apps on a block list; a focus screen
  steps in when you open them.
- **Focus mode & focus sessions** — a permanent toggle, or a timed
  15/25/45/60-minute session with a countdown on the home screen.
- **Day / evening mode** — off by default. Switch it on and every app is
  "always", "day only" or "evening only"; the evening window (20:00–07:00
  by default, wrapping past midnight) decides what is on screen, and the
  active mode is shown on the home screen. Need an evening app during the
  day? "Evening mode now" asks for a confirmation first, then runs for
  15/30/60 minutes and expires on its own. Notifications follow the same
  windows: per app you can allow them always, during the day only, or in
  the evening only.
- **Blocking schedules** — automatically block during chosen windows
  (e.g. Mon–Fri 09:00–17:00; overnight windows supported).
- **Daily time limits** — per-app minutes budget; blocked for the rest of
  the day once reached.
- **Website blocker** — block chosen sites (e.g. youtube.com, including
  subdomains) in common browsers.
- **Screen time dashboard** — today's usage per app and in total.
- **In-app time reminders** — a gentle notification after N minutes in
  the same app.
- **Notification filter** — mute chosen apps entirely, or let day/evening
  windows decide per app; filtered notifications are counted on the stats
  screen. Filtering cancels a notification, so one blocked during the day
  is dismissed, not held back until the evening.
- **Themes** — monochrome Light / pure-black, white or CRT-green text, a
  large-font option and an optional system-wide grayscale toggle.

## Install on your phone

Download the newest build on the phone itself:

<https://github.com/harmgiezen149-maker/minimalist-launcher/releases/latest/download/minimalist-launcher.apk>

If a download scanner holds the `.apk` at 99%, take the zipped copy and
open the APK inside it:
<https://github.com/harmgiezen149-maker/minimalist-launcher/releases/latest/download/minimalist-launcher-apk.zip>

1. Open the APK. Allow "install unknown apps" for your browser or file
   manager when Android asks.
2. Launch **Minimalist Launcher** and walk through the permission
   checklist. Every permission is optional; each one unlocks the feature
   listed next to it.

Updates: install a newer APK straight over the old one; settings survive.
Every build has a higher version number (Settings shows it at the bottom).
After an update that changes the blocker service's capabilities (like the
website blocker), toggle the service off and on once under Settings ›
Accessibility if blocking doesn't react.

Moving to a new phone, or reinstalling: Settings › Backup › Export settings
writes everything to a file; Import settings on the new install reads it
back. Permissions are not in the file; grant them again under Permissions.

### Signing

Only builds of `main` are published, and only after they pass lint and the
tests. They are signed with a private key that exists in this repository
solely in encrypted form (`keystore/release.p12.gpg`); the passphrase is the
repository secret `SIGNING_PASSPHRASE`. The key is decrypted only inside the
publish job, which runs no Gradle or other build code, and every published
APK is checked against the certificate fingerprint in
`keystore/release-cert.sha256`. The key was created once on GitHub's runner
by the **Create signing key** workflow.

Never replace the key: Android accepts an update only when it is signed
with the same key as the installed app. A new key means uninstalling and
reinstalling (export your settings first).

Builds published before the move to this repository were signed with a key
that was public; uninstall those before installing a build from here.

To go back to your old home screen: Settings › Apps › Default apps ›
Home app, or simply uninstall Minimalist Launcher.

### Recent apps overview empty?

On Samsung phones the recent-apps overview is always drawn by One UI
Home, even when another launcher is the default; this launcher has no
way to remove apps from that list. If apps sometimes vanish from it:

1. Test first: set One UI Home as the home app for a day or two
   (Settings › Apps › Default apps › Home app). If the list still empties,
   the cause is outside this launcher.
2. Settings › Apps › One UI Home › Battery → Unrestricted.
3. Device Care › Battery › Background usage limits: keep One UI Home and
   your everyday apps out of "Sleeping apps" / "Deep sleeping apps" (or add
   them to "Never sleeping apps"), and turn off "Put unused apps to sleep".
4. Device Care › Auto optimisation: turn off the automatic restart and
   memory clean-up.
5. Switch the home app to One UI Home and back, then restart the phone.

### System-wide grayscale (optional, power users)

The in-app themes are already monochrome. To make the *entire phone*
grayscale, grant one extra permission over USB debugging:

```
adb shell pm grant io.github.minilauncher android.permission.WRITE_SECURE_SETTINGS
```

Then use Settings › System grayscale inside the app.

## How blocking works

The launcher's accessibility service watches window changes. When a
blocked or over-limit app comes to the foreground it jumps back to the
home screen (this launcher *is* the home screen, which makes that
reliable) and shows a full-screen focus page with the reason and today's
usage — optionally with an "Allow 5 more minutes" escape hatch that you
can disable for hard mode.

Screen time is measured with Android's UsageStatsManager; nothing is
persisted, it is recomputed from the system on demand.

## Building

CI (`.github/workflows/build.yml`) runs lint and the unit tests and builds
the release APK on every push, with the Gradle wrapper and JDK 17. Locally
(needs the Android SDK):

```
./gradlew lint testDebugUnitTest assembleDebug
```

## On-device test checklist

- Set as default home; press Home from another app → lands on launcher.
- Add favorites, rename one, hide one (recover via Settings › Hidden apps).
- Block an app, enable Focus mode, open it → focus screen appears.
- Create a schedule crossing the current time → app blocks; cross the end
  boundary → unblocks.
- Set a 1-minute limit on an app, use it past the limit → blocked until
  midnight.
- Mute an app's notifications and trigger one → it disappears; count shows
  on the stats screen.
- Reboot → blocker and notification filter still work (services rebind).
