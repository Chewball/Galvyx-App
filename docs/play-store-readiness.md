# Galvyx Play Store Readiness

## Current publishing position

Galvyx is a local-first field-notes app. The current v1 posture is:

- No login or account system.
- No backend service.
- No analytics SDK.
- No ad SDK.
- Site visit data is stored locally on the device.
- Photos are saved in app-owned external files storage.
- Reports are generated locally and shared only when the user chooses Android's share sheet.

## Store listing checklist

Before production release:

- [ ] Create/confirm Google Play Developer account.
- [ ] Confirm final package name: `com.galvyx.app`.
- [ ] Create production app icon and feature graphic.
- [ ] Capture store screenshots from real app flows.
- [ ] Publish a privacy policy URL.
- [ ] Complete Play Data Safety form using the local-first posture below.
- [ ] Complete content rating questionnaire.
- [ ] Generate signed release `.aab`.
- [ ] Upload to internal testing first.
- [ ] Test install/update from Play internal track.
- [ ] Only then promote toward production.

## Draft Data Safety posture

Use this as a draft, not legal advice:

- Data collected by app: site visit entries, technician-entered notes, device details, expense entries, and photos.
- Data sharing: not shared with the developer by default.
- Data processing: stored locally on device unless the user exports/shares a PDF report through Android share targets.
- Account creation: not required.
- Data deletion: user can delete individual notes/devices/expenses/photos and entire visit records from inside the app.
- Photos: captured and stored for field report documentation; not uploaded by Galvyx.

## Manual smoke test before internal testing

- Create a new visit.
- Edit visit header fields.
- Add, edit, and delete a note.
- Add, edit, and delete a device.
- Add, edit, and delete an expense.
- Add and delete a photo.
- Export/share a PDF.
- Close/reopen the app and confirm records persist.
- Confirm no visit/photo disappears after app restart.
- Test with light/dark system theme.
- Test with larger Android font size.
