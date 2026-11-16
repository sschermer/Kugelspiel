# Kugelspiel

Native Android 2D puzzle-platformer prototype (Iteration 001).

Development status: work in progress; APK builds are produced by GitHub Actions.

## Build

Requires JDK 17, Android SDK platform 35, and Gradle 8.10.2.

Run `gradle assembleDebug` (or use the GitHub Actions `Android Debug APK` workflow). The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Controls

Landscape orientation, left/right/jump touch buttons. The prototype uses self-drawn vector graphics without external game assets. The game includes ten levels, hazards, checkpoints and finish markers. The planned color/gravity puzzle mechanics are intentionally deferred.

## Licensing

**All rights reserved** until the project owner selects explicit code and asset licenses. Public repository visibility does not grant redistribution rights. Android SDK and Android Gradle plugin are build-time dependencies; see `THIRD_PARTY_NOTICES.md`.

No production signing keys are stored in the repository. Debug builds use the CI debug key and will not be update-compatible across independent CI machines; stable signing must be configured later.
