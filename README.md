# PDF Meta Viewer for Android

The Android companion to [PDF Meta Viewer](https://github.com/Alexxfromgit/PDF-Meta-Viewer) inspects PDF metadata and exports it as JSON. PDF processing runs on the device.

## Project status

The `main` branch contains the original prototype. Its extraction and export code is separate from the navigation screens, and the prototype checkout lacks the Gradle files needed for a standalone build.

The working inspection app is available on [`feature/android-desktop-parity`](https://github.com/Alexxfromgit/PDF-Meta-Viewer-Android/tree/feature/android-desktop-parity). It opens PDFs through Android’s document picker or Open/Share actions, searches and sorts metadata, previews extractable text, and saves JSON through the system save dialog. It uses PDFBox Android for extraction and writes the same `source` and `metadata` fields as the desktop app. [Follow the implementation in pull request #1](https://github.com/Alexxfromgit/PDF-Meta-Viewer-Android/pull/1).

## Build the Android app

There is currently no published APK. Build the feature branch with JDK 17 and Android SDK Platform 35. It runs on Android 9 (API 28) and newer.

```sh
git clone -b feature/android-desktop-parity https://github.com/Alexxfromgit/PDF-Meta-Viewer-Android.git
cd PDF-Meta-Viewer-Android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Use `gradlew.bat` on Windows. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. You can also open the checkout in Android Studio and run the `app` configuration on a device or emulator.

## Use the app

1. Open a PDF from the app or share a PDF to it from another Android app.
2. Search the metadata by field or value, copy selected fields, or switch to the text preview.
3. Export the metadata and choose a destination in Android’s save dialog.

The [feature branch README](https://github.com/Alexxfromgit/PDF-Meta-Viewer-Android/blob/feature/android-desktop-parity/README.md) describes keyboard shortcuts, export compatibility, and test coverage.
