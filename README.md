# Kugelspiel

Native Android puzzle-platformer — **Iteration 005, version 0.5.0**.

## Controls

Exactly three multitouch buttons: **left**, **right**, **jump**. Hold left and right together for 2.5 seconds to restart the current level. Progress for the new mechanics is saved separately from iteration 004.

## New mechanics

### Jump chains (explicit owner-approved rules)

- Normal jump starts at **100% HEIGHT**.
- When landing after a jump, the ball performs **one small automatic intermediate hop**.
- Press jump during this short **0.36-second** timing window to turn the next jump into **150% height**.
- Repeat on subsequent landings to stack up to **200%, then 250% height** (three boosts).
- **Unlimited further chains** are allowed, but height stays capped at **250%**.
- On ANY ceiling/head collision, jump energy is **immediately reset to baseline 100%** (jump height 1).
- Missing the hop window ends the chain on landing. A boost can be rebuilt by jumping again.
- The code scales launch velocity by square root of the height factor; the physical apex, not launch speed, is the percentage.
- HUD shows the active jump height and the ball shows one illuminated segment per boost.

### Helium stations

- Cyan **HE+** filling recess; amber **HE-** draining recess. Both functional apparatuses, not loose pickups.
- The metallic ball must be centered **within 10 game units horizontally** and **19 vertically** and nearly stationary (speed under 27 horizontal and 30 vertical units/second), for **0.55 seconds**. The apparatus visually charges before it activates.
- When filled, the ball **automatically rises slowly**, with left/right steering; jump stacking is disabled until helium is drained.
- A matching draining socket reverts the ball to normal gravity/rolling.
- The filling apparatus **requires points collected in earlier rooms** (its threshold is displayed when still locked). Points are permissions and are not spent.
- Apparatuses remain reusable; the new level layout supports alternative routes and safe passages.

### More deliberate maze geometry

- Floor/ceiling collision strips are **19** units thick instead of an entire 58-unit tile.
- Larger, multi-floor scrolling mazes have partial platforms with a **missing stair**, creating a choice between a lower route and chaining jumps upward.
- Several areas contain **score-based intermediate doors**, with live **NOCH X** indicators. Points are NOT deducted and missing points can be collected by backtracking.
- Turquoise arrowed passages remain safe; red warning pits are fatal.
- Existing points stay black with white numerals until collected, then semi-transparent white with black numerals. Each point token awards only once.

## Test/build

JDK 17, Android SDK 35, Gradle 8.10.2.

`gradle assembleDebug` builds an Android APK in `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions tests both the maze structure and the pure-Java bounce-chain state machine before building and uploading `kugelspiel-iteration-005-debug`.

Manually run the tests:

```sh
mkdir -p out
javac -encoding UTF-8 -d out app/src/main/java/de/sschermer/kugelspiel/LevelData.java tools/LevelRouteTest.java app/src/main/java/de/sschermer/kugelspiel/BounceChain.java tools/BounceChainTest.java
java -cp out de.sschermer.kugelspiel.LevelRouteTest
java -cp out de.sschermer.kugelspiel.BounceChainTest
```

The checks cover map connectivity, safe vs fatal gaps, available score, point-gated helium and score doors, matching stations, and jump stacking/reset math. **They do not simulate real touch input, actual physics-driven reachability, or prove all player routes are free of softlocks. Device playtesting remains necessary.**

## Rights and security

The app does not bundle third-party gameplay assets or libraries. Drawings use Android Canvas. Project rights are retained pending the owner's explicit code/asset license decision; public GitHub visibility does not confer reuse rights. See `THIRD_PARTY_NOTICES.md`.

Package ID remains `de.sschermer.kugelspiel`, versionCode **5**. GitHub Actions debug builds do not retain a stable signing key between runs; updating over an older debug APK can fail. Configure persistent private signing before offering update-compatible releases, never commit the keystore to the public repository.
