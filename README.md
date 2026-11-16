# Kugelspiel

Native Android 2D **logic puzzle-platformer** (Iteration 002, version 0.2.0).

**Design direction:** approximately 80% thinking, 20% dexterity. The current ten hand-authored test levels emphasize return routes and state combinations, not timing, spikes, falls or speedruns.

## Mechanics

- Three simultaneous touch controls: left, right, jump. Landscape mode.
- Metallic rolling sphere with gentle acceleration, jump buffering and coyote time.
- Color pickups set permanent **Blue**, **Orange** or **Violet** state. A wall of the same color becomes passable. Both pickups and gates carry letter symbols (B/O/V), not color alone.
- Gravity pickups explicitly set **Up** or **Down**. The sphere then rolls along the ceiling or floor. Arrow symbols distinguish these items.
- Pickups never disappear and activate whenever touched again. Players can reverse direction to revisit earlier pickups.
- Some color walls cover the entire corridor; others block only the floor or ceiling route. Ten levels include deliberate backtracking challenges.
- Progress is stored locally after each solved level. The goal is the vertical green portal.

There are no ads, analytics, network permissions, imported art, third-party game assets or external game libraries.

## Build and tests

Requires JDK 17, Gradle 8.10.2, Android SDK platform 35.

`gradle assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow runs a **pure Java symbolic route test** before packaging the APK. To run the test manually:

```sh
mkdir -p out
javac -encoding UTF-8 -d out app/src/main/java/de/sschermer/kugelspiel/LevelData.java tools/LevelRouteTest.java
java -cp out de.sschermer.kugelspiel.LevelRouteTest
```

The symbolic solver checks reachability through the two lanes, permanent color/gravity states, gates and repeatable items. It **does not** prove real-device playability, rendering, or frame-perfect collision behavior; those need manual device tests.

## Architecture

- `LevelData.java`: independently testable puzzle data and gate/item semantics.
- `GameView.java`: Android Canvas presentation, fixed-step character motion, persistent pickups and touch handling.
- `MainActivity.java`: Android lifecycle.

## Licensing and signing

**All rights reserved** until the owner explicitly approves the code and asset licensing. Public repository visibility alone does not authorize redistribution. Build dependencies are described in `THIRD_PARTY_NOTICES.md`.

The package ID remains `de.sschermer.kugelspiel`. Version codes increase for later releases. These GitHub Actions debug APKs are signed with runner-generated debug keys that can change between runs; to update an installed APK in place, a persistent private signing keystore must be configured securely. Never commit private keys.

## Known limitations

- No automated Android device/emulator gameplay test yet.
- Mechanical, visual, difficulty and actual softlock testing on different phones is still required.
- Route test uses a simplified lane-based graph and does not model jumping between tracks or off-track gate contacts.
