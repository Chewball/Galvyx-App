# Galvyx

Galvyx is a local-first Android app for field technicians to capture site visit notes, photos, device details, expenses, and PDF reports.

## Current v1 scope

- Create and manage site visits.
- Capture notes, devices, expenses, and photos.
- Edit visit headers, notes, devices, and expenses.
- Confirm destructive deletes before removing records.
- Store data locally on the device.
- Generate and share PDF site visit reports.
- Maintain a simple company profile for report branding.

## Build

Open the project in Android Studio and let Gradle sync, or run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

If building from a fresh CLI environment, set Android SDK location in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Store readiness docs

- `docs/play-store-readiness.md`
- `docs/privacy-policy-draft.md`
