# Project guidance

## Build and verification

Requires JDK 17, Android SDK 35 and the checked-in Gradle 8.11.1 wrapper (AGP 8.9.0). Minimum SDK is 28; target SDK is 34.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The second command requires an Android device or emulator. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

PDF Meta Viewer Android is a native counterpart of the Java desktop inspection workflow. PDF Hidden Info is a separate product.

- `MainActivity` owns the inspection screen, document-picker/save contracts, clipboard actions, appearance selection and keyboard shortcuts.
- `ui/ViewerViewModel` retains results through recreation and runs all document-provider access, parsing and export work on background executors. A generation check rejects stale analysis results.
- `ui/MetadataPresentation` handles literal case-insensitive search, property/value sorting and selection by metadata key. Sorting must never change which values get copied.
- `extractor/PDFExtractor` creates parser state per document, reads PDF metadata through PDFBox Android, and bounds retained preview text at 2,000,000 characters.
- `extractor/XmpMetadata` projects XMP by namespace, retains multiple values, rejects external entities and normalizes standard timestamps to UTC.
- `repository/DocumentRecord` is immutable. `DocumentRepository` serializes desktop-compatible JSON through Jackson.
- `utilities/FileUtils` snapshots provider URIs to unique cache files without losing their original display names, and deletes snapshots after analysis.

The portrait and landscape activity layouts share IDs. The launch screen resolves these views with `findViewById`; view binding remains enabled for the retained prototype fragments. Those fragments and the old navigation graph are not part of the active inspection workflow.

## Constraints

- Keep all file/provider work off the UI thread. Parsing and exporting must survive activity recreation, and failures must never be reported as successful.
- Snapshot the record before opening a save dialog so changing documents cannot change the export awaiting a destination.
- Use Storage Access Framework grants rather than broad storage permissions. The app has no Internet permission.
- Preserve the JSON envelope `{source, metadata}` and string-valued metadata. Optional text belongs inside `content` and `contentTruncated` fields, never appended after the JSON document.
- Android provider streams cannot guarantee filesystem-level atomic replacement. Serialize JSON completely before opening a destination and propagate write failures.
- Keep view-model-owned search, sort and selection state from being overwritten by restored widget state.
- Keep build/dependency versions in `gradle/libs.versions.toml`. Do not add desktop Tika/PDFBox binaries, iText or duplicate dependency versions to this Android build.
- The README documents parser differences: no external OCR on Android, possible glyph/whitespace differences, and the correct authored XMP dates where desktop JempBox misparses the sample's minutes-only timestamp form.

## Regression coverage

JVM tests cover the real bundled PDF against a desktop export, JSON formatting/failures, immutable records, search/sort/copy semantics, parser isolation, malformed/encrypted PDFs, preview bounds, XMP namespaces/entity rejection, background provider access and rotation. Native tests cover real Android parsing, widgets, recreation/theme changes, JSON streams and failure state. Keep the desktop fixture in `app/src/test/resources/desktop-sample.json` as a recorded baseline; do not silently rewrite it to make tests pass.
