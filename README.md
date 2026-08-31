# Galvyx

Galvyx is an Android field-reporting app for technicians who need to document site visits without slowing down the job.

Use Galvyx to capture visit details, work notes, photos, device information, and expenses, then export a clean PDF report from the phone.

## Features

- Create and manage site visit records.
- Capture notes, devices, expenses, photos, and receipt scans.
- Attach one or more receipt photos to each expense.
- Calculate expense totals with breakdowns by category, payment method, reimbursement status, and receipt coverage.
- Edit visit headers, notes, devices, expenses, and photo captions.
- Search visits by client, project, technician, note text, device details, expense entries, and photo captions.
- Filter recent visits by job type.
- Confirm destructive deletes before removing records.
- Store visit data locally on the device.
- Generate and share PDF site visit reports.
- Set company/report profile details for exported reports.

## Privacy posture

Galvyx is local-first. The app does not require an account, does not run a developer-operated backend, does not include advertising, and does not include analytics tracking. Reports are shared only when the user chooses a destination through Android's share sheet.

See `docs/privacy-policy.md` for the public privacy-policy text.

## Build

Open the project in Android Studio and let Gradle sync, or run:

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease bundleRelease
```

If building from a fresh CLI environment, set Android SDK location in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Release materials

- `docs/privacy-policy.md`
- `docs/play-store-listing.md`
- `docs/play-store-readiness.md`
