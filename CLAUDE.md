# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.pdfmetaviewerandroid.ExampleUnitTest"

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build
./gradlew clean assembleDebug
```

## Architecture

**MVVM with Repository pattern**, using Android Navigation Component for a single-activity, multi-fragment UI.

```
com.example.pdfmetaviewerandroid
├── MainActivity              # Single activity, hosts NavController + BottomNavigationView
├── ui/                       # Three navigation destinations (bottom tabs)
│   ├── home/                 # HomeFragment + HomeViewModel (main PDF interaction)
│   ├── dashboard/            # DashboardFragment + DashboardViewModel
│   └── notifications/        # NotificationsFragment + NotificationsViewModel
├── extractor/
│   └── PDFExtractor          # Apache Tika integration — parses PDF, returns metadata Map
├── repository/
│   ├── DocumentRepository    # Static utility: coordinates extraction + writes JSON to disk
│   └── DocumentRecord        # Data model for a processed document
├── converters/
│   └── ConvertJavaMapToJson  # Jackson-based Map → JSON string conversion
└── utilities/
    ├── PdfMetaViewerConstants # File paths, UI state strings
    └── LookAndFeelConstants   # Unused legacy Swing constants
```

**Data flow:** Fragment → ViewModel (LiveData) → DocumentRepository → PDFExtractor (Tika) → JSON file via Jackson.

## Key Dependencies

- **Apache Tika** (`tika-core`, `tika-parsers-standard-package`, `tika-xmp` 2.8.0): PDF metadata extraction
- **Jackson** (`jackson-databind` 2.15.2): Map-to-JSON serialization
- **SLF4J + log4j-to-slf4j**: Logging bridge (required by Tika)
- **AndroidX Navigation** + **Lifecycle/ViewModel/LiveData**: UI architecture
- **View Binding**: Enabled — use `binding.viewId` rather than `findViewById`

## Important Notes

- Min SDK 28, target/compile SDK 34, Java 8 source/target compatibility.
- `PdfMetaViewerConstants` defines the hardcoded resource paths used by the extractor (e.g., example PDF at `src/main/res/Amazon-S3-Tutorial.pdf`, output at `src/main/res/metadata.json`).
- `LookAndFeelConstants` contains Swing look-and-feel strings carried over from a desktop predecessor — safe to ignore.
- No custom ProGuard rules; minification is disabled in release builds.
