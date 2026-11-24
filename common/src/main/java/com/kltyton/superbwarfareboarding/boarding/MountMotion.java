package com.kltyton.superbwarfareboarding.boarding;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Vehicle-local physics-aware trajectories for boarding and dismounting.
 * Anchors every step onto verified .geo.json vehicle surfaces (sills, metal steps,
 * ladder rungs, landing gear tires, sponsons, fenders, and wing roots), eliminates
 * mid-air levitation and moon-gravity hops, and keeps dismount waypoints on the exact
 * unified [0..1] animation timeline so hands physically open and close every door/hatch.
 */
final class MountMotion {
    enum MotionMode {
        /** Smooth ease-in/out for stationary holds, seated transitions, or door pushing. */
        SMOOTH,
        /** Continuous steady stride across flat decks, wings, steps, or ground. */
        WALK,
        /** Hugging exterior climb: rises cleanly along the model edge onto the surface without floating away. */
        CLIMB_UP,
        /** Crisp gravity drop off an elevated fender/wing (dy > 0.80) with ZERO upward levitation. */
        JUMP_DOWN,
        /** Center over hatch/cockpit horizontally, then drop straight into seat. */
        DROP_IN
    }

    private final List<Contact> contacts;

    private MountMotion(List<Contact> contacts) {
        this.contacts = contacts;
    }

    /** Plans the boarding trajectory from vehicle-local ground/platform start into the seat/interior. */
    static MountMotion plan(BoardingProfile profile, CabDoor door, AABB hull, Vec3 start, Vec3 seat, EntryPortal portal) {
        Vec3 opening = portal == null ? seat : portal.lip();
        Vec3 destination = portal != null && portal.interior() != null ? portal.interior() : seat;
        Vec3 edge = sill(hull, start, opening);
        double rise = Math.max(0, destination.y - start.y);
        double lipRise = Math.max(0, opening.y - start.y);

        // Hinged cab doors: pickup, truck, fh_77bw
        if (door != null && portal != null) {
            if (lipRise > 2.65 || start.y - opening.y > 0.75) return null;
            Vec3 stepBack = portal.exteriorStep() != null ? portal.exteriorStep()
                    : start.add(Math.copySign(0.26, start.x), 0, -0.22);
            Vec3 foothold = portal.foothold();
            double sideSign = Math.signum(start.x);
            if (sideSign == 0) sideSign = Math.signum(opening.x);

            // FH-77BW high cab: climb exact 4 steel ladder rungs (tizi/tizi2 at Z=4.13) -> pull door open -> step in
            if (Math.abs(opening.z) > 4.2 && opening.y > 2.0) {
                Vec3 sillPoint = new Vec3(opening.x, Math.max(start.y, opening.y), opening.z);
                if (lipRise > 1.25) {
                    Vec3 ladderBase = new Vec3(sideSign * 2.62, start.y, 4.13);
                    Vec3 rung1 = new Vec3(sideSign * 2.35, Math.max(start.y, 0.65), 4.13);
                    Vec3 rung2 = new Vec3(sideSign * 2.14, Math.max(start.y, 0.98), 4.13);
                    Vec3 rung3 = new Vec3(sideSign * 1.94, Math.max(start.y, 1.30), 4.13);
                    Vec3 rung4 = new Vec3(sideSign * 1.82, Math.max(start.y, 1.63), 4.13);
                    return new MountMotion(List.of(
                            new Contact(0.00, start, MotionMode.SMOOTH),
                            new Contact(0.08, ladderBase, MotionMode.WALK),
                            new Contact(0.17, rung1, MotionMode.WALK),
                            new Contact(0.27, rung2, MotionMode.WALK),
                            new Contact(0.37, rung3, MotionMode.WALK),
                            new Contact(0.46, rung4, MotionMode.WALK),
                            new Contact(0.66, rung4, MotionMode.SMOOTH),
                            new Contact(0.77, sillPoint, MotionMode.CLIMB_UP),
                            new Contact(0.86, destination, MotionMode.SMOOTH),
                            new Contact(1.00, destination, MotionMode.SMOOTH)));
                }
                Vec3 atHandle = new Vec3(sideSign * 2.06, start.y, 4.10);
                Vec3 clearDoor = new Vec3(sideSign * 2.52, start.y, 3.88);
                return new MountMotion(List.of(
                        new Contact(0.00, start, MotionMode.SMOOTH),
                        new Contact(0.16, atHandle, MotionMode.WALK),
                        new Contact(0.42, clearDoor, MotionMode.SMOOTH),
                        new Contact(0.56, atHandle, MotionMode.WALK),
                        new Contact(0.70, sillPoint, MotionMode.WALK),
                        new Contact(0.84, destination, MotionMode.SMOOTH),
                        new Contact(1.00, destination, MotionMode.SMOOTH)));
            }

            // Sodayo pickup cab: step to closed handle -> pull 74 deg door open while stepping clear ->
            // step directly onto real cab floor bottom#0 (Y=0.59, ZERO air levitation!) -> duck & sit -> close door.
            if (lipRise < 0.75) {
                Vec3 atHandle = new Vec3(sideSign * 1.54, start.y, 0.76);
                Vec3 stance = foothold != null ? new Vec3(foothold.x, start.y, foothold.z) : new Vec3(sideSign * 1.50, start.y, 0.74);
                Vec3 entry = new Vec3(opening.x, Math.max(start.y, opening.y), opening.z);
                return new MountMotion(List.of(
                        new Contact(0.00, start, MotionMode.SMOOTH),
                        new Contact(0.15, atHandle, MotionMode.WALK),
                        new Contact(0.42, new Vec3(stepBack.x, start.y, stepBack.z), MotionMode.SMOOTH),
                        new Contact(0.54, stance, MotionMode.WALK),
                        new Contact(0.68, entry, MotionMode.WALK),
                        new Contact(0.84, destination, MotionMode.SMOOTH),
                        new Contact(1.00, destination, MotionMode.SMOOTH)));
            }

            // Truck cab: pull 86 deg door open -> step onto EXACT metal footstep head#24 (|X|=1.24, Y=0.72, Z=4.16) ->
            // step up onto real cab floor head#142 (|X|=0.96, Y=1.32, Z=3.88) -> sit -> close door.
            Vec3 step1 = foothold != null
                    ? new Vec3(foothold.x, Math.max(start.y, foothold.y), foothold.z)
                    : new Vec3(sideSign * 1.24, Math.max(start.y, 0.72), 4.16);
            Vec3 sillPoint = new Vec3(opening.x, Math.max(start.y, opening.y), opening.z);
            Vec3 atHandle = new Vec3(sideSign * 1.66, start.y, 3.44);
            Vec3 stepBase = new Vec3(step1.x + Math.copySign(0.42, step1.x), start.y, step1.z);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.15, atHandle, MotionMode.WALK),
                    new Contact(0.42, new Vec3(stepBack.x, start.y, stepBack.z), MotionMode.SMOOTH),
                    new Contact(0.54, stepBase, MotionMode.WALK),
                    new Contact(0.66, step1, MotionMode.WALK),
                    new Contact(0.77, sillPoint, MotionMode.CLIMB_UP),
                    new Contact(0.86, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Bradley inset rear personnel door (left-hinged at X=0.054, Z=-3.44, sill body#12 Y=0.92)
        if (profile == BoardingProfile.BRADLEY_DOOR && portal != null) {
            if (lipRise > 1.25 || start.y - opening.y > 0.50 || rise > 2.30) return null;
            Vec3 atDoor = new Vec3(0.86, start.y, opening.z - 0.64);
            Vec3 stepBack = portal.exteriorStep() != null
                    ? new Vec3(portal.exteriorStep().x, start.y, portal.exteriorStep().z)
                    : new Vec3(0.84, start.y, opening.z - 1.02);
            Vec3 alignDoor = new Vec3(opening.x, start.y, opening.z - 0.44);
            Vec3 insideStep = portal.foothold() != null ? portal.foothold()
                    : new Vec3(opening.x, opening.y, opening.z + 0.42);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.14, atDoor, MotionMode.WALK),
                    new Contact(0.36, stepBack, MotionMode.SMOOTH),
                    new Contact(0.46, alignDoor, MotionMode.WALK),
                    new Contact(0.58, opening, MotionMode.WALK),
                    new Contact(0.72, insideStep, MotionMode.WALK),
                    new Contact(0.84, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // LAV-25 / LAV-AD side-hinged rear double doors (sill low#2/#9 Y=0.96, Z=-3.38)
        if (profile == BoardingProfile.RAMP && portal != null) {
            if (lipRise > 1.50 || start.y - opening.y > 0.55) return null;
            Vec3 atDoor = new Vec3(opening.x, start.y, opening.z - 1.12);
            Vec3 stepBack = portal.exteriorStep() != null
                    ? new Vec3(portal.exteriorStep().x, start.y, portal.exteriorStep().z)
                    : new Vec3(opening.x, start.y, opening.z - 1.40);
            Vec3 alignDoor = new Vec3(opening.x, start.y, opening.z - 0.46);
            Vec3 sillStep = new Vec3(opening.x, Math.max(start.y, opening.y), opening.z);
            Vec3 insideBay = portal.foothold() != null ? portal.foothold()
                    : new Vec3(opening.x, opening.y, opening.z + 0.46);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.14, atDoor, MotionMode.WALK),
                    new Contact(0.36, stepBack, MotionMode.SMOOTH),
                    new Contact(0.46, alignDoor, MotionMode.WALK),
                    new Contact(0.58, sillStep, MotionMode.WALK),
                    new Contact(0.72, insideBay, MotionMode.WALK),
                    new Contact(0.84, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Ju 87 dive-bomber: flush against trailing wing edge -> hoist directly onto wing -> walk to cockpit -> drop in
        if (profile == BoardingProfile.JU87_COCKPIT && portal != null && portal.exteriorStep() != null && portal.foothold() != null) {
            Vec3 trailingStep = portal.exteriorStep();
            Vec3 wingDeck = portal.foothold();
            if (wingDeck.y - start.y > 1.95 || start.y > wingDeck.y + 0.55) return null;
            Vec3 atWingEdge = new Vec3(trailingStep.x, start.y, trailingStep.z - 0.34);
            Vec3 onWing = new Vec3(trailingStep.x, Math.max(start.y, trailingStep.y), trailingStep.z + 0.10);
            Vec3 midWing = onWing.lerp(new Vec3(wingDeck.x, Math.max(start.y, wingDeck.y), wingDeck.z), 0.52);
            Vec3 besideCockpit = new Vec3(wingDeck.x, Math.max(start.y, wingDeck.y), wingDeck.z);
            Vec3 overRim = new Vec3(opening.x, Math.max(wingDeck.y + 0.14, opening.y), opening.z);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.10, atWingEdge, MotionMode.WALK),
                    new Contact(0.28, onWing, MotionMode.CLIMB_UP),
                    new Contact(0.39, midWing, MotionMode.WALK),
                    new Contact(0.50, besideCockpit, MotionMode.WALK),
                    new Contact(0.64, besideCockpit, MotionMode.SMOOTH),
                    new Contact(0.75, overRim, MotionMode.CLIMB_UP),
                    new Contact(0.88, destination, MotionMode.DROP_IN),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Multi-stage tank / APC hatch climb (boots touch roadwheel/skirt edge -> fender top -> turret roof -> hatch!)
        if ((profile == BoardingProfile.T90_HATCH || profile == BoardingProfile.HATCH)
                && portal != null && portal.exteriorStep() != null && portal.foothold() != null) {
            Vec3 fender = portal.exteriorStep();
            Vec3 deck = portal.foothold();
            if (opening.y - start.y > 3.60 || start.y > opening.y + 0.70) return null;
            double approachY = start.y;
            double fenderY = Math.max(approachY, fender.y);
            double deckY = Math.max(fenderY, deck.y);
            double lipY = Math.max(deckY, opening.y);
            double sideSign = Math.signum(start.x - opening.x);
            if (sideSign == 0) sideSign = Math.signum(fender.x);

            // Stand flush beside track/skirt (NOT 0.90m out in empty air!), step onto wheel/skirt edge, then hoist onto fender!
            double hugX = sideSign * (Math.abs(fender.x) + 0.60);
            Vec3 atTrack = new Vec3(hugX, approachY, fender.z);
            Vec3 skirtFoot = new Vec3(sideSign * (Math.abs(fender.x) + 0.36), Mth.lerp(0.52, approachY, fenderY), fender.z);
            Vec3 onFender = new Vec3(fender.x, fenderY, fender.z);

            boolean climbsTurret = deckY - fenderY > 0.35 && Math.abs(deck.z - fender.z) > 0.75;
            Vec3 turretBase = climbsTurret
                    ? new Vec3(fender.x, fenderY, Mth.lerp(0.74, fender.z, deck.z))
                    : onFender.lerp(new Vec3(deck.x, deckY, deck.z), 0.50);
            Vec3 besideHatch = new Vec3(deck.x, deckY, deck.z);
            Vec3 overHatch = new Vec3(opening.x, lipY, opening.z);
            Vec3 dropInside = new Vec3(destination.x, destination.y, destination.z);

            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.08, atTrack, MotionMode.WALK),
                    new Contact(0.17, skirtFoot, MotionMode.WALK),
                    new Contact(0.26, onFender, MotionMode.CLIMB_UP),
                    new Contact(0.40, turretBase, climbsTurret ? MotionMode.WALK : MotionMode.CLIMB_UP),
                    new Contact(0.52, besideHatch, climbsTurret ? MotionMode.CLIMB_UP : MotionMode.WALK),
                    new Contact(0.64, besideHatch, MotionMode.SMOOTH),
                    new Contact(0.74, overHatch, MotionMode.WALK),
                    new Contact(0.86, dropInside, MotionMode.DROP_IN),
                    new Contact(1.00, dropInside, MotionMode.SMOOTH)));
        }

        // Fighter / gunship cockpit climb (A-10A wing root, J-16 LERX, Mi-28 exact wheel gear2#4 -> sponson bone2#66 -> sill)
        if (profile == BoardingProfile.COCKPIT && portal != null && portal.exteriorStep() != null && portal.foothold() != null) {
            Vec3 step = portal.exteriorStep();
            Vec3 sillHold = portal.foothold();
            if (opening.y - start.y > 2.85 || start.y > opening.y + 0.65) return null;
            double sideSign = Math.signum(start.x - opening.x);
            if (sideSign == 0) sideSign = Math.signum(step.x);
            Vec3 midStep = new Vec3(step.x, Math.max(start.y, step.y), step.z);
            Vec3 upperStep = new Vec3(sillHold.x, Math.max(midStep.y, sillHold.y), sillHold.z);
            Vec3 overRim = new Vec3(opening.x, Math.max(upperStep.y, opening.y), opening.z);

            if (opening.z - sillHold.z > 1.0) {
                // Wing / LERX walk fighter (A-10A / J-16): stand RIGHT against wing edge (NOT 2m away!), hoist directly onto wing!
                double zOff = start.z >= step.z ? 0.42 : -0.42;
                Vec3 atEdge = Math.abs(start.x - step.x) > 0.35
                        ? new Vec3(step.x + sideSign * 0.36, start.y, step.z)
                        : new Vec3(step.x, start.y, step.z + zOff);
                double shoulderX = Math.abs(sillHold.x) < 1.05
                        ? Math.copySign(0.84, sillHold.x)
                        : Math.copySign(1.02, sillHold.x);
                double deckTopY = Math.max(upperStep.y, overRim.y - 0.16);
                Vec3 midDeck = new Vec3(
                        Mth.lerp(0.55, upperStep.x, shoulderX),
                        Mth.lerp(0.55, upperStep.y, deckTopY),
                        Mth.lerp(0.55, upperStep.z, opening.z - 0.22));
                Vec3 besideCockpit = new Vec3(shoulderX, deckTopY, opening.z - 0.22);
                return new MountMotion(List.of(
                        new Contact(0.00, start, MotionMode.SMOOTH),
                        new Contact(0.08, atEdge, MotionMode.WALK),
                        new Contact(0.24, midStep, MotionMode.CLIMB_UP),
                        new Contact(0.34, upperStep, midStep.y + 0.30 < upperStep.y ? MotionMode.CLIMB_UP : MotionMode.WALK),
                        new Contact(0.43, midDeck, MotionMode.WALK),
                        new Contact(0.52, besideCockpit, MotionMode.WALK),
                        new Contact(0.64, besideCockpit, MotionMode.SMOOTH),
                        new Contact(0.75, overRim, MotionMode.CLIMB_UP),
                        new Contact(0.87, destination, MotionMode.DROP_IN),
                        new Contact(1.00, destination, MotionMode.SMOOTH)));
            }

            // Mi-28 side cockpit door: exact wheel tire gear2#4 (-0.98, 0.54, 1.32) -> sponson bone2#66 (-0.94, 1.02, 1.34)
            // -> pull door open (0.40..0.58) -> step onto cockpit sill (-0.74, 1.48) -> sit!
            Vec3 atWheel = new Vec3(step.x + sideSign * 0.36, start.y, step.z);
            Vec3 sillClimb = new Vec3(opening.x, Math.max(upperStep.y, opening.y), opening.z);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.08, atWheel, MotionMode.WALK),
                    new Contact(0.22, midStep, MotionMode.WALK),
                    new Contact(0.40, upperStep, MotionMode.WALK),
                    new Contact(0.58, upperStep, MotionMode.SMOOTH),
                    new Contact(0.72, sillClimb, MotionMode.WALK),
                    new Contact(0.84, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Tall vertical wall / ghast climb (happiest_ghast clear rear-side wall Z=-1.55, 0.80m behind side cannon!)
        if (profile == BoardingProfile.GHAST_CLIMB && portal != null) {
            if (lipRise > 9.00 || start.y - opening.y > 0.85) return null;
            double sideSign = Math.signum(start.x);
            if (sideSign == 0) sideSign = Math.signum(opening.x);
            double wallX = portal.exteriorStep() != null ? portal.exteriorStep().x : sideSign * 2.36;
            double wallZ = portal.exteriorStep() != null ? portal.exteriorStep().z : -1.55;
            double topY = Math.max(start.y, opening.y);
            Vec3 wallBase = new Vec3(wallX + sideSign * 0.10, start.y, wallZ);
            Vec3 rung1 = new Vec3(wallX, start.y + (topY - start.y) * 0.28, wallZ);
            Vec3 rung2 = new Vec3(wallX, start.y + (topY - start.y) * 0.56, wallZ);
            Vec3 rung3 = new Vec3(wallX, start.y + (topY - start.y) * 0.84, wallZ);
            Vec3 topRim = portal.foothold() != null
                    ? new Vec3(portal.foothold().x, Math.max(rung3.y, portal.foothold().y), portal.foothold().z)
                    : new Vec3(wallX, topY - 0.14, wallZ);
            Vec3 onBack = new Vec3(opening.x, topY, wallZ);
            Vec3 behindSeats = new Vec3(destination.x, topY, Math.min(wallZ + 0.25, destination.z));
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.08, wallBase, MotionMode.WALK),
                    new Contact(0.24, rung1, MotionMode.WALK),
                    new Contact(0.40, rung2, MotionMode.WALK),
                    new Contact(0.56, rung3, MotionMode.WALK),
                    new Contact(0.66, topRim, MotionMode.WALK),
                    new Contact(0.76, onBack, MotionMode.CLIMB_UP),
                    new Contact(0.86, behindSeats, MotionMode.WALK),
                    new Contact(0.95, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Deck / Dock / Platform entry
        if (profile == BoardingProfile.DECK && portal != null) {
            if (lipRise > 1.65 || start.y - opening.y > 0.85) return null;
            if (portal.exteriorStep() != null && portal.foothold() != null) {
                Vec3 step = new Vec3(portal.exteriorStep().x, Math.max(start.y, portal.exteriorStep().y), portal.exteriorStep().z);
                Vec3 mid = new Vec3(portal.foothold().x, Math.max(start.y, portal.foothold().y), portal.foothold().z);
                return new MountMotion(List.of(
                        new Contact(0.00, start, MotionMode.SMOOTH),
                        new Contact(0.18, step, MotionMode.CLIMB_UP),
                        new Contact(0.44, opening, MotionMode.WALK),
                        new Contact(0.70, mid, MotionMode.WALK),
                        new Contact(0.88, destination, MotionMode.SMOOTH),
                        new Contact(1.00, destination, MotionMode.SMOOTH)));
            }
            if (portal.exteriorStep() != null) {
                Vec3 step = new Vec3(portal.exteriorStep().x, Math.max(start.y, portal.exteriorStep().y), portal.exteriorStep().z);
                return new MountMotion(List.of(
                        new Contact(0.00, start, MotionMode.SMOOTH),
                        new Contact(0.24, step, MotionMode.CLIMB_UP),
                        new Contact(0.54, opening, MotionMode.WALK),
                        new Contact(0.84, destination, MotionMode.SMOOTH),
                        new Contact(1.00, destination, MotionMode.SMOOTH)));
            }
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.14, start, MotionMode.SMOOTH),
                    new Contact(0.50, opening, MotionMode.WALK),
                    new Contact(0.84, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Tall ladder / artillery / airship climb
        if (profile.climbs()) {
            if (rise > 8.40 || lipRise > 8.40) return null;
            double deckHeight = Math.max(start.y, opening.y);
            Vec3 step = portal != null && portal.exteriorStep() != null
                    ? new Vec3(portal.exteriorStep().x, Math.max(start.y, Math.min(portal.exteriorStep().y, start.y + (deckHeight - start.y) * 0.45)), portal.exteriorStep().z)
                    : new Vec3(edge.x, Math.min(deckHeight, start.y + (deckHeight - start.y) * 0.45), edge.z);
            Vec3 upper = portal != null && portal.foothold() != null
                    ? new Vec3(portal.foothold().x, Math.max(step.y, portal.foothold().y), portal.foothold().z)
                    : new Vec3(edge.x, deckHeight, edge.z);
            Vec3 reach = new Vec3(step.x, start.y, step.z);
            Vec3 atOpening = new Vec3(opening.x, Math.max(upper.y, opening.y), opening.z);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.12, reach, MotionMode.WALK),
                    new Contact(0.32, step, MotionMode.CLIMB_UP),
                    new Contact(0.52, upper, MotionMode.CLIMB_UP),
                    new Contact(0.68, atOpening, MotionMode.WALK),
                    new Contact(0.88, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }

        // Low step-in / open side seat / chair
        if (rise > 1.35 || lipRise > 1.45) return null;
        if (profile == BoardingProfile.CHAIR) {
            Vec3 chairStep = start.lerp(destination, 0.58);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.14, start, MotionMode.SMOOTH),
                    new Contact(0.46, new Vec3(chairStep.x, start.y, chairStep.z), MotionMode.WALK),
                    new Contact(0.82, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }
        if (portal != null && portal.exteriorStep() != null) {
            Vec3 step = new Vec3(portal.exteriorStep().x, Math.max(start.y, portal.exteriorStep().y), portal.exteriorStep().z);
            return new MountMotion(List.of(
                    new Contact(0.00, start, MotionMode.SMOOTH),
                    new Contact(0.12, start, MotionMode.SMOOTH),
                    new Contact(0.36, step, MotionMode.WALK),
                    new Contact(0.60, opening, MotionMode.WALK),
                    new Contact(0.86, destination, MotionMode.SMOOTH),
                    new Contact(1.00, destination, MotionMode.SMOOTH)));
        }
        Vec3 threshold = portal != null ? opening : new Vec3(edge.x, start.y + Math.min(rise, 0.30), edge.z);
        return new MountMotion(List.of(
                new Contact(0.00, start, MotionMode.SMOOTH),
                new Contact(0.14, start, MotionMode.SMOOTH),
                new Contact(0.50, threshold, MotionMode.WALK),
                new Contact(0.84, destination, MotionMode.SMOOTH),
                new Contact(1.00, destination, MotionMode.SMOOTH)));
    }

    /**
     * Plans a dedicated natural dismount trajectory on the EXACT unified [0..1] animation timeline!
     * [0..releaseFrac] holds at origin while opening door/hatch from inside;
     * every subsequent waypoint matches the exact keyframe timestamp in dismount.animation.json so:
     *   - zero floating big jumps,
     *   - boots step onto real .geo.json sills/steps/rungs/wheels/fenders,
     *   - hands physically push every door/hatch/canopy shut while facing it!
     */
    static MountMotion planDismount(BoardingProfile profile, CabDoor door, AABB hull, Vec3 seat, Vec3 landing,
                                    EntryPortal portal, double releaseFrac) {
        Vec3 origin = portal != null && portal.interior() != null ? portal.interior() : seat;
        Vec3 opening = portal == null ? seat : portal.lip();
        double drop = Math.max(0, opening.y - landing.y);
        double sideSign = Math.signum(landing.x - opening.x);
        if (sideSign == 0) sideSign = Math.signum(landing.x);
        if (sideSign == 0) sideSign = 1;

        // 1. Tanks & APC top hatches (release=0.16):
        // 0..0.16 open hatch inside -> 0.28 climb to rim -> 0.38 step beside hatch & face hatch ->
        // 0.38..0.52 close hatch -> 0.60..0.76 walk to outer fender edge -> 0.76..0.89 straight drop right beside hull -> 1.00 cushion!
        if ((profile == BoardingProfile.T90_HATCH || profile == BoardingProfile.HATCH)
                && portal != null && portal.exteriorStep() != null && portal.foothold() != null) {
            if (opening.y - landing.y > 3.60 || landing.y > opening.y + 0.70) return null;
            Vec3 deck = portal.foothold();
            Vec3 fender = portal.exteriorStep();
            double deckY = Math.max(landing.y, deck.y);
            double fenderY = Math.max(landing.y, fender.y);
            Vec3 rim = new Vec3(opening.x, Math.max(deckY, opening.y), opening.z);
            Vec3 besideHatch = new Vec3(deck.x, deckY, deck.z);
            double fSign = Math.signum(fender.x) != 0 ? Math.signum(fender.x) : sideSign;
            Vec3 fenderOuter = new Vec3(fSign * (Math.abs(fender.x) + 0.26), fenderY, fender.z);
            boolean hasTwoTiers = deckY - fenderY > 0.35;
            if (hasTwoTiers) {
                Vec3 turretRim = new Vec3(fender.x, deckY, besideHatch.z);
                Vec3 sideFender = new Vec3(fender.x, fenderY, Mth.lerp(0.35, besideHatch.z, fender.z));
                return new MountMotion(List.of(
                        new Contact(0.00, origin, MotionMode.SMOOTH),
                        new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                        new Contact(0.28, rim, MotionMode.CLIMB_UP),
                        new Contact(0.38, besideHatch, MotionMode.WALK),
                        new Contact(0.52, besideHatch, MotionMode.SMOOTH),     // facing hatch, hand closes lid!
                        new Contact(0.60, turretRim, MotionMode.WALK),
                        new Contact(0.68, sideFender, MotionMode.WALK),        // step cleanly onto side fender
                        new Contact(0.76, fenderOuter, MotionMode.WALK),       // step to outer fender lip
                        new Contact(0.89, landing, MotionMode.JUMP_DOWN),      // crisp straight gravity drop (ZERO hopLift!)
                        new Contact(1.00, landing, MotionMode.SMOOTH)));       // crouch cushion & stand up
            }
            return new MountMotion(List.of(
                    new Contact(0.00, origin, MotionMode.SMOOTH),
                    new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                    new Contact(0.28, rim, MotionMode.CLIMB_UP),
                    new Contact(0.38, besideHatch, MotionMode.WALK),
                    new Contact(0.52, besideHatch, MotionMode.SMOOTH),
                    new Contact(0.74, fenderOuter, MotionMode.WALK),
                    new Contact(0.88, landing, MotionMode.JUMP_DOWN),
                    new Contact(1.00, landing, MotionMode.SMOOTH)));
        }

        // 2. Fighter / gunship / dive-bomber cockpits (release=0.16)
        if ((profile == BoardingProfile.COCKPIT || profile == BoardingProfile.JU87_COCKPIT)
                && portal != null && portal.exteriorStep() != null && portal.foothold() != null) {
            if (opening.y - landing.y > 2.85 || landing.y > opening.y + 0.65) return null;
            Vec3 step = portal.exteriorStep();
            Vec3 sillHold = portal.foothold();
            Vec3 rim = new Vec3(opening.x, Math.max(sillHold.y, opening.y), opening.z);

            if (profile == BoardingProfile.JU87_COCKPIT || opening.z - sillHold.z > 1.0) {
                // Wing / LERX fighters (A-10A, J-16, Ju-87): open canopy -> climb out -> face canopy & close -> walk wing -> drop off!
                double shoulderX = profile == BoardingProfile.JU87_COCKPIT
                        ? sillHold.x
                        : (Math.abs(sillHold.x) < 1.05 ? Math.copySign(0.84, sillHold.x) : Math.copySign(1.02, sillHold.x));
                double deckTopY = Math.max(sillHold.y, rim.y - 0.16);
                Vec3 besideCockpit = new Vec3(
                        shoulderX,
                        deckTopY,
                        profile == BoardingProfile.JU87_COCKPIT ? sillHold.z : opening.z - 0.22);
                Vec3 midDeck = new Vec3(sillHold.x, Math.max(landing.y, sillHold.y), sillHold.z);
                Vec3 wingEdge = new Vec3(step.x, Math.max(landing.y, step.y), step.z);
                return new MountMotion(List.of(
                        new Contact(0.00, origin, MotionMode.SMOOTH),
                        new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                        new Contact(0.28, rim, MotionMode.CLIMB_UP),
                        new Contact(0.38, besideCockpit, MotionMode.WALK),
                        new Contact(0.52, besideCockpit, MotionMode.SMOOTH),   // facing canopy, physically closes it!
                        new Contact(0.64, midDeck, MotionMode.WALK),
                        new Contact(0.75, wingEdge, MotionMode.WALK),
                        new Contact(0.89, landing, MotionMode.JUMP_DOWN),      // crisp drop off wing trailing edge
                        new Contact(1.00, landing, MotionMode.SMOOTH)));
            }

            // Mi-28 side door dismount: stay low inside while door opens (0..0.18) -> slide sideways onto sill ->
            // step onto exact sponson bone2#66 (-0.94, 1.02, 1.34) -> face door & close (0.44..0.60) ->
            // step down onto EXACT landing gear tire gear2#4 (-0.98, 0.54, 1.32) -> step onto ground! ZERO floating jump!
            Vec3 sillExit = new Vec3(opening.x, Math.max(landing.y, opening.y), opening.z);
            Vec3 sponson = new Vec3(sillHold.x, Math.max(landing.y, sillHold.y), sillHold.z);
            Vec3 onWheel = new Vec3(step.x, Math.max(landing.y, step.y), step.z);
            return new MountMotion(List.of(
                    new Contact(0.00, origin, MotionMode.SMOOTH),
                    new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                    new Contact(0.30, sillExit, MotionMode.WALK),
                    new Contact(0.42, sponson, MotionMode.WALK),               // boots squarely ON sponson
                    new Contact(0.58, sponson, MotionMode.SMOOTH),             // facing door, hand closes side door!
                    new Contact(0.74, onWheel, MotionMode.WALK),               // boots squarely ON landing gear tire!
                    new Contact(0.90, landing, MotionMode.WALK),               // step cleanly onto ground beside wheel!
                    new Contact(1.00, landing, MotionMode.SMOOTH)));
        }

        // 2b. Happiest Ghast / high vertical wall dismount (release=0.10): walk to rear-side Z=-1.55 -> climb down clear wall!
        if (profile == BoardingProfile.GHAST_CLIMB && portal != null) {
            if (opening.y - landing.y > 9.00 || landing.y > opening.y + 0.85) return null;
            double sSign = Math.signum(landing.x) != 0 ? Math.signum(landing.x) : Math.signum(opening.x);
            double wallX = portal.exteriorStep() != null ? portal.exteriorStep().x : sSign * 2.36;
            double wallZ = portal.exteriorStep() != null ? portal.exteriorStep().z : -1.55;
            double topY = Math.max(landing.y, opening.y);
            Vec3 behindSeats = new Vec3(origin.x, topY, Math.min(wallZ + 0.25, origin.z));
            Vec3 onBack = new Vec3(opening.x, topY, wallZ);
            Vec3 topRim = portal.foothold() != null
                    ? new Vec3(portal.foothold().x, Math.max(landing.y, portal.foothold().y), wallZ)
                    : new Vec3(wallX, topY - 0.14, wallZ);
            Vec3 rung3 = new Vec3(wallX, landing.y + (topY - landing.y) * 0.78, wallZ);
            Vec3 rung2 = new Vec3(wallX, landing.y + (topY - landing.y) * 0.50, wallZ);
            Vec3 rung1 = new Vec3(wallX, landing.y + (topY - landing.y) * 0.24, wallZ);
            Vec3 wallBase = new Vec3(wallX + sSign * 0.10, landing.y, wallZ);
            return new MountMotion(List.of(
                    new Contact(0.00, origin, MotionMode.SMOOTH),
                    new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                    new Contact(0.18, behindSeats, MotionMode.WALK),
                    new Contact(0.28, onBack, MotionMode.WALK),
                    new Contact(0.38, topRim, MotionMode.WALK),
                    new Contact(0.51, rung3, MotionMode.WALK),
                    new Contact(0.64, rung2, MotionMode.WALK),
                    new Contact(0.77, rung1, MotionMode.WALK),
                    new Contact(0.90, wallBase, MotionMode.WALK),
                    new Contact(1.00, landing, MotionMode.SMOOTH)));
        }

        // 3. Bradley rear personnel door (release=0.16):
        // open door inside -> step cleanly out sill -> step down to ground -> TURN 180° TO FACE DOOR -> push door shut!
        if (profile == BoardingProfile.BRADLEY_DOOR && portal != null) {
            if (drop > 1.35 || landing.y - opening.y > 0.50) return null;
            Vec3 insideStep = portal.foothold() != null ? portal.foothold()
                    : new Vec3(opening.x, opening.y, opening.z + 0.42);
            Vec3 sillPoint = new Vec3(opening.x, Math.max(landing.y, opening.y), opening.z);
            Vec3 doorClear = portal.exteriorStep() != null
                    ? new Vec3(portal.exteriorStep().x, landing.y, portal.exteriorStep().z)
                    : new Vec3(0.84, landing.y, opening.z - 1.02);
            Vec3 pushClose = new Vec3(0.78, landing.y, opening.z - 0.66);
            return new MountMotion(List.of(
                    new Contact(0.00, origin, MotionMode.SMOOTH),
                    new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                    new Contact(0.30, insideStep, MotionMode.WALK),
                    new Contact(0.42, sillPoint, MotionMode.WALK),
                    new Contact(0.56, doorClear, MotionMode.WALK),             // step cleanly down to ground (ZERO jump!)
                    new Contact(0.68, doorClear, MotionMode.SMOOTH),           // turn 180 deg to face open door
                    new Contact(0.88, pushClose, MotionMode.SMOOTH),           // step in pushing door physically shut!
                    new Contact(1.00, landing, MotionMode.SMOOTH)));
        }

        // 4. LAV-25 / LAV-AD rear double doors (release=0.16):
        // open double doors -> step cleanly out sill -> step down to ground -> TURN 180° TO FACE DOORS -> push shut!
        if (profile == BoardingProfile.RAMP && portal != null) {
            if (drop > 1.50 || landing.y - opening.y > 0.55) return null;
            Vec3 insideBay = portal.foothold() != null ? portal.foothold()
                    : new Vec3(opening.x, opening.y, opening.z + 0.46);
            Vec3 sillPoint = new Vec3(opening.x, Math.max(landing.y, opening.y), opening.z);
            Vec3 doorClear = portal.exteriorStep() != null
                    ? new Vec3(portal.exteriorStep().x, landing.y, portal.exteriorStep().z)
                    : new Vec3(opening.x, landing.y, opening.z - 1.40);
            Vec3 pushClose = new Vec3(opening.x, landing.y, opening.z - 1.12);
            return new MountMotion(List.of(
                    new Contact(0.00, origin, MotionMode.SMOOTH),
                    new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                    new Contact(0.30, insideBay, MotionMode.WALK),
                    new Contact(0.42, sillPoint, MotionMode.WALK),
                    new Contact(0.56, doorClear, MotionMode.WALK),             // step cleanly down to ground (ZERO jump!)
                    new Contact(0.68, doorClear, MotionMode.SMOOTH),           // turn 180 deg to face open doors
                    new Contact(0.88, pushClose, MotionMode.SMOOTH),           // step forward pushing doors physically shut!
                    new Contact(1.00, landing, MotionMode.SMOOTH)));
        }

        // 5. Hinged cab doors (pickup, truck, FH-77BW, release=0.16)
        if (door != null && portal != null) {
            if (drop > 2.65 || landing.y - opening.y > 0.75) return null;
            Vec3 sillPoint = new Vec3(opening.x, Math.max(landing.y, opening.y), opening.z);
            double sSign = Math.signum(landing.x) != 0 ? Math.signum(landing.x) : Math.signum(opening.x);

            // FH-77BW: open door -> step onto EXACT top ladder rung #0 (1.82, 1.63, 4.13) -> face door & close -> climb down 4 rungs!
            if (Math.abs(opening.z) > 4.2 && opening.y > 2.0) {
                if (drop > 1.25) {
                    Vec3 rung4 = new Vec3(sSign * 1.82, Math.max(landing.y, 1.63), 4.13);
                    Vec3 rung3 = new Vec3(sSign * 1.94, Math.max(landing.y, 1.30), 4.13);
                    Vec3 rung2 = new Vec3(sSign * 2.14, Math.max(landing.y, 0.98), 4.13);
                    Vec3 rung1 = new Vec3(sSign * 2.35, Math.max(landing.y, 0.65), 4.13);
                    Vec3 ladderBase = new Vec3(sSign * 2.62, landing.y, 4.13);
                    return new MountMotion(List.of(
                            new Contact(0.00, origin, MotionMode.SMOOTH),
                            new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                            new Contact(0.28, sillPoint, MotionMode.WALK),
                            new Contact(0.39, rung4, MotionMode.WALK),         // boots ON top rung #0
                            new Contact(0.54, rung4, MotionMode.SMOOTH),       // facing door, hand closes cab door!
                            new Contact(0.65, rung3, MotionMode.WALK),
                            new Contact(0.76, rung2, MotionMode.WALK),
                            new Contact(0.86, rung1, MotionMode.WALK),
                            new Contact(0.94, ladderBase, MotionMode.WALK),
                            new Contact(1.00, landing, MotionMode.SMOOTH)));
                }
                Vec3 clearDoor = new Vec3(sSign * 2.52, landing.y, 3.88);
                return new MountMotion(List.of(
                        new Contact(0.00, origin, MotionMode.SMOOTH),
                        new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                        new Contact(0.34, sillPoint, MotionMode.WALK),
                        new Contact(0.56, clearDoor, MotionMode.WALK),
                        new Contact(0.82, landing, MotionMode.SMOOTH),
                        new Contact(1.00, landing, MotionMode.SMOOTH)));
            }

            // Sodayo Pickup: open door inside -> step onto real cab floor bottom#0 (1.06, 0.59, 0.72) ->
            // step straight DOWN onto grass right beside sill (1.50, 0, 0.74) [ZERO FLOATING JUMP!] ->
            // step to open door tip (2.10, 0, 0.90) & TURN TO FACE OPEN DOOR -> physically push door shut to (1.76, 0, 0.78)!
            if (drop < 0.75) {
                Vec3 besideSill = portal.foothold() != null
                        ? new Vec3(portal.foothold().x, landing.y, portal.foothold().z)
                        : new Vec3(sSign * 1.50, landing.y, 0.74);
                Vec3 doorEdge = portal.exteriorStep() != null
                        ? new Vec3(portal.exteriorStep().x, landing.y, portal.exteriorStep().z)
                        : new Vec3(sSign * 2.18, landing.y, 0.76);
                Vec3 doorClosed = new Vec3(sSign * 1.82, landing.y, 0.68);
                return new MountMotion(List.of(
                        new Contact(0.00, origin, MotionMode.SMOOTH),
                        new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                        new Contact(0.32, sillPoint, MotionMode.WALK),         // feet on cab floor sill Y=0.59
                        new Contact(0.46, besideSill, MotionMode.WALK),        // step straight onto ground beside car!
                        new Contact(0.60, doorEdge, MotionMode.WALK),          // step to open door tip while turning toward door
                        new Contact(0.86, doorClosed, MotionMode.SMOOTH),      // hand physically pushes door shut!
                        new Contact(1.00, landing, MotionMode.SMOOTH)));
            }

            // Truck: open door -> cab floor head#142 -> EXACT metal footstep head#24 (1.24, 0.72, 4.16) ->
            // ground beside step -> step to open door edge (2.22, 0, 3.20) & TURN TO FACE DOOR -> push door shut!
            Vec3 step1 = portal.foothold() != null
                    ? new Vec3(portal.foothold().x, Math.max(landing.y, portal.foothold().y), portal.foothold().z)
                    : new Vec3(sSign * 1.24, Math.max(landing.y, 0.72), 4.16);
            Vec3 stepBase = new Vec3(step1.x + Math.copySign(0.42, step1.x), landing.y, step1.z);
            Vec3 doorEdge = portal.exteriorStep() != null
                    ? new Vec3(portal.exteriorStep().x, landing.y, portal.exteriorStep().z)
                    : new Vec3(sSign * 2.24, landing.y, 3.18);
            Vec3 doorClosed = new Vec3(sSign * 1.76, landing.y, 3.44);
            return new MountMotion(List.of(
                    new Contact(0.00, origin, MotionMode.SMOOTH),
                    new Contact(releaseFrac, origin, MotionMode.SMOOTH),
                    new Contact(0.29, sillPoint, MotionMode.WALK),             // real cab floor Y=1.32
                    new Contact(0.42, step1, MotionMode.WALK),                 // boots squarely ON metal step head#24!
                    new Contact(0.53, stepBase, MotionMode.WALK),              // step smoothly onto ground (ZERO jump!)
                    new Contact(0.65, doorEdge, MotionMode.WALK),              // step beside open door & face it
                    new Contact(0.88, doorClosed, MotionMode.SMOOTH),          // hand physically pushes door shut!
                    new Contact(1.00, landing, MotionMode.SMOOTH)));
        }

        // 6. Default (step / chair / deck / simple ladder): rescale reversed plan() cleanly into [releaseFrac..1.0]
        MountMotion forward = plan(profile, door, hull, landing, seat, portal);
        if (forward == null) return null;
        return forward.reversedInto(releaseFrac, origin);
    }

    private MountMotion reversedInto(double releaseFrac, Vec3 origin) {
        var rev = new java.util.ArrayList<Contact>(contacts.size() + 1);
        rev.add(new Contact(0.0, origin, MotionMode.SMOOTH));
        for (int i = contacts.size() - 1; i >= 0; i--) {
            Contact c = contacts.get(i);
            MotionMode mode = switch (c.mode) {
                case CLIMB_UP -> MotionMode.WALK;
                case DROP_IN -> MotionMode.CLIMB_UP;
                default -> c.mode;
            };
            double mapped = releaseFrac + (1.0 - c.time) * (1.0 - releaseFrac);
            rev.add(new Contact(mapped, c.position, mode));
        }
        return new MountMotion(rev);
    }

    Vec3 sample(double time) {
        double clamped = Mth.clamp(time, 0, 1);
        for (int i = 1; i < contacts.size(); i++) {
            Contact end = contacts.get(i);
            if (clamped > end.time) continue;
            Contact start = contacts.get(i - 1);
            double span = end.time - start.time;
            if (span <= 1e-6) return end.position;
            double u = Mth.clamp((clamped - start.time) / span, 0, 1);
            return interpolate(start.position, end.position, u, end.mode);
        }
        return contacts.get(contacts.size() - 1).position;
    }

    private static Vec3 interpolate(Vec3 a, Vec3 b, double u, MotionMode mode) {
        return switch (mode) {
            case WALK -> {
                double s = u + 0.06 * Math.sin(u * Math.PI * 2.0) / (Math.PI * 2.0);
                yield a.lerp(b, Mth.clamp(s, 0, 1));
            }
            case CLIMB_UP -> {
                // Hugging surface climb: rises steadily while pulling smoothly inward along the real edge,
                // never levitating straight up in empty air far outside the hull!
                if (b.y > a.y + 0.35) {
                    double uy = smooth(Math.min(1.0, u / 0.78));
                    double uxz = smooth(Math.max(0.0, (u - 0.16) / 0.84));
                    yield new Vec3(Mth.lerp(uxz, a.x, b.x), Mth.lerp(uy, a.y, b.y), Mth.lerp(uxz, a.z, b.z));
                }
                yield a.lerp(b, smooth(u));
            }
            case DROP_IN -> {
                // Center over hatch/seat horizontally first, then drop straight inside!
                double uxz = smooth(Math.min(1.0, u / 0.55));
                double uy = u * u * (2.0 - u) * 0.35 + u * u * 0.65;
                yield new Vec3(Mth.lerp(uxz, a.x, b.x), Mth.lerp(uy, a.y, b.y), Mth.lerp(uxz, a.z, b.z));
            }
            case JUMP_DOWN -> {
                // Crisp gravity step-off for genuine elevated drops (dy > 0.80) with ZERO upward hopLift!
                double dy = a.y - b.y;
                if (dy > 0.80) {
                    double y = Mth.lerp(u * u, a.y, b.y);
                    double uxz = smooth(Math.min(1.0, u / 0.82));
                    yield new Vec3(Mth.lerp(uxz, a.x, b.x), y, Mth.lerp(uxz, a.z, b.z));
                }
                yield a.lerp(b, smooth(u));
            }
            case SMOOTH -> a.lerp(b, smooth(u));
        };
    }

    private static double smooth(double u) {
        double t = Mth.clamp(u, 0, 1);
        return t * t * (3.0 - 2.0 * t);
    }

    private static Vec3 sill(AABB hull, Vec3 from, Vec3 seat) {
        Vec3 floorSeat = new Vec3(seat.x, from.y, seat.z);
        AABB floor = new AABB(hull.minX, from.y - 1, hull.minZ, hull.maxX, from.y + 1, hull.maxZ);
        var crossing = floor.clip(from, floorSeat);
        if (crossing.isEmpty()) return from.lerp(floorSeat, 0.62);
        Vec3 edge = crossing.get();
        Vec3 inward = floorSeat.subtract(edge);
        return edge.add(inward.lengthSqr() > 0.02 ? inward.normalize().scale(0.14) : inward);
    }

    private record Contact(double time, Vec3 position, MotionMode mode) {}
}
