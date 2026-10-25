# FlowMind — quiet utility

The supplied HTML is a reference draft, not an implementation directive. The Pinterest references set the direction: charcoal surfaces, generous negative space, clear white type, and small instrument-like details. The video contributes the moving selection treatment; the navigation image contributes the cool glass rim.

- Background #090A0C. Surface #18191D. Chalk #F3F2EF. Secondary text #A5A6AF. Ice #D4E5F5 is reserved for selections and progress.
- 24 dp page gutters, 24–32 dp between sections, 48 dp minimum actions. Let titles wrap; support large text and short screens with scrolling.
- Today prioritizes capture and the next actual reminder. Schedule filters timestamps, not decorative time strings. Done uses completion dates. Settings progressively discloses optional AI configuration.
- Native Compose throughout. A 220 ms fade/short slide connects tabs. The floating dock samples the scrolling content for blur on Android 12+, adds a restrained glass rim, and moves its selected pill with a damped spring. Older Android uses a readable translucent surface.
- Preserve Room data and all existing provider, export, reminder, and notification capabilities. No fabricated counters, connectivity badges, avatars, sample tasks, or microphone activity.
- Voice uses Android SpeechRecognizer, runtime permission, partial/final transcripts, explicit errors, and lifecycle cleanup. It never saves a reminder automatically.

Validation: compile the APK, run behavior tests and Compose screenshots, inspect narrow/large-text layouts. A screenshot cannot verify a physical microphone or OEM alarm delivery; these require device checks.

Android implementation references: [SpeechRecognizer lifecycle and service visibility](https://developer.android.com/reference/android/speech/SpeechRecognizer) and [Compose graphics layers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).
