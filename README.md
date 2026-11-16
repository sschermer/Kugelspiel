# Kugelspiel

Native Android 2D logic puzzle-platformer, **Iteration 003 (0.3.0)**.

## Design

Emphasis: primarily thinking and route planning, with occasional manageable platforming. Every visible collectible or world object serves a function. The world is hand-curated and scrolls horizontally; levels grow progressively longer and contain up to four distinct branch choices.

- Exactly three touch controls: **left**, **right** and **jump**, supporting simultaneous touches.
- The metallic sphere has acceleration, forgiving jumps, and smooth camera follow.
- **Color switches** set blue, orange or violet permanently until changed. Matching gates can be passed; all color switches are repeatable. The switches are now larger, brighter and have a B/O/V symbol for accessibility.
- **Gravity switches** turn gravity upward or downward, letting the ball roll on the ceiling. These switches remain in the world and can be used repeatedly.
- **Point tokens** have values **1, 5, or 10**. Uncollected tokens are **black with white numerals**. Once picked up, the same token stays in the world as a **semi-transparent white token with a black numeral**, and cannot award points again.
- The exit stays locked until the level's minimum point total is met. Lower-point paths are often easier; higher-scoring paths may involve colors, gravity, a short gap, a floating ledge or an occasional spike.
- Checkpoints preserve collected points (and their collected appearance) and allow retrying brief skill passages. On death, the run returns to the last checkpoint's snapshot.
- A progress bar across the top tracks the overall scroll position and marks branch starts.
- Once a level is completed, the highest points on completion are stored locally. Progress is saved under a new key for iteration 003, so redesigned levels begin at level 1.
- Hold **left + right together for 2.5 seconds** to restart the current level; this uses the existing three touch buttons and does not add a new control.

### Levels

| Stage | Name | Width (world units) | Branches | Minimum points |
|---|---|---:|---:|---:|
| 1 | Punktelandung | 1100 | 0 | 6 |
| 2 | Farbiges Tor | 1380 | 0 | 6 |
| 3 | Oben oder unten? | 1780 | 1 | 10 |
| 4 | Zwei Spuren | 2050 | 1 | 10 |
| 5 | Der doppelte Weg | 3000 | 2 | 18 |
| 6 | Schlucht und Steg | 3230 | 2 | 18 |
| 7 | Drei Entscheidungen | 4100 | 3 | 28 |
| 8 | Umkehrpunkte | 4390 | 3 | 29 |
| 9 | Vier Routen | 5490 | 4 | 37 |
| 10 | Der große Irrgarten | 6030 | 4 | 40 |

## Build

Requires JDK 17, Gradle 8.10.2 and Android SDK platform 35. Run `gradle assembleDebug` or use the GitHub Actions **Android Debug APK** workflow. APK: `app/build/outputs/apk/debug/app-debug.apk`.

### Automated checks

The CI build first runs standalone Java tests:

```sh
mkdir -p out
javac -encoding UTF-8 -d out app/src/main/java/de/sschermer/kugelspiel/LevelData.java tools/LevelRouteTest.java
java -cp out de.sschermer.kugelspiel.LevelRouteTest
```

These check increasing level size, number of branches, score thresholds, multiple simple route combinations, pickup and gate placements, checkpoints and forgiving gap widths. They **do not** prove actual full-game solvability, UI quality, touch responsiveness, or the absence of softlocks. The game must be tested on Android handsets.

## Files

- `LevelData.java`: original level definitions, collectibles, colored gates, terrain and hazard data.
- `GameView.java`: Canvas rendering, keyboard-free three-button controls, collision and physics, points/checkpoints/progress.
- `MainActivity.java`: Android activity lifecycle.
- `tools/LevelRouteTest.java`: pure-Java structural and route-choice design checks.

## Licensing and security

All project-owned code and artwork are **not licensed for reuse** pending the owner's explicit decision. A public repository alone does not grant an open-source license. No external game assets, bundled fonts, sounds, SDK permission requests, analytics or network APIs are used. Build-tool upstream details are recorded in `THIRD_PARTY_NOTICES.md`.

The stable package ID is `de.sschermer.kugelspiel`, with versionCode 3. Each GitHub Actions debug build may have a different debug signature. To support in-place updates, set up a persistent **privately stored keystore** for signed builds; never commit the key or passwords to this public repository.

## Current limitations

- The ten level templates are structurally checked but not yet manually verified end-to-end on actual smartphones. Some routes may need balance adjustments after playtesting.
- No production keystore or automated Android device/emulator playthrough has been configured.
- Branches currently use floor and ceiling routes, with a few intermediate ledges; richer vertically connected room geometry remains for future iterations.
