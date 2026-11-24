package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.interactiveanimation.api.SceneSubject;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SeatRoute {
    /** Distance above which the rider transitions from natural walk to a brisk athletic sprint. */
    private static final double RUN_THRESHOLD = 3.20;
    private static final int WALK_STRIDE_TICKS = 12;
    private static final int RUN_STRIDE_TICKS = 10;
    private static final double WALK_STRIDE_LENGTH = 1.80;
    private static final double RUN_STRIDE_LENGTH = 2.65;
    private static final int MAX_WALK_STRIDES = 4;
    private static final int MAX_RUN_STRIDES = 12;
    private static final double TURN_DISTANCE = 0.34;
    private static final int FACE_PATH_TICKS = 3;
    private static final int FACE_ENTRANCE_TICKS = 3;

    private final VehicleEntity vehicle;
    private final Player player;
    private final int seatIndex;
    private final Vec3 entrance;
    private final AABB hull;
    private final List<Vec3> path;
    private final double[] distances;
    private final double distance;
    private final boolean running;
    private final int strides;
    private final int strideTicks;
    private final BoardingProfile profile;
    private final CabDoor door;
    private final float walkStartYaw;
    private long startedAt;
    private float yaw;
    private MountMotion mountMotion;

    SeatRoute(VehicleEntity vehicle, Player player, int seatIndex) {
        this.vehicle = vehicle;
        this.player = player;
        this.seatIndex = seatIndex;
        startedAt = vehicle.level().getGameTime();
        yaw = player.getYRot();
        walkStartYaw = yaw;
        SeatInfo seat = seat();
        Vec3 position = seatPosition(seat);
        door = CabDoor.forSeat(vehicle, seatIndex);
        profile = door == null ? BoardingProfile.of(vehicle, seatIndex, position.y - player.getY()) : BoardingProfile.CAB;
        Vec3 start = EntryPath.toLocal(vehicle, player.position());
        Vec3 endpoint = EntryPath.toLocal(vehicle, position);
        var dismount = seat.getDismountInfo();
        Vec3 dismountHint = dismount == null || dismount.getPosition() == null ? null
                : EntryPath.toLocal(vehicle, transform(dismount.getTransform(), dismount.getPosition()));
        double clearance = player.getBbWidth() / 2.0 + (profile == BoardingProfile.CHAIR ? 0.16 : 0.26);
        hull = EntryPath.footprint(vehicle);
        var bounds = hull.inflate(clearance, 0, clearance);
        var portal = EntryPortal.resolve(vehicle, seatIndex, endpoint, start);
        Vec3 target = portal == null ? endpoint : portal.lip();
        Vec3 hint = portal != null && portal.approachHint() != null ? portal.approachHint() : dismountHint;
        Vec3 edge = portal == null ? EntryPath.entrance(bounds, target, hint, start)
                : portal.entrance(bounds, hint, start);
        entrance = supportAtEntrance(edge, target);
        path = EntryPath.find(vehicle, player, bounds, start, entrance);
        distances = new double[path.size()];
        for (int i = 1; i < path.size(); i++) {
            distances[i] = distances[i - 1] + path.get(i - 1).distanceTo(path.get(i));
        }
        distance = path.isEmpty() ? 0 : distances[distances.length - 1];
        double skipThreshold = door == null ? 0.22 : 0.12;
        if (distance < skipThreshold) {
            running = false;
            strides = 0;
            strideTicks = WALK_STRIDE_TICKS;
        } else if (distance > RUN_THRESHOLD) {
            running = true;
            strides = Mth.clamp((int) Math.ceil(distance / RUN_STRIDE_LENGTH), 2, MAX_RUN_STRIDES);
            strideTicks = RUN_STRIDE_TICKS;
        } else {
            running = false;
            strides = Mth.clamp((int) Math.ceil(distance / WALK_STRIDE_LENGTH), 1, MAX_WALK_STRIDES);
            strideTicks = WALK_STRIDE_TICKS;
        }
        mountMotion = planMount(strides == 0 ? start : entrance);
    }

    BoardingProfile profile() {
        return profile;
    }

    CabDoor door() {
        return door;
    }

    List<VehicleMotion.Joint> accessJoints() {
        return VehicleMotion.accessJoints(vehicle);
    }

    int strides() {
        return strides;
    }

    boolean running() {
        return running;
    }

    String approachAnimation() {
        return (running ? "animation.boarding.run_" : "animation.boarding.walk_") + strides;
    }

    boolean hasRoute() {
        return !path.isEmpty() && distance <= MAX_RUN_STRIDES * RUN_STRIDE_LENGTH && mountMotion != null;
    }

    boolean available() {
        var passengers = vehicle.getOrderedPassengers();
        return player.isAlive() && player.level() == vehicle.level() && !player.isPassenger()
                && vehicle.isAlive() && !vehicle.isWreck() && !vehicle.getLocked()
                && seatIndex < passengers.size() && seatIndex < vehicle.computed().seats().size()
                && passengers.indexOf(null) == seatIndex;
    }

    boolean walkUnblocked() {
        if (!available()) return false;
        Vec3 next = walkPosition();
        return next != null && EntryPath.clearWalk(vehicle, player, player.position(), next);
    }

    SceneSubject.Frame walkFrame() {
        double progress = walkDistance();
        Vec3 position = walkPosition();
        Vec3 behind = pointAt(progress - TURN_DISTANCE);
        Vec3 ahead = pointAt(progress + TURN_DISTANCE);
        float direction = heading(behind, ahead, yaw);
        double elapsed = vehicle.level().getGameTime() - startedAt;
        double totalTicks = strides * strideTicks;
        float settle = (float) smooth((elapsed - (totalTicks - FACE_ENTRANCE_TICKS)) / FACE_ENTRANCE_TICKS);
        float turn = (float) smooth(elapsed / FACE_PATH_TICKS);
        float facing = Mth.rotLerp(settle, Mth.rotLerp(turn, walkStartYaw, direction), entranceYaw());
        return oriented(position == null ? player.position() : position, facing);
    }

    void beginMount() {
        startedAt = vehicle.level().getGameTime();
        mountMotion = planMount(EntryPath.toLocal(vehicle, player.position()));
    }

    SceneSubject.Frame mountFrame() {
        double time = mountProgress();
        Vec3 pos = mountPosition(time);
        float startYaw = strides == 0 ? walkStartYaw : entranceYaw();
        float entryFacing = Mth.rotLerp((float) smooth(time / 0.16), startYaw, entranceYaw());
        float seatYaw = vehicle.getYRot() + seat().getOrientation();
        if (door != null) {
            if (door.isFh77(vehicle)) {
                float climbFacing = Mth.rotLerp((float) smooth(time / 0.14), startYaw, entranceYaw());
                float intoCab = Mth.rotLerp((float) smooth((time - 0.68) / 0.18), climbFacing, seatYaw);
                return oriented(pos, intoCab);
            }
            float pull = Mth.rotLerp((float) smooth((time - 0.18) / 0.24), entranceYaw(),
                    Mth.rotLerp(0.30f, entranceYaw(), seatYaw));
            float towardDoor = Mth.rotLerp((float) smooth((time - 0.44) / 0.14), pull, entranceYaw());
            float entry = Mth.rotLerp((float) smooth((time - 0.68) / 0.18), towardDoor, seatYaw);
            float facing = Mth.rotLerp((float) smooth(time / 0.12), startYaw, entry);
            return oriented(pos, facing);
        }
        if (profile == BoardingProfile.BRADLEY_DOOR || profile == BoardingProfile.RAMP) {
            float forward = Mth.rotLerp((float) smooth((time - 0.12) / 0.16), entryFacing, vehicle.getYRot());
            return oriented(pos, forward);
        }
        if (profile == BoardingProfile.JU87_COCKPIT) {
            float tangent = travelYaw(time, entryFacing);
            float walkBlend = (float) (smooth((time - 0.28) / 0.10) * (1.0 - smooth((time - 0.48) / 0.10)));
            float midYaw = Mth.rotLerp(walkBlend, entryFacing, tangent);
            float finalYaw = Mth.rotLerp((float) smooth((time - 0.76) / 0.16), midYaw, seatYaw);
            return oriented(pos, finalYaw);
        }
        if (profile == BoardingProfile.T90_HATCH || profile == BoardingProfile.HATCH || profile == BoardingProfile.COCKPIT) {
            // Face the hull during initial climb (0..0.25), turn naturally along the deck/wing walk path (0.26..0.50),
            // face the hatch/cockpit to open it (0.50..0.72), then turn smoothly into seat heading (0.72..0.88).
            float tangent = travelYaw(time, entryFacing);
            float deckWalk = (float) (smooth((time - 0.25) / 0.08) * (1.0 - smooth((time - 0.48) / 0.10)));
            float hatchFacing = Mth.rotLerp(deckWalk, entryFacing, tangent);
            float finalYaw = Mth.rotLerp((float) smooth((time - 0.72) / 0.18), hatchFacing, seatYaw);
            return oriented(pos, finalYaw);
        }
        if (profile == BoardingProfile.GHAST_CLIMB) {
            // Face the ghast wall while climbing up tentacles/side (0..0.74), then face forward into saddle (0.74..0.94).
            float saddleFacing = Mth.rotLerp((float) smooth((time - 0.74) / 0.18), entryFacing, seatYaw);
            return oriented(pos, saddleFacing);
        }
        double turnIntoSeat = profile == BoardingProfile.LADDER
                ? smooth((time - 0.66) / 0.22)
                : smooth((time - 0.44) / 0.36);
        return oriented(pos, Mth.rotLerp((float) turnIntoSeat, entryFacing, seatYaw));
    }

    private float travelYaw(double time, float fallback) {
        Vec3 a = mountPosition(Math.max(0.0, time - 0.03));
        Vec3 b = mountPosition(Math.min(1.0, time + 0.03));
        return a.subtract(b).horizontalDistanceSqr() > 0.002 ? heading(a, b, fallback) : fallback;
    }

    boolean mountUnblocked() {
        if (mountMotion == null || !available()) return false;
        return EntryPath.clear(vehicle, player, player.position(), mountPosition(mountProgress()));
    }

    private double mountProgress() {
        int duration = door == null ? profile.ticks() : door.durationTicks(vehicle);
        return Mth.clamp((double) (vehicle.level().getGameTime() - startedAt) / duration, 0, 1);
    }

    private Vec3 mountPosition(double time) {
        return EntryPath.toWorld(vehicle, mountMotion.sample(time));
    }

    private MountMotion planMount(Vec3 start) {
        double approachHeight = EntryPath.toWorld(vehicle, start).y;
        Vec3 end = EntryPath.standingPoint(player, seatPosition(seat()), approachHeight);
        Vec3 localSeat = EntryPath.toLocal(vehicle, end);
        var portal = EntryPortal.resolve(vehicle, seatIndex, localSeat, start);
        return MountMotion.plan(profile, door, hull, start, localSeat, portal);
    }

    private Vec3 supportAtEntrance(Vec3 point, Vec3 target) {
        Vec3 outward = new Vec3(point.x - target.x, 0, point.z - target.z);
        if (outward.horizontalDistanceSqr() < 0.0001) return point;
        outward = outward.normalize();
        for (double offset = 0; offset <= 0.8; offset += 0.2) {
            Vec3 candidate = point.add(outward.scale(offset));
            if (groundAt(EntryPath.toWorld(vehicle, candidate)) != null) return candidate;
        }
        return point;
    }

    private Vec3 walkPosition() {
        return groundAt(pointAt(walkDistance()));
    }

    private Vec3 groundAt(Vec3 next) {
        Vec3 probe = new Vec3(next.x, player.getY() + 0.10, next.z);
        var box = player.getBoundingBox().move(probe.subtract(player.position()));
        Vec3 support = Entity.collideBoundingBox(player, new Vec3(0, -0.48, 0), box, player.level(), List.of());
        return support.y > -0.479 ? probe.add(support) : null;
    }

    private double walkDistance() {
        double total = strides * strideTicks;
        double duration = Math.max(4.0, total - FACE_PATH_TICKS - FACE_ENTRANCE_TICKS);
        double t = Mth.clamp((vehicle.level().getGameTime() - startedAt - FACE_PATH_TICKS) / duration, 0, 1);
        double accelRamp = running ? Math.min(0.16, 3.0 / duration) : Math.min(0.18, 2.0 / duration);
        double brakeRamp = running ? Math.min(0.24, 5.0 / duration) : Math.min(0.20, 2.5 / duration);
        double area = 1.0 - 0.5 * (accelRamp + brakeRamp);
        double s;
        if (t < accelRamp) {
            s = 0.5 * t * t / accelRamp;
        } else if (t > 1.0 - brakeRamp) {
            double rem = 1.0 - t;
            s = area - 0.5 * rem * rem / brakeRamp;
        } else {
            s = t - 0.5 * accelRamp;
        }
        return distance * Mth.clamp(s / area, 0, 1);
    }

    private Vec3 pointAt(double length) {
        if (length <= 0) return EntryPath.toWorld(vehicle, path.get(0));
        for (int i = 1; i < distances.length; i++) {
            if (length <= distances[i] && distances[i] > distances[i - 1]) {
                double t = (length - distances[i - 1]) / (distances[i] - distances[i - 1]);
                return EntryPath.toWorld(vehicle, path.get(i - 1).lerp(path.get(i), t));
            }
        }
        return EntryPath.toWorld(vehicle, entrance);
    }

    private float entranceYaw() {
        SeatInfo seat = seat();
        float seatYaw = vehicle.getYRot() + seat.getOrientation();
        if (profile == BoardingProfile.CHAIR) return seatYaw;
        Vec3 target = seatPosition(seat);
        var portal = EntryPortal.resolve(vehicle, seatIndex, EntryPath.toLocal(vehicle, target), entrance);
        if (portal != null) {
            target = EntryPath.toWorld(vehicle, portal.aim());
        }
        return heading(EntryPath.toWorld(vehicle, entrance), target, seatYaw);
    }

    private SceneSubject.Frame oriented(Vec3 position, float facing) {
        yaw += Mth.wrapDegrees(facing - yaw);
        return new SceneSubject.Frame(position, yaw);
    }

    private SeatInfo seat() {
        return vehicle.computed().seats().get(seatIndex);
    }

    private Vec3 seatPosition(SeatInfo seat) {
        return transform(seat.transform, seat.getPosition()).add(0, VehicleConfig.getPassengerYOffset(player.getType()), 0);
    }

    private Vec3 transform(String transform, Vec3 local) {
        var point = vehicle.transformPosition(vehicle.getTransformFromString(transform), local.x, local.y, local.z);
        return new Vec3(point.x, point.y, point.z);
    }

    private static float heading(Vec3 start, Vec3 end, float fallback) {
        Vec3 delta = end.subtract(start);
        if (delta.horizontalDistanceSqr() < 0.000001) return fallback;
        return (float) Math.toDegrees(Mth.atan2(-delta.x, delta.z));
    }

    private static double smooth(double value) {
        double t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
