# PDF Meta Viewer for Android

Native Android counterpart of the Java desktop PDF Meta Viewer. Documents stay on the device; the app has no Internet or broad storage permission.

## Features

- Open a PDF with the system document picker, an Android Open/Share action, or a one-file drag and drop.
- Automatic background analysis with a loading indicator and explicit failures. Selecting a new PDF cancels the previous request and prevents stale results from replacing the new document.
- Inspect document information, custom fields, XMP, PDF version, page count, encryption permissions, annotation types, font flags and text diagnostics.
- Search property names and values literally, ignoring case; sort either property or value in either direction.
- Select multiple fields, select all visible fields, and copy property/value pairs as tab-separated rows. Long-press a row to copy that field.
- View and select extracted text, with the desktop's 2,000,000-character preview limit and truncation indicator.
- Export pretty-printed UTF-8 JSON using Android's save dialog. Its create-document workflow handles existing-name conflicts without silently replacing the original PDF.
- See the original document name, actual copied file size, page count and field count.
- Switch light/dark appearance; the choice is remembered. Results, search, selection and the selected tab survive rotation and appearance changes.
- Portrait and landscape layouts, system-inset/keyboard handling and accessible control labels.

Keyboard: Ctrl/Command+O opens a PDF; Ctrl/Command+E exports JSON; Ctrl/Command+F focuses metadata search; Ctrl/Command+C copies selected metadata; Escape clears the search.

## JSON compatibility

The default export has the same envelope as the current desktop application:

```json
{
  "source": "example.pdf",
  "metadata": {
    "Content-Type": "application/pdf",
    "dc:title": "Example"
  }
}
```

Metadata values are strings and keys are sorted. Default exports exclude document text. The repository API can optionally include `content` and `contentTruncated` as proper JSON fields.

Android uses PDFBox Android 2.0.27.0 because the desktop Tika/PDFBox dependencies are not Android-compatible. Canonical metadata and aliases are checked against a real export from desktop HEAD `2d771f3` (Tika 3.3.2), using the same bundled Amazon S3 PDF. Five extraction diagnostics are intentionally excluded from exact equality: the four glyph/count/ratio fields can differ between parser versions, and `pdf:ocrPageCount` is always `0` on Android. Android does not run desktop external OCR programs; image-only pages have no extractable text. Text whitespace can also differ between parsers. The fixture also exposes a desktop JempBox bug: it parses `2017-03-16T15:02+02:00` as `2017-03-17T11:02:00Z`. Android preserves the correct authored instant, `2017-03-16T13:02:00Z`, for the two affected XMP date fields; the test asserts those exact corrected values rather than reproducing the desktop bug.

The XMP projection is namespace-aware, keeps multiple values, includes language alternatives and normalizes standard timestamps to UTC. Malformed, oversized or unsafe XMP produces a warning field while preserving the remaining document metadata. A PDF that requires a password returns a clear error, matching the desktop inspection workflow; password entry, editing and metadata scrubbing are outside this product's current desktop scope.

Local filesystem compatibility exports use an atomic replacement where supported. Android document-provider streams cannot promise filesystem-level atomic moves; export JSON is fully serialized before the destination is opened, and a provider/write failure is reported as a failure.

## Build and verification

Requires JDK 17 and Android SDK 35. The Gradle wrapper pins Gradle 8.11.1 and AGP 8.9.0. Minimum Android version is 9 (API 28); target SDK remains 34.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Verified on 22 October 2026: debug APK build, 21 passing JVM regression tests, Android lint with zero errors, and two passing native workflow tests on Android 16 / API 36 (arm64).

Tests cover real-PDF metadata compatibility, export format and failures, immutable results, literal search, selection after sorting/filtering, repeated extraction, malformed/encrypted PDFs, preview bounds, XMP namespace handling/entity rejection, active-screen IO, rotation and read-error state. The desktop reference fixture lives at `app/src/test/resources/desktop-sample.json`.

## Structure

- `MainActivity`: native inspection screen and system document contracts.
- `ui/ViewerViewModel`: background work, cancellation, retained results and export snapshots.
- `ui/MetadataPresentation`: search, sorting and selection independently of Android widgets.
- `extractor/PDFExtractor`: per-document parser state and bounded text extraction.
- `extractor/XmpMetadata`: read-only XMP projection.
- `repository/DocumentRecord`: immutable result.
- `repository/DocumentRepository`: desktop-format JSON and export naming.
- `utilities/FileUtils`: a unique, temporary local snapshot of a provider URI, preserving its original display name and cleaning it after analysis.

The original prototype fragments and compatibility APIs remain in the tree, but are not part of the launch screen. PDF Hidden Info Android/iOS is a different product and is not a dependency or source of this implementation.

## License

This project is provided for educational and demonstration purposes.
