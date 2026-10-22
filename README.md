# MineNAV

MineNAV is an Android map and navigation app built on [OpenStreetMap](https://www.openstreetmap.org) data, with a focus on fully offline use.

It is a personal, non-commercial fork of [OsmAnd](https://github.com/osmandapp/OsmAnd) for my own use, and is in its early stages: the codebase has been trimmed to the Android app only, and rebranding is still to come. Package names, class names and most resources still say "OsmAnd" for now.

## Features (inherited from OsmAnd)

- Offline vector maps, downloadable per country or region
- Turn-by-turn navigation for car, bike and walking, with voice guidance and re-routing
- Search by address, place type or coordinates
- Favorites, POIs, GPX track recording and display
- Online and cached tile maps, overlays and underlays
- Optional plugins: contour lines and hillshade, nautical charts, ski maps

## Project layout

| Module | What it is |
|---|---|
| `OsmAnd` | The Android application |
| `OsmAnd-java` | Core logic in plain Java: map data, rendering, routing, search |
| `OsmAnd-shared` | Kotlin Multiplatform library (Android and JVM targets) |
| `OsmAnd-api` | AIDL API for other apps to talk to the app |
| `plugins/` | Optional plugin apps (Nautical, SRTM, Skimaps, AI Connector) |
| `test/ais` | Small standalone AIS simulator app used for testing |

## Building

Requirements: JDK 17 and the Android SDK (compile SDK 35, min SDK 24).

The build reads icons, fonts, voice files, routing configs and POI types from the OsmAnd resources repository, which must sit next to this one in a folder named `resources`:

```
<workspace>/
├── MineNAV/        ← this repository
└── resources/      ← https://github.com/osmandapp/OsmAnd-resources
```

Prebuilt native libraries are downloaded during the build. To compile them yourself you also need [OsmAnd-core-legacy](https://github.com/osmandapp/OsmAnd-core-legacy) cloned as `core-legacy/` (see `OsmAnd/ndk-build.sh`).

Then build a debug APK:

```bash
./gradlew assembleNightlyFreeLegacyFatDebug
```

The APK ends up in `OsmAnd/build/outputs/apk/`. See `OsmAnd/build-common.gradle` for how external resources are collected, and OsmAnd's [build guide](https://www.osmand.net/docs/technical/build-osmand/) for more detail.

## License and attribution

MineNAV is based on OsmAnd, © OsmAnd BV and contributors.

- Code is licensed under **GPLv3**. Builds kept for personal use carry no extra obligations; anyone who receives a build must also be offered its source under GPLv3.
See [LICENSE](LICENSE) for the full terms. Map data © OpenStreetMap contributors, available under the [ODbL](https://www.openstreetmap.org/copyright).
