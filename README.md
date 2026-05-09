# PDF Meta Viewer — Android

An Android application that extracts and exports metadata from PDF documents using **Apache Tika**, serializes the results to JSON via **Jackson**, and presents them through a clean MVVM-architected UI.

---

## Features

- Parse any PDF file and extract all embedded metadata (author, title, creation date, content type, and more)
- Optionally include the full document text content alongside the metadata
- Export results as a structured JSON file to a configurable output path
- Clean bottom-navigation UI with three tabs (Home, Dashboard, Notifications)
- Reactive UI powered by LiveData — no manual view refreshes required

---

## Architecture

**MVVM + Repository pattern** with Android Navigation Component (single-activity, multi-fragment).

```
com.example.pdfmetaviewerandroid
├── MainActivity                  # Single activity — hosts NavController + BottomNavigationView
├── ui/
│   ├── home/                     # HomeFragment + HomeViewModel (primary PDF interaction)
│   ├── dashboard/                # DashboardFragment + DashboardViewModel
│   └── notifications/            # NotificationsFragment + NotificationsViewModel
├── extractor/
│   └── PDFExtractor              # Apache Tika integration — parses PDF, returns metadata Map
├── repository/
│   ├── DocumentRepository        # Coordinates extraction and writes JSON to disk
│   └── DocumentRecord            # Data model: id, metadata map, document text
├── converters/
│   └── ConvertJavaMapToJson      # Jackson-based Map → JSON string conversion
└── utilities/
    └── PdfMetaViewerConstants    # File paths and UI state strings
```

**Data flow:**

```
Fragment → ViewModel (LiveData) → DocumentRepository → PDFExtractor (Tika) → JSON file (Jackson)
```

---

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Apache Tika Core | 2.8.0 | PDF parsing engine |
| Apache Tika Parsers Standard | 2.8.0 | Full format parser package |
| Apache Tika XMP | 2.8.0 | XMP metadata support |
| Jackson Databind | 2.15.2 | Map-to-JSON serialization |
| SLF4J | 2.0.9 | Logging facade (required by Tika) |
| log4j-to-slf4j | 2.21.0 | Logging bridge |
| AndroidX Navigation | — | Fragment navigation |
| AndroidX Lifecycle / LiveData / ViewModel | — | MVVM components |
| View Binding | — | Type-safe view access |

---

## Requirements

- **Android SDK:** Min API 28 (Android 9.0 Pie), Target/Compile SDK 34
- **Java:** Source and target compatibility Java 8
- **Android Studio:** Hedgehog (2023.1.1) or newer recommended

---

## Getting Started

### Clone the repository

```bash
git clone https://github.com/your-username/PdfMetaViewerAndroid.git
cd PdfMetaViewerAndroid
```

### Build and run

Open the project in Android Studio and run on a device or emulator (API 28+), or use Gradle from the command line:

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug
```

The debug APK is output to `app/build/outputs/apk/debug/`.

---

## Usage

1. Place a PDF file at the path defined by `PdfMetaViewerConstants.RESOURCES_EXAMPLE_PATH`  
   (default: `src/main/res/Amazon-S3-Tutorial.pdf`).
2. Run the app and navigate to the **Home** tab.
3. Metadata is extracted and written as JSON to the path defined by `PdfMetaViewerConstants.RESOURCES_PATH`  
   (default: `src/main/res/metadata.json`).
4. To include the full document text in the output, pass `isContentIncluded = true` when calling  
   `PDFExtractor.storeDocumentRecordAs(...)`.

### Output format (example)

```json
{
  "Content-Type": "application/pdf",
  "Author": "Amazon Web Services",
  "Creation-Date": "2021-06-15T10:30:00Z",
  "dc:title": "Amazon S3 Tutorial",
  "pdf:PDFVersion": "1.5",
  "xmpTPg:NPages": "42"
}
```

---

## Testing

```bash
# Run unit tests (JVM — no device needed)
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.pdfmetaviewerandroid.ExampleUnitTest"

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

**Test dependencies:** TestNG 7.8.0, AssertJ 3.24.2, Mockito 5.8.0, Espresso 3.5.1.

---

## Project Structure (files)

```
PdfMetaViewerAndroid/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/pdfmetaviewerandroid/
│   │   │   ├── res/
│   │   │   │   ├── layout/          # Fragment and activity layouts
│   │   │   │   ├── navigation/      # Navigation graph
│   │   │   │   ├── menu/            # Bottom navigation menu
│   │   │   │   └── values/          # Colors, strings, themes
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                    # Unit tests
│   │   └── androidTest/             # Instrumented tests
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## License

This project is provided for educational and demonstration purposes.
