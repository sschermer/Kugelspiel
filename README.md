# Kugelspiel

Native Android logic puzzle-platformer — **Iteration 006 / 0.6.0**.

## Geometry correction based on phone screenshot

The previous prototype rendered a large turquoise or red rectangle spanning the vertical gap between levels. Even though floors had already become 19 units thick, the highlighted passages were still huge. This iteration makes the geometry and visuals match:

- Each floor is also the ceiling of the level below: one **19-world-unit shared divider**, not two different plates with an empty buffer between them.
- Safe passages and lethal gaps now cross only that actual **19-unit divider**, with a narrower opening of **three 58-unit tiles**.
- Safe openings are marked with **turquoise short edge rails, a downward arrow and DURCHGANG**, not a giant full-height turquoise fill.
- Deadly gaps remain visibly different: **dark red interior, red teeth and GEFAHR**.
- Structural collision is matched to the visible thin divider throughout the maze; intermediate platforms remain functional.
- **Every colored gate spans the full navigable room height from ceiling underside to floor top.** A 250% jump cannot bypass a closed colored barrier.
- For consistency, point-requirement interdoors also span full room height.

## Retained mechanics

- Exactly three touch controls: left/right/jump.
- Bounce-chain jumping: normal 100% height, timed automatic landing hop, three cumulative boosters to 150/200/250%, unlimited chains at the capped height, **instant reset to 100% on any head-to-ceiling contact**.
- Reusable color and gravity switches.
- Helium fill and drain machines require positioning and low movement speed. Filling can be gated by score requirements.
- Collectible 1/5/10 point tokens remain visible after collection, changing from black/white to translucent white/black, and count only once.
- Exits and intermediate doors open when the necessary accumulated points are reached. Points are not spent.
- Ten progressively larger scrolling multi-floor mazes. Hold left+right simultaneously to restart the current level.

Existing saved progress from iteration 005 remains on the same level key (`level_005`) so installing this update does not intentionally reset stage progress.

## Build and validation

JDK 17; Android SDK 35; Gradle 8.10.2.

Run `gradle assembleDebug` or GitHub Actions. Output: `app/build/outputs/apk/debug/app-debug.apk`. CI artifact: `kugelspiel-iteration-006-debug`.

To run the pure-Java structural and bounce tests:

```sh
mkdir -p out
javac -encoding UTF-8 -d out app/src/main/java/de/sschermer/kugelspiel/LevelData.java tools/LevelRouteTest.java app/src/main/java/de/sschermer/kugelspiel/BounceChain.java tools/BounceChainTest.java
java -cp out de.sschermer.kugelspiel.LevelRouteTest
java -cp out de.sschermer.kugelspiel.BounceChainTest
```

The structure tests explicitly check the 19-unit shared divider, 3-tile opening, and exact full-room gate spans. **Automated structural checks do not constitute a real-device playthrough**; playability and difficulty across all ten levels require device testing.

## Copyright and signing

Original Canvas graphics, with no third-party runtime assets. Code and graphics remain owner-controlled pending explicit license approval; public visibility alone does not grant reuse rights. See `THIRD_PARTY_NOTICES.md`.

Package: `de.sschermer.kugelspiel`; versionCode **6**. Debug APK signing is currently not stable between CI runs; updating an installed debug build may require uninstalling its predecessor. Never add signing keys or passwords to the public repository.
