package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.interactiveanimation.api.SceneSubject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/**
 * The safe exterior landing and unified-timeline dismount motion from an occupied native seat.
 * Keeps position, body yaw (turning to face doors/hatches/canopies when closing them), and
 * Bedrock animation keyframes locked to the exact same [0..1] clock.
 */
final class DismountRoute {
    private static final double MAX_STEP_DOWN = 11.00;
    private static final double STOP_SPEED = 0.045;
    private static final double[] LANDING_OFFSETS = {0, 0.30, 0.60, 0.95, 1.30};
    private static final List<Vec3> EXTERIOR_SIDES = List.of(new Vec3(-1, 0, 0), new Vec3(1, 0, 0),
            new Vec3(0, 0, -1), new Vec3(0, 0, 1));
    final ServerPlayer player;
    final VehicleEntity vehicle;
    final BoardingProfile profile;
    final CabDoor door;
    private final int seat;
    private final Vec3 standingSeat;
    private final Vec3 landing;
    private final Vec3 carrierOrigin;
    private final Vec3 portalAimLocal;
    private final MountMotion motion;
    private final long startTick;
    private final float startYaw;
    private final float exitYaw;
    private final int duration;
    private final int release;
    private final boolean interiorStart;
    private final boolean ladderDescent;
    private final boolean mi28SideDoor;

    private DismountRoute(ServerPlayer player, VehicleEntity vehicle, int seat, BoardingProfile profile,
                          CabDoor door, Vec3 standingSeat, Vec3 landing, MountMotion motion,
                          Vec3 portalLip, Vec3 portalAim, boolean interiorStart, boolean ladderDescent) {
        this.player = player;
        this.vehicle = vehicle;
        this.seat = seat;
        this.profile = profile;
        this.door = door;
        this.standingSeat = standingSeat;
        this.landing = landing;
        this.carrierOrigin = vehicle.position();
        this.portalAimLocal = portalAim != null ? portalAim : portalLip;
        this.motion = motion;
        this.startTick = vehicle.level().getGameTime();
        this.startYaw = player.getYRot();
        this.interiorStart = interiorStart;
        this.ladderDescent = ladderDescent;
        this.mi28SideDoor = profile == BoardingProfile.MI28_DOOR;
        Vec3 outward = (profile == BoardingProfile.BRADLEY_DOOR || profile == BoardingProfile.RAMP)
                ? Vec3.directionFromRotation(0, vehicle.getYRot() + 180)
                : landing.subtract(EntryPath.toWorld(vehicle, portalLip));
        this.exitYaw = outward.horizontalDistanceSqr() < 0.0001
                ? vehicle.getYRot() + 180
                : (float) Math.toDegrees(Mth.atan2(-outward.x, outward.z));
        duration = door == null ? profile.ticks() : door.durationTicks(vehicle);
        release = releaseTick(profile, duration);
    }

    static DismountRoute find(ServerPlayer player, VehicleEntity vehicle) {
        int seat = vehicle.getSeatIndex(player);
        if (seat < 0 || seat >= vehicle.computed().seats().size() || !stationary(vehicle)) return null;
        Vec3 seated = player.position();
        // Only lift out of ground blocks if actually sunken; never pop +0.56 blocks upward through low cab roofs!
        Vec3 standingSeat = EntryPath.standingPoint(player, seated, seated.y + 0.08);
        Vec3 localSeat = EntryPath.toLocal(vehicle, standingSeat);
        Vec3 nativeExit = EntryPath.toLocal(vehicle, vehicle.getDismountLocationForIndex(player, seat));
        var portal = EntryPortal.resolve(vehicle, seat, localSeat, nativeExit);
        Vec3 mouth = portal == null ? localSeat : portal.lip();
        Vec3 hint = portal != null && portal.approachHint() != null ? portal.approachHint() : nativeExit;
        var hull = EntryPath.footprint(vehicle);
        double clearance = player.getBbWidth() * 0.5 + 0.32;
        var bounds = hull.inflate(clearance, 0, clearance);
        Vec3 edge = portal == null ? EntryPath.entrance(bounds, mouth, hint, nativeExit)
                : portal.entrance(bounds, hint, nativeExit);
        var door = CabDoor.forSeat(vehicle, seat);
        var edges = new ArrayList<ExitApproach>();
        edges.add(new ExitApproach(edge, portal));
        var opposite = portal == null || door != null ? null : portal.oppositeSide(vehicle);
        if (opposite != null) {
            edges.add(new ExitApproach(opposite.entrance(bounds, opposite.approachHint(), nativeExit), opposite));
        }
        if (portal == null && door == null) {
            for (Vec3 side : EXTERIOR_SIDES) {
                var alternative = new ExitApproach(EntryPath.entrance(bounds, mouth, mouth.add(side), nativeExit), null);
                if (!edges.contains(alternative)) edges.add(alternative);
            }
        }
        double height = Math.max(standingSeat.y + 0.65, EntryPath.toWorld(vehicle, mouth).y + 0.20);
        double search = MAX_STEP_DOWN + height - standingSeat.y;
        int supported = 0;
        int planned = 0;
        Vec3 lastLanding = null;
        for (ExitApproach candidate : edges) {
            Vec3 entrance = candidate.edge;
            Vec3 opening = candidate.portal == null ? mouth : candidate.portal.lip();
            Vec3 normal = new Vec3(entrance.x - opening.x, 0, entrance.z - opening.z);
            normal = normal.horizontalDistanceSqr() > 0.0001 ? normal.normalize() : new Vec3(1, 0, 0);
            for (double offset : LANDING_OFFSETS) {
                Vec3 world = EntryPath.toWorld(vehicle, entrance.add(normal.scale(offset)));
                Vec3 probe = new Vec3(world.x, height, world.z);
                var box = player.getDimensions(Pose.STANDING).makeBoundingBox(probe);
                if (vehicle.level().getBlockCollisions(player, box).iterator().hasNext()) continue;
                Vec3 down = Entity.collideBoundingBox(player, new Vec3(0, -search, 0), box, vehicle.level(), List.of());
                if (down.y <= -search + 0.001) continue;
                Vec3 landing = probe.add(down);
                if (vehicle.level().getBlockCollisions(player, box.move(down)).iterator().hasNext()) continue;
                supported++;
                lastLanding = EntryPath.toLocal(vehicle, landing);
                var profile = door == null ? BoardingProfile.of(vehicle, seat, standingSeat.y - landing.y) : BoardingProfile.CAB;
                double releaseFrac = releaseFraction(profile);
                var motion = MountMotion.planDismount(vehicle, profile, door, hull, localSeat, lastLanding, candidate.portal, releaseFrac);
                if (motion == null) continue;
                planned++;
                Vec3 departure = EntryPath.toWorld(vehicle, motion.sample(0));
                if (!clearRoute(player, vehicle, motion, departure)) continue;
                boolean usesInterior = candidate.portal != null && candidate.portal.interior() != null;
                boolean isLadder = (door != null && door.isFh77(vehicle) && opening.y - lastLanding.y > 1.25)
                        || profile == BoardingProfile.LADDER || profile == BoardingProfile.GHAST_CLIMB;
                Vec3 aim = candidate.portal != null ? candidate.portal.aim() : opening;
                return new DismountRoute(player, vehicle, seat, profile, door, departure, landing,
                        motion, opening, aim, usesInterior, isLadder);
            }
        }
        LogUtils.getLogger().debug("No dismount path for {} seat {} local {} ({} landings, {} motions, last landing {})",
                vehicle.getType(), seat, localSeat, supported, planned, lastLanding);
        return null;
    }

    private record ExitApproach(Vec3 edge, EntryPortal portal) {}

    static boolean stationary(VehicleEntity vehicle) {
        Vec3 displacement = vehicle.position().subtract(vehicle.xo, vehicle.yo, vehicle.zo);
        return vehicle.isAlive() && !vehicle.isWreck()
                && displacement.lengthSqr() < STOP_SPEED * STOP_SPEED
                && vehicle.getDeltaMovement().horizontalDistanceSqr() < STOP_SPEED * STOP_SPEED;
    }

    private static boolean clearRoute(ServerPlayer rider, VehicleEntity vehicle, MountMotion motion, Vec3 start) {
        Vec3 previous = start;
        for (int step = 1; step <= 20; step++) {
            Vec3 next = EntryPath.toWorld(vehicle, motion.sample(step / 20.0));
            if (!EntryPath.clear(vehicle, rider, previous, next)) return false;
            previous = next;
        }
        return true;
    }

    private static double releaseFraction(BoardingProfile profile) {
        return switch (profile) {
            case CAB, HATCH, BRADLEY_DOOR, T90_HATCH, RAMP, COCKPIT, MI28_DOOR, JU87_COCKPIT -> 0.16;
            default -> 0.10;
        };
    }

    private static int releaseTick(BoardingProfile profile, int duration) {
        return Math.round((float) (duration * releaseFraction(profile)));
    }

    boolean seated() {
        return player.getVehicle() == vehicle && vehicle.getSeatIndex(player) == seat;
    }

    boolean available() {
        return stationary(vehicle) && vehicle.position().distanceToSqr(carrierOrigin) < 0.35 * 0.35;
    }

    Vec3 departurePosition() {
        return standingSeat;
    }

    Vec3 landingPosition() {
        return landing;
    }

    float landingYaw() {
        return exitYaw;
    }

    String clip() {
        if (door != null) return door.dismountAnimation(vehicle);
        return "animation.dismount." + profile.name().toLowerCase(java.util.Locale.ROOT);
    }

    SceneSubject.Frame frame() {
        long ticks = vehicle.level().getGameTime() - startTick;
        double t = Mth.clamp((double) ticks / duration, 0.0, 1.0);
        if (ticks < release) {
            float yaw = interiorStart && (profile == BoardingProfile.BRADLEY_DOOR || profile == BoardingProfile.RAMP)
                    ? exitYaw : startYaw;
            return new SceneSubject.Frame(standingSeat, yaw);
        }
        Vec3 point = t >= 1.0 ? landing : EntryPath.toWorld(vehicle, motion.sample(t));

        // 1. Ladder & Happiest Ghast descents
        if (ladderDescent) {
            if (profile == BoardingProfile.GHAST_CLIMB) {
                // Walk forward to rear-side edge (0.10..0.30), turn to face wall to climb down (0.30..0.90), turn outward on landing.
                float toEdge = Mth.rotLerp((float) smooth((t - 0.10) / 0.14), startYaw, exitYaw);
                float wallFacing = Mth.rotLerp((float) smooth((t - 0.28) / 0.14), toEdge, exitYaw + 180);
                float yaw = Mth.rotLerp((float) smooth((t - 0.90) / 0.10), wallFacing, exitYaw);
                return new SceneSubject.Frame(point, yaw);
            }
            // FH-77BW & tall steel ladders: face the door/ladder while closing cab door & climbing down, then turn outward at bottom!
            float wallFacing = Mth.rotLerp((float) smooth((t - 0.22) / 0.18), startYaw, exitYaw + 180);
            float yaw = Mth.rotLerp((float) smooth((t - 0.88) / 0.12), wallFacing, exitYaw);
            return new SceneSubject.Frame(point, yaw);
        }

        // 2. Pickup & Truck hinged cab doors:
        // Step out facing outward (0.16..0.48), then TURN TO FACE THE OPEN DOOR (0.48..0.64),
        // physically push the door shut while facing it (0.64..0.88), then relax back outward (0.88..1.00)!
        if (door != null) {
            float towardDoor = headingTo(point, EntryPath.toWorld(vehicle, portalAimLocal), exitYaw + 180);
            float stepOutYaw = Mth.rotLerp((float) smooth((t - 0.16) / 0.24), startYaw, exitYaw);
            float faceDoorYaw = Mth.rotLerp((float) smooth((t - 0.48) / 0.14), stepOutYaw, towardDoor);
            float finishYaw = Mth.rotLerp((float) smooth((t - 0.88) / 0.12), faceDoorYaw, exitYaw);
            return new SceneSubject.Frame(point, finishYaw);
        }

        // 3. Bradley & LAV-25/LAV-AD rear doors:
        // Step cleanly out facing rearward (0.16..0.58), then TURN 180° TO FACE THE OPEN REAR DOOR(S) (0.58..0.70),
        // physically push door(s) shut while facing forward toward vehicle (0.70..0.90), then turn casually outward!
        if (profile == BoardingProfile.BRADLEY_DOOR || profile == BoardingProfile.RAMP) {
            float faceRearDoor = vehicle.getYRot();
            float turnToDoor = Mth.rotLerp((float) smooth((t - 0.56) / 0.14), exitYaw, faceRearDoor);
            float finishYaw = Mth.rotLerp((float) smooth((t - 0.90) / 0.10), turnToDoor, exitYaw);
            return new SceneSubject.Frame(point, finishYaw);
        }

        // 4. Mi-28 side cockpit door:
        // Slide out to sponson -> FACE SIDE DOOR (0.36..0.60) to physically push it shut -> step down onto wheel & ground!
        if (mi28SideDoor) {
            float towardDoor = headingTo(point, EntryPath.toWorld(vehicle, portalAimLocal), exitYaw + 180);
            float faceDoor = Mth.rotLerp((float) smooth((t - 0.30) / 0.12), startYaw, towardDoor);
            float stepDown = Mth.rotLerp((float) smooth((t - 0.60) / 0.16), faceDoor, exitYaw);
            return new SceneSubject.Frame(point, stepDown);
        }

        // 5. Tank hatches & Fighter cockpits:
        // Climb out -> TURN TO FACE HATCH/CANOPY (0.30..0.54) to physically close lid ->
        // walk along deck/wing facing travel direction (0.54..0.74) -> face outward for crisp drop & cushion!
        if (profile == BoardingProfile.T90_HATCH || profile == BoardingProfile.HATCH
                || profile == BoardingProfile.COCKPIT || profile == BoardingProfile.JU87_COCKPIT) {
            float towardHatch = headingTo(point, EntryPath.toWorld(vehicle, portalAimLocal), startYaw);
            Vec3 prev = EntryPath.toWorld(vehicle, motion.sample(Math.max(0.0, t - 0.03)));
            Vec3 next = EntryPath.toWorld(vehicle, motion.sample(Math.min(1.0, t + 0.03)));
            float travel = next.subtract(prev).horizontalDistanceSqr() > 0.002
                    ? (float) Math.toDegrees(Mth.atan2(-(next.x - prev.x), next.z - prev.z))
                    : exitYaw;
            float closeYaw = Mth.rotLerp((float) smooth((t - 0.28) / 0.12), startYaw, towardHatch);
            float walkYaw = Mth.rotLerp((float) smooth((t - 0.52) / 0.12), closeYaw, travel);
            float dropYaw = Mth.rotLerp((float) smooth((t - 0.74) / 0.12), walkYaw, exitYaw);
            return new SceneSubject.Frame(point, dropYaw);
        }

        // 6. Simple step / chair / deck dismounts
        double turn = Mth.clamp((t - 0.10) / 0.35, 0.0, 1.0);
        float yaw = Mth.rotLerp((float) smooth(turn), startYaw, exitYaw);
        return new SceneSubject.Frame(point, yaw);
    }

    private static float headingTo(Vec3 from, Vec3 to, float fallback) {
        Vec3 d = to.subtract(from);
        if (d.horizontalDistanceSqr() < 0.0004) return fallback;
        return (float) Math.toDegrees(Mth.atan2(-d.x, d.z));
    }

    private static double smooth(double v) {
        double t = Mth.clamp(v, 0, 1);
        return t * t * (3.0 - 2.0 * t);
    }
}
