# Galvyx Play Store Readiness

## Release posture

Galvyx is ready for Play Console internal testing after release signing is configured and store artwork is prepared.

Current app behavior:

- No account required.
- No developer-operated backend.
- No analytics SDK.
- No ad SDK.
- Site visit data is stored locally on the device.
- Photos are saved in app-owned external files storage.
- Reports are generated locally and shared only when the user chooses Android's share sheet.

## Play Console checklist

- [ ] Confirm package name: `com.galvyx.app`.
- [ ] Create production app icon and feature graphic.
- [ ] Capture phone screenshots from the current app flow.
- [ ] Publish `docs/privacy-policy.md` at a public URL.
- [ ] Add the public privacy-policy URL in Play Console.
- [ ] Complete the Play Data Safety form using `docs/play-store-listing.md`.
- [ ] Complete the content rating questionnaire.
- [ ] Configure release signing without committing keystore files or passwords.
- [ ] Generate a signed release `.aab`.
- [ ] Upload the signed `.aab` to Play internal testing.
- [ ] Install from the Play internal testing track.
- [ ] Test the update path before production rollout.

## Manual smoke test

- [ ] Create a new visit.
- [ ] Edit visit header fields.
- [ ] Add, edit, and delete a note.
- [ ] Add, edit, and delete a device.
- [ ] Add, edit, and delete an expense.
- [ ] Add, edit, and delete a photo caption.
- [ ] Search visits by client, project, device, note, expense, and photo-caption text.
- [ ] Filter recent visits by job type.
- [ ] Export/share a PDF and confirm page footers/page numbers appear on longer reports.
- [ ] Close/reopen the app and confirm records persist.
- [ ] Confirm no visit/photo disappears after app restart.
- [ ] Test with light and dark system themes.
- [ ] Test with larger Android font size.

## Data Safety answers

- Data collected by app: site visit entries, technician-entered notes, device details, expense entries, photos, and report profile settings.
- Data shared by developer: none by default.
- Data processed by developer: none by default.
- Account creation: not required.
- Data deletion: users can delete notes, devices, expenses, photos, and entire site visit records inside the app.
- Photos: captured and stored for field report documentation; not uploaded by Galvyx.
