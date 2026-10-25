# FlowMind UI rebuild

Local APK compilation was stopped at the user's request. No subsequent Gradle task, emulator, or APK build was run. The earlier offline attempt failed while resolving AAPT2 and produced no APK.

## Source checks

- Passed: Kotlin grammar parsing across 35 source/test files and XML parsing across 10 files.
- Passed: project import/reference checks and `git diff --check`.
- These are source checks, not evidence of a successful Android build or working device microphone.

## Cloud validation

Use the existing cloud build environment to run `:app:testDebugUnitTest` and its normal APK build. This checkout originally omitted the Gradle launcher scripts/JAR; use the cloud environment's Gradle 9.3.1 setup. The project targets Android 36.1 with AGP 9.1.1.

The existing signing setup is retained; the cloud environment must provide its expected `debug.keystore` when building debug APKs. Custom AI endpoints require HTTPS, including local servers.

- `ReminderTimeTest`: timestamp filtering, date boundaries, weekday recurrence, snooze durations, 24-hour formatting, and parser regressions.
- `ReminderBehaviorTest`: transactional completion without duplicate repeats, persisted schedule edits, provider credential migration/isolation, cancelling disabled alerts, and export data/escaping.
- `FlowMindUiTest`: screen captures, navigation selection, draft validation, and 320 dp layout with enlarged text. Screenshots go to `app/build/outputs/flowmind-review/`.

To generate screenshot artifacts, run the unit-test task with `-Proborazzi.test.record=true` in the cloud environment.

Tests are prepared and have **not** been run locally. Inspect the cloud screenshots before accepting the UI. The glass dock samples the real content beneath it on Android 12 and newer; older versions use a translucent surface. Hardware blur and motion still need a device review.

## Phone checks after the cloud build

1. Fresh install: no fabricated reminders or counters. Existing installs: existing Room data remains intact.
2. Enter a reminder such as `Read tomorrow at 5pm`; check the preview, save once, restart, and verify persistence.
3. Tap the microphone, allow permission, dictate, and verify the final text appears in the draft. Cancel another session and confirm the draft remains. Test denied permission and unavailable speech service. Backgrounding or leaving Today must release recording.
4. Pick a real date in Schedule; test Today, Tomorrow, Priority, and search across titles, categories, notes, and participants.
5. Edit a reminder's date/time, then verify both the displayed schedule and alarm delivery. Snooze from the app and notification; each should honor the selected duration.
6. Complete a repeating reminder. Confirm exactly one next occurrence and a completed journal entry. Repeated taps/actions must not duplicate it.
7. Deny notification access, allow it again, change exact-alarm access, disable alerts, and check that alerts do not fire while disabled. Quiet hours silence sound from 22:00 to 07:00. Android's channel settings can override app sound/banner preferences.
8. Save JSON, Markdown, and Calendar exports to a chosen file; open the results. Provider tests require the user's real service credentials.
9. Check all screens with the keyboard open, system gesture navigation, 320 dp width, and larger text. Confirm the final item can scroll above the dock.

No live provider call, physical speech recognition, notification delivery, native screenshot, or motion result has been verified in this checkout.
