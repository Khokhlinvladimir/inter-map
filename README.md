# inter-map

### osmdroid, fully migrated to Kotlin

[![CI](https://github.com/Khokhlinvladimir/inter-map/actions/workflows/CI.yml/badge.svg)](https://github.com/Khokhlinvladimir/inter-map/actions/workflows/CI.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android API](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**inter-map** is a Kotlin-first continuation of [osmdroid](https://github.com/osmdroid/osmdroid): an open-source Android `MapView` with online and offline tiles, overlays, location, shapes, WMS, Mapsforge, GeoPackage support, and a large executable sample catalog.

The repository has been synchronized with the final official upstream code and then migrated from Java to Kotlin across the library, applications, tools, tests, and examples. The upstream project is archived; this fork is an independent continuation and is not an official osmdroid release.

## Gallery

<table>
  <tr>
    <td width="33%" align="center">
      <img src="docs/screenshots/home.png" alt="OpenStreetMapViewer sample catalog" width="100%"><br>
      <strong>Sample catalog</strong>
    </td>
    <td width="33%" align="center">
      <img src="docs/screenshots/starter-map.png" alt="Starter map example" width="100%"><br>
      <strong>Starter map</strong>
    </td>
    <td width="33%" align="center">
      <img src="docs/screenshots/clustering.png" alt="Grid marker clustering example" width="100%"><br>
      <strong>Marker clustering</strong>
    </td>
  </tr>
</table>

## What changed

- **Kotlin throughout** — all tracked Java production, test, sample, and utility sources were migrated to idiomatic Kotlin while preserving the established APIs and behavior.
- **Current upstream baseline** — the official `osmdroid/osmdroid` history through [`b491ee4`](https://github.com/osmdroid/osmdroid/commit/b491ee4b7) is included in the migration branch.
- **Native marker clustering** — a reusable grid-based cluster overlay and UI-independent clustering algorithm are included in `osmdroid-android`.
- **Runtime hardening** — sample lifecycle, asynchronous callback, cache, snapshot, and Android component-export behavior have been tightened for current Android versions.
- **Executable migration coverage** — unit tests, Android instrumentation, and the complete OpenStreetMapViewer catalog exercise the port rather than only checking compilation.

## Marker clustering

Archived upstream osmdroid does not contain a built-in marker clustering overlay. This fork adds an independent implementation with:

- linear-time grid grouping for large marker sets;
- Web Mercator projection and fractional zoom support;
- correct centroid handling around the antimeridian;
- cached cluster icons and automatic rebuilding when data or zoom changes;
- click-to-zoom behavior;
- a deterministic 1,200-marker sample in OpenStreetMapViewer.

The algorithm is separated from Android UI classes, so its grouping behavior can be unit-tested directly and reused independently of rendering.

## Modules

| Module | Purpose |
| --- | --- |
| `osmdroid-android` | Core Android `MapView`, tile providers, overlays, offline storage, and clustering |
| `osmdroid-mapsforge` | Mapsforge tile integration |
| `osmdroid-geopackage` | GeoPackage support |
| `osmdroid-wms` | WMS client support |
| `osmdroid-shape` | Shapefile support |
| `OpenStreetMapViewer` | Full sample and regression application |
| `osmdroid-simple-map` | Minimal Android integration example |
| `OSMMapTilePackager` | JVM tool for packaging map tiles |
| `osmdroid-server-jdk` | JVM tile server |

## Build from source

Requirements:

- JDK 17 or newer;
- Android SDK 34;
- Android API 24+ for Android modules.

The Gradle wrapper is included, so a separate Gradle installation is not required.

```bash
# Linux / macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

Build and install the sample browser:

```bash
./gradlew :OpenStreetMapViewer:installDebug
```

Build the library and publish it to the local Maven repository:

```bash
./gradlew :osmdroid-android:publishToMavenLocal
```

The local development coordinate follows the upstream project properties:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.21-SNAPSHOT:debug@aar")
}
```

> This fork does not claim the official `org.osmdroid` Maven Central releases. Use `mavenLocal()` for a local build or publish the fork under coordinates you control.

## Verification

The Kotlin port was checked at three levels on an Android 16 / API 36 x86_64 emulator:

1. JVM unit tests for the core library and extension modules, including clustering edge cases.
2. Android instrumentation and historical bug-regression scenarios.
3. An exhaustive OpenStreetMapViewer device run that opened all **106/106** registered sample fragments, all **11/11** historical bug drivers, and the remaining **13** instrumentation tests. This includes the clustering demo and asynchronous lifecycle scenarios.

The project supports Android API 24+, but this migration verification used the locally available API 36 emulator. API 24 remains a separate compatibility target.

Some examples depend on external services, local endpoints, API keys, or downloadable map data. Their integration boundary is kept explicit; deterministic local behavior is tested without embedding third-party credentials in the repository.

Useful focused commands:

```bash
./gradlew :osmdroid-android:testDebugUnitTest
./gradlew :OpenStreetMapViewer:assembleDebugAndroidTest

adb shell am instrument -w \
  -e class org.osmdroid.test.ExtraSamplesTest#testActivity \
  org.osmdroid.test/androidx.test.runner.AndroidJUnitRunner
```

## Project status

The original [osmdroid repository](https://github.com/osmdroid/osmdroid) is archived and no longer receives releases. Its source, documentation, wiki, and history remain the compatibility reference for this fork:

- [osmdroid wiki](https://github.com/osmdroid/osmdroid/wiki)
- [How to use the library](https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library)
- [Upgrade guide](https://github.com/osmdroid/osmdroid/wiki/Upgrade-Guide)
- [Original contributors](https://github.com/osmdroid/osmdroid/graphs/contributors)

Map tiles and map data are supplied by the provider selected by the application. They are not included with the library; always follow the selected provider's usage policy and attribution requirements.

## License

Licensed under the [Apache License 2.0](LICENSE), consistent with the upstream project. See the repository history for individual contributions and attribution.
