# Kugelspiel

Native Android puzzle-platformer, **Iteration 004 / Version 0.4.0**.

## New level structure

The first three stages introduce collection, color matching, and vertical navigation. From stage 4, the layouts become progressively larger **multi-storey scrolling mazes**. Stage 10 has six floors, two routes between most adjacent floors, and optional routes to more point tokens. Dimensions increase steadily from 27 × 10 to 125 × 40 tiles.

World-space gameplay elements are roughly one third smaller than the former prototype. The on-screen three-button controls remain touch-sized. Both horizontal and vertical camera movements follow the sphere.

### Passage or death trap?

- **Turquoise passage:** turquoise vertical side rails, downward arrow, and the label **DURCHGANG**. These holes are open through both the floor and the lower level ceiling, so the sphere can fall through to continue exploring.
- **Red pit:** dark red opening with triangular danger teeth and the label **GEFAHR**. Falling into one kills the sphere and returns it to the last checkpoint or level start.
- Jumping remains intentionally forgiving; intermediate platforms are optional point detours.
- Later worlds combine floor and ceiling routes, gravity switches, color gates, safe vertical connections, dangerous pits, and optional platforms.

### Other retained mechanics

- Exactly three controls: left, right, jump (multitouch).
- Metallic sphere; horizontal momentum and variable gravity direction.
- Large, saturated blue/orange/violet color crystals with B/O/V labels; color and gravity changes remain active and **can be used repeatedly**.
- Score items **1/5/10** award points only once. Uncollected: **black with white number**; collected: **semi-transparent white with black number**.
- Exits unlock at the displayed score threshold. The exit reads **NOCH X / PUNKTE**, or **OFFEN** when ready.
- Saved stage progress, best-level scores, checkpoint replay, and hold left + right for 2.5 seconds to restart.

## Build and tests

JDK 17, Gradle 8.10.2 and Android SDK 35. Run `gradle assembleDebug` or download the GitHub Actions workflow artifact `kugelspiel-iteration-004-debug`.

A plain-Java map validation checks tile traversability, opening dimensions, level growth, exit connectivity in the tile layout, point thresholds, and hazard/passage categories:

```sh
mkdir -p out
javac -encoding UTF-8 -d out app/src/main/java/de/sschermer/kugelspiel/LevelData.java tools/LevelRouteTest.java
java -cp out de.sschermer.kugelspiel.LevelRouteTest
```

**Limitations:** This is a structural maze check, not a full Android physics playthrough. Physical reachability under the actual gravity and color rules, every possible softlock, point balancing, and perceived difficulty must still be tested on a real device.

## Files

- `LevelData.java`: original deterministic multi-floor level geometry and functional world objects
- `GameView.java`: Android Canvas rendering, collision, scrolling, collectibles and checkpoints
- `MainActivity.java`: lifecycle
- `tools/LevelRouteTest.java`: structural regression test

## Public repository, rights and signing

No third-party graphic or audio assets, analytics or network dependencies are added. Project source and visuals remain **all rights reserved** pending the owner's explicit license choice. Public visibility alone does not authorize reuse; see `THIRD_PARTY_NOTICES.md`.

Package: `de.sschermer.kugelspiel`, versionCode **4**. CI produces **debug-signed** APKs, whose signing keys are not stable across GitHub Actions runners; update installs may require uninstalling the previous debug APK. A persistent private keystore must be configured securely for update-compatible release APKs, never checked into GitHub.
