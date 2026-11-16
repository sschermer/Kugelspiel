# Kugelspiel

Native Android puzzle-platformer — **Iteration 007 / version 0.7.0**.

## Individually designed worlds, not repeated templates

Stages 1–3 remain tutorials. **Stages 4–6 now come from individual level-design methods** rather than the regular stage generator.

| Stage | Concept | Map |
|---|---|---|
| 4: Verzweigte Höhlen | Loops, broad cave collapses, orange and blue color paths, rock obstacles and an optional 16-point chamber | 3 storeys, 56 tiles wide, 8 independently placed vertical openings, 29 points required |
| 5: Der Sprungturm | Vertical exploration, a missing stair, 10-point bonus platforms, 18-point door, optional helium travel | 4 storeys, 66 tiles wide, multiple offset passage openings, 36 points required |
| 6: Zwei Welten | Two separate horizontal regions, 28-point interdoors on all four floors; return to the left exit after collecting right-side points | 4 storeys, 78 tiles wide, 48-point exit, optional helium and a lethal pit |

The three worlds contain genuine full-height rock blocks as well as intermediate functional platforms. Each has **hand-placed galleries with sections of the intermediate floor missing**. These large turquoise-marked safe openings are a different width from the normal three-tile connectors. The stage structure is driven by its puzzle, not by uniform item spacing.

Stages 7–10 deliberately retain their existing layouts while stages 4–6 undergo device playtesting. They will be redesigned after this design direction has been tested.

## Geometry and mechanics

- Shared floor/ceiling dividers have **19 units** of actual collision and visual thickness.
- Safe passages are turquoise with an arrow and the word DURCHGANG; lethal pits are dark red with spikes and the word GEFAHR.
- All color gates and point interdoors reach from floor to ceiling and cannot be bypassed by stacked jumps.
- Three simultaneous touch buttons: left, right and jump.
- Metallic ball, reusable gravity switches, and clearly distinguished blue/orange/violet color pickups.
- Jump height can stack from 100% to **150%, 200%, 250%**; unlimited further chains stay at 250%. Ceiling contact resets the boost to 100%.
- Helium filling/draining sockets require centering and low speed. A filling socket can demand a minimum score.
- Point tokens (1, 5, 10): black with a white number when available; translucent white with a black number when already collected. Points are awarded only once and are never spent.
- Exits and intermediate doors show the number of missing points. Checkpoints and stage progress remain active.
- Holding left and right together for 2.5 seconds resets the current stage.

Saved progress remains on the same stage key as in Iteration 005, so it is not intentionally reset.

## Building and testing

Requirements: JDK 17, Gradle 8.10.2, Android SDK 35. GitHub Actions builds the artifact **kugelspiel-iteration-007-debug**. The workflow compiles the pure-Java level and jump tests, then packages a debug APK.

Source layout: LevelData.java (geometry), HandcraftedLevels.java (three individually designed stages), GameView.java (Android Canvas and controls), BounceChain.java (stacked jump rules), LevelRouteTest.java and BounceChainTest.java (structural tests).

The tests check map dimensions, floor gaps, full-room gate heights, unique stage structure, score-gate requirements, and jump-state rules. **They do not simulate a complete real-device run and cannot guarantee that every route is feasible or free of softlocks.** Device playtesting is necessary.

## Public repo, rights and signing

No external game artwork or audio was added. Original Canvas graphics are created in code. All project code and artwork remain owner-controlled with no reuse license selected. Public repository visibility does not grant reuse rights; consult THIRD_PARTY_NOTICES.md.

App ID remains de.sschermer.kugelspiel, versionCode 7. GitHub Actions debug signing may differ between builds, so updates might require removing an older debug APK. Persistent signing keys must not be committed to the public repository.
