# ISP Routes

## Purpose and Capabilities

ISP Routes is an ATAK plugin that speeds up route development by giving users a compact right-side marker palette while keeping the ATAK map visible. The pane occupies approximately 30% of the screen in landscape, leaving the map as the primary workspace.

Users drag a marker tile from the plugin pane onto the map. When the tile is released over the map, the plugin creates a persistent local ATAK marker at that location.

Supported marker types:

- Police Station
- Hospital
- Gas Station
- School
- LOI
- CNP - Left Turn
- CNP - Right Turn
- CNP - Other
- Restricted
- Check Point
- Tunnel
- Overpass

## Status

In development. Current implementation supports the plugin toolbar entry, right-side marker grid, custom bundled vector icons, drag-to-map placement, marker title/category metadata, and local marker persistence.

## Ports Required

No plugin-specific network ports are required. Markers are created locally in ATAK and are not automatically transmitted by this plugin.

## Equipment Required

- ATAK 5.5.0-compatible Android device or emulator
- ATAK MIL/CIV SDK build environment
- Java 17 or newer for Gradle/Android Gradle Plugin execution

## Compilation

Build the Civ debug APK from this plugin directory:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleCivDebug
```

The debug APK is generated under:

```text
app\build\outputs\apk\civ\debug\
```

## Developer Notes

- The plugin package and namespace are `com.atakmap.android.isproutes.plugin`.
- The ATAK plugin extension entry points to `ISPRoutesPlugin` in `app/src/main/assets/plugin.xml`.
- Marker category metadata is stored in `isp_routes_type`, and the display label is stored in `isp_routes_label`.
- The plugin uses ATAK's host `MapView`; it does not embed a separate map inside the plugin pane.
