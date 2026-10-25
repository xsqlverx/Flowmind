# FlowMind UI rebuild

Local APK compilation was stopped at the user's request. No subsequent Gradle task, emulator, or APK build was run. The earlier offline attempt failed while resolving AAPT2 and produced no APK.

## Source checks

- Passed: Kotlin grammar parsing across 49 source/test files and XML parsing across 12 files.
- Passed: project import/reference checks and `git diff --check`.
- These are source checks, not evidence of a successful Android build or working device microphone.

## Cloud validation

Use the existing cloud build environment to run `:app:testDebugUnitTest` and its normal APK build. This checkout originally omitted the Gradle launcher scripts/JAR; use the cloud environment's Gradle 9.3.1 setup. The project targets Android 36.1 with AGP 9.1.1.

The existing signing setup is retained; the cloud environment must provide its expected `debug.keystore` when building debug APKs. Custom AI endpoints require HTTPS, including local servers.

- `ReminderTimeTest`: timestamp filtering, date boundaries, weekday recurrence, snooze durations, 24-hour formatting, and parser regressions.
- `ReminderBehaviorTest`: transactional completion without duplicate repeats, persisted schedule edits, provider credential migration/isolation, cancelling disabled alerts, and export data/escaping.
- `FlowMindUiTest`: screen captures, navigation selection, draft validation, and 320 dp layout with enlarged text. Screenshots go to `app/build/outputs/flowmind-review/`.
- `NotificationTimingTest`: lead-time scheduling, a bounded 24-hour catch-up window, and preserving the full snooze interval.
- `NotificationDeliveryTest`: actual receiver-handler/Room/AlarmManager/NotificationManager paths, blocked permission/channel reporting, duplicate suppression, stale/deleted reminders, snooze reconstruction, completion/edit races, recovery, and exact-alarm fallback.
- `ReminderVoiceInputTest`: consecutive dictations, ignored stale callbacks, complete recognizer cleanup, one bounded busy retry, cancelled retries, timeout recovery, permission failures, and partial/empty results. A fake speech engine exercises the production session controller without accessing a microphone.
- `FeedbackQueueTest`: ordered delivery, repeated identical messages, stale acknowledgements, and action/error icons.
- `FeedbackUiTest`: icon/message/compact/upward phases, repeat messages, reduced motion, 320 dp wrapped error text, edit-sheet feedback, and preserving feedback while backgrounded. Existing static screenshots disable decorative motion for deterministic captures.

There are 69 test cases in the checkout. They are prepared for cloud execution and have not been executed locally.

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

## Follow-up motion and voice checks

The user approved the existing visual design and reported a second-dictation failure. This update keeps the visual system, removes tab travel/zoom, adds a small ambient line, and replaces every in-app snackbar with the centered icon/capsule sequence. Feedback is queued centrally and rendered above screen content and sheets. These changes do not replace Android's background reminder notifications.

The active voice controller previously kept the recognizer after final results/errors, and its callbacks shared one active flag across sessions. It now invalidates the old listener before cancelling/destroying the engine, waits briefly before reopening, and handles one service-busy retry. Cancellation, leaving Today, backgrounding, and timeout also dispose of the session and any delayed retry. These are source repairs pending cloud and phone validation.

After the cloud build:

1. Dictate at least five reminders consecutively, including immediate second taps. Each final transcript should append once without restarting the app. Test cancel → start, a silent attempt → retry, background → return, and switching tabs while listening.
2. Delete, complete, archive, restore, snooze, save/edit, export, and trigger a permission or provider error. Every in-app message should show its icon in the center, expand, compact, and exit upward. Trigger the same action twice quickly to check ordered feedback.
3. Check a long error at 320 dp and large text, with the keyboard open and an edit sheet visible. Background during a message; it should remain queued for your return.
4. Watch the next-reminder line move slowly, scroll it away, and return. Check that it stays still when system animations are disabled and does not continue running with the app backgrounded. Review tab changes for a quick stationary dissolve while the dock stays in place.

## Notification audit and repair

The audit found missing contextual notification permission requests, detached reboot recovery, snooze lead-time loss after restart, and silent posting/scheduling failures. These paths were repaired before pushing:

- Creating a reminder requests Android 13+ notification permission once. Today shows permission/channel/master-switch/precise-timing status, with a route to settings.
- Recovery holds `goAsync()` until the Room query and rescheduling complete, with `finish()` in `finally`. Recovery runs after reboot, app update, clock/time-zone changes, and exact-alarm access being granted. This follows Android's [receiver lifetime](https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()) and [alarm recovery guidance](https://developer.android.com/develop/background-work/services/alarms).
- Alarms carry the expected due timestamp. Stale pending alarms/actions are rejected, and deleted records are never reconstructed from old intent text.
- Persistent delivery markers prevent repeat notifications for the same due timestamp. Blocked/failed posts remain eligible for recovery. Missed reminders are retried within 24 hours; older overdue items stay in the schedule without a notification avalanche.
- Snooze timing is persisted separately from global lead time. App and shade actions use the selected interval from now. Transactions prevent an old snooze or edit from undoing a completed reminder.
- Scheduling reports exact, approximate, disabled, expired, already-delivered, or failed outcomes. Test notifications check runtime access, app/channel blocks, and posting results. A successful post is not a delivery measurement on the phone.
- The new monochrome status icon is separate from the launcher artwork. Normal reminders retain standard notification semantics and respect sound, quiet-hours, and banner preferences.

The newer GitHub commit's dot/colon time parsing, archive/restore, swipe, and haptic behavior were integrated into the rebuilt screens. The existing Room schema, signing setup, and data remain intact.
