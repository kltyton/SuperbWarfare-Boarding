<div align="center">

<img src="docs/publishing/artwork/superb-warfare-boarding-icon-256.png" width="128" alt="Superb Warfare: Boarding Icon" />

# Superb Warfare: Boarding (卓越前线：登乘演出)

**Model-anchored boarding and dismounting animations for rideable Superb Warfare vehicles, powered by [AnimCue](https://github.com/kltyton/AnimCue).**

[English](README.md) | [简体中文](README_zh.md)

</div>

---

## Overview

**Superb Warfare: Boarding** (mod ID: `superbwarfare_boarding`) adds fluid, vehicle-specific boarding and dismounting animations to rideable vehicles from **Superb Warfare**, powered by **AnimCue** (`interactive_animation`).

Right-click any rideable Superb Warfare vehicle as usual:
- **Smart Approach**: Automatically walks for short approaches or sprints athletically for longer distances while navigating around wings, hulls, and nearby vehicles. If the entry path is completely obstructed, the interaction seamlessly falls back to Superb Warfare's native mounting behavior.
- **Model-Anchored Entries & Exits**: Every vehicle class uses trajectories and skeletal poses aligned to its actual stock model geometry—swinging cab doors, steel ladder rungs, metal footsteps, landing gear tires, armored rear doors, turret hatches, wing roots, and fighter canopies.
- **Physical Door & Hatch Closing**: On stationary dismount, players step cleanly down onto safe ground, turn to face open cab/rear doors or hatches, and physically push them shut before regaining normal control.
- **Smooth Camera & Double-Side Rendering**: First-person views smoothly orbit outside the vehicle during boarding/dismounting and return seamlessly to your original perspective. Vehicle model backface culling is temporarily disabled during boarding and dismounting animations so open doors and cabin interiors render cleanly from all angles.
- **Double-Press Fast Skip**: Press right-click (while boarding) or your dismount key (while exiting) a second time to skip directly into or out of the seat.
- **Synchronized Audio**: Keyframe-aligned door/hatch open & close sounds, hull step sounds, seat settling, and ground-material-aware footsteps.

Superb Warfare's native locking, menus, vehicle tools, seats, moving/airborne dismounts, and ejection behaviors remain completely intact.

---

## Vehicle Coverage Highlights

- **Cars & Trucks**: Sodayo Pickup series (`sodayo_pick_up*`), Heavy Truck (`truck`), and FH-77BW self-propelled howitzer (`fh_77bw`, climbing its 4-rung steel cab ladder and opening the front-hinged cab door).
- **IFVs & APCs**: Bradley (`bradley`, rear inset personnel door), LAV-25 & LAV-AD (`lav_25`, `lav_ad`, rear double doors), BMP-2 (`bmp_2`), and LAV-150 (`lav_150`).
- **Tanks & SPGs**: T-90A (`t_90a`), Prism Tank (`prism_tank`), M1A2 (`m_1a_2`), ZTZ-99A (`ztz_99a`), PLZ-05 (`plz_05`), and YX-100 (`yx_100`) with track/fender climbs and animated turret/hull hatches.
- **Helicopters & Aircraft**: Mi-28 (`mi_28`, stepping via the main landing gear tire and sponson through the armored side cockpit door), A-10A (`a_10a`), J-16 (`j_16`), Ju 87 (`ju_87`), AH-6 (`ah_6`), and AC-130H (`ac_130h`).
- **Boats, Airships & Emplacements**: Happiest Ghast (`happiest_ghast`), Kirov (`kirov`), Speedboats, Wheelchairs, and Artillery/Defense emplacements.

---

## Requirements

| Platform | Minecraft | Loader | Required Dependencies | Optional Limb/Torso Bending |
| --- | --- | --- | --- | --- |
| **NeoForge** | `1.21.1` | NeoForge `21.1.248+` | [AnimCue](https://github.com/kltyton/AnimCue) (`interactive_animation >= 0.1.0`), Superb Warfare (`>= 0.8.9.2`), GeckoLib, Kotlin for Forge | Player Animation Library (PAL) + PAL More Rotation |
| **Forge** | `1.20.1` | Forge `47.4.22+` | [AnimCue](https://github.com/kltyton/AnimCue) (`interactive_animation >= 0.1.0`), Superb Warfare (`>= 0.8.9.2`), GeckoLib, Kotlin for Forge, MixinExtras (`>= 0.3.6`, provided by Superb Warfare) | playerAnimator + bendy-lib + PAL More Rotation (Forge) |

All dependencies are installed side-by-side (no Jar-in-Jar bundling).

---

## Building from Source

Run the Gradle wrapper from the target directory you want to build:

```bash
# NeoForge 1.21.1
cd targets/neoforge-1.21.1
../../gradlew build verifyDistribution

# Forge 1.20.1
cd targets/forge-1.20.1
../../gradlew build verifyDistribution
```

Optional Gradle properties for local builds:
- `-PinteractiveAnimationJar=<path>` — point to a local AnimCue JAR for the matching loader/version.
- `-PsuperbWarfareJar=<path>` — point to a local Superb Warfare JAR.
- `-PpalMoreJar=<path>` — point to a local PAL More Rotation Forge 1.20.1 JAR when compiling the Forge target.

---

## License

Licensed under the **GNU Lesser General Public License v3.0 or later** ([LGPL-3.0-or-later](LICENSE)).
All vehicle models and textures are provided by your installed copy of Superb Warfare.
