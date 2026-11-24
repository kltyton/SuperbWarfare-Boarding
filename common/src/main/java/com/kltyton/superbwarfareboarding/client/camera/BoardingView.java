package com.kltyton.superbwarfareboarding.client.camera;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.tools.OBB;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.kltyton.superbwarfareboarding.boarding.DismountScenes;
import com.kltyton.superbwarfareboarding.boarding.EntryPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Follows the rider from the outside of the vehicle and returns to the original native view. */
public final class BoardingView {
    private static final int PULL_BACK_TICKS = 15;
    private static final int RETURN_TICKS = 15;
    private static final double SELF_CLEARANCE = 1.05;
    private static final Map<UUID, Integer> ACTIVE_SEATS = new HashMap<>();
    private static Runnable dismountSkip = () -> {};
    private static Session session;
    private static LocalPlayer observedPlayer;
    private static CameraType observedPerspective;
    private static Vec3 observedPosition;
    private static float observedYaw;
    private static float observedPitch;

    private BoardingView() {}

    public static void registerDismountSkip(Runnable sender) {
        dismountSkip = sender == null ? () -> {} : sender;
    }

    public static boolean ownsVehicleContact(VehicleEntity vehicle, Entity rider) {
        return ClientScenes.subjectFor(rider, BoardingScenes.RIDER) == vehicle
                || ClientScenes.subjectFor(rider, DismountScenes.RIDER) == vehicle;
    }

    /** Tracks the exact seat index currently boarding or dismounting this vehicle on the client. */
    public static int activeSeat(VehicleEntity vehicle) {
        for (var player : vehicle.level().players()) {
            if (!ownsVehicleContact(vehicle, player)) continue;
            if (player.getVehicle() == vehicle) {
                int seated = vehicle.getSeatIndex(player);
                if (seated >= 0) {
                    ACTIVE_SEATS.put(vehicle.getUUID(), seated);
                    return seated;
                }
            }
            Integer cached = ACTIVE_SEATS.get(vehicle.getUUID());
            if (cached != null) return cached;
            int free = vehicle.getOrderedPassengers().indexOf(null);
            if (free >= 0) {
                ACTIVE_SEATS.put(vehicle.getUUID(), free);
                return free;
            }
            return -1;
        }
        ACTIVE_SEATS.remove(vehicle.getUUID());
        return -1;
    }

    public static void clearActiveSeat(VehicleEntity vehicle) {
        ACTIVE_SEATS.remove(vehicle.getUUID());
    }

    public static boolean isAnimating(VehicleEntity vehicle) {
        return session != null && session.vehicle == vehicle;
    }

    public static void tick(Minecraft game) {
        var player = game.player;
        if (player == null || game.level == null || game.getCameraEntity() != player || !player.isAlive()) {
            restore(game);
            observedPlayer = null;
            observedPosition = null;
            return;
        }
        if (session != null && session.player != player) restore(game);
        long now = game.level.getGameTime();
        Entity subject = ClientScenes.subjectFor(player, BoardingScenes.RIDER);
        boolean exiting = false;
        if (subject == null) {
            subject = ClientScenes.subjectFor(player, DismountScenes.RIDER);
            exiting = subject != null;
        }
        if (subject instanceof VehicleEntity vehicle) {
            if (session == null || session.vehicle != vehicle || session.exiting != exiting) {
                restore(game);
                CameraType perspective = game.options.getCameraType();
                Pose origin = observedPlayer == player && observedPerspective == perspective
                        ? new Pose(observedPosition, observedYaw, observedPitch) : null;
                session = new Session(player, vehicle, perspective, now, origin, exiting);
            }
            session.lastActive = now;
            session.nearbyHulls = collectNearbyHulls(player, vehicle);
            session.selfHulls = collectSelfHulls(vehicle);
            pollSkipInput(game, player, session, now);
        } else if (session != null && session.returning < 0) {
            // Once mounted (or finished exiting), begin the smooth return immediately so focus never snaps to a distant seat.
            boolean handoffComplete = (!session.exiting && player.getVehicle() == session.vehicle)
                    || (session.exiting && player.getVehicle() == null && now - session.lastActive >= 1)
                    || now - session.lastActive >= 2;
            if (handoffComplete) {
                if (session.skipSent || now - session.start < 22) {
                    restore(game);
                    return;
                }
                session.returning = now;
            }
        }
        if (session != null && session.returning >= 0 && now > session.returning + RETURN_TICKS) restore(game);
    }

    private static void pollSkipInput(Minecraft game, LocalPlayer player, Session active, long now) {
        if (game.screen != null || active.skipSent) return;
        if (!active.exiting) {
            boolean useClicked = game.options.keyUse.consumeClick();
            boolean useDown = game.options.keyUse.isDown();
            if (!useDown && !useClicked) {
                active.sawKeyRelease = true;
            } else if ((active.sawKeyRelease || useClicked) && now - active.start >= 3 && game.gameMode != null) {
                active.skipSent = true;
                game.gameMode.interact(player, active.vehicle, InteractionHand.MAIN_HAND);
            }
        } else {
            boolean exitClicked = ModKeyMappings.DISMOUNT.consumeClick() || game.options.keyShift.consumeClick();
            boolean exitDown = ModKeyMappings.DISMOUNT.isDown() || game.options.keyShift.isDown();
            if (!exitDown && !exitClicked) {
                active.sawKeyRelease = true;
            } else if ((active.sawKeyRelease || exitClicked) && now - active.start >= 3) {
                active.skipSent = true;
                dismountSkip.run();
            }
        }
    }

    private static AABB[] collectNearbyHulls(LocalPlayer player, VehicleEntity vehicle) {
        return player.level().getEntitiesOfClass(VehicleEntity.class,
                player.getBoundingBox().inflate(7), other -> other != vehicle && other.isAlive()).stream()
                .map(other -> other.getCollisionOBB() == null ? other.getBoundingBox()
                        : OBB.getWorldAABB(other.getCollisionOBB())).toArray(AABB[]::new);
    }

    private static AABB[] collectSelfHulls(VehicleEntity vehicle) {
        var obbs = vehicle.getOBBs();
        if (obbs != null && !obbs.isEmpty()) {
            var boxes = new ArrayList<AABB>(obbs.size());
            for (OBB part : obbs) boxes.add(OBB.getWorldAABB(part));
            return boxes.toArray(AABB[]::new);
        }
        return new AABB[]{vehicle.getBoundingBox()};
    }

    public static CameraType originalPerspective(Entity cameraEntity) {
        return session != null && session.player == cameraEntity ? session.previous : null;
    }

    public static void observe(Minecraft game, Entity cameraEntity, Camera camera) {
        if (cameraEntity != game.player || game.player == null) return;
        observedPlayer = game.player;
        observedPerspective = game.options.getCameraType();
        observedPosition = camera.getPosition();
        observedYaw = camera.getYRot();
        observedPitch = camera.getXRot();
    }

    public static void apply(Minecraft game, Camera camera, float partialTick) {
        var shot = session;
        if (shot == null) return;
        var port = (BoardingCamera) camera;
        double now = shot.player.level().getGameTime() - 1 + partialTick;
        var nativePose = new Pose(camera.getPosition(), camera.getYRot(), camera.getXRot());
        boolean nativeDetached = camera.isDetached();
        Vec3 carrierPosition = shot.vehicle.getPosition(partialTick);
        Pose next;
        if (shot.returning >= 0) {
            if (shot.exitOffset == null) {
                shot.exitOffset = shot.last == null ? Vec3.ZERO : shot.last.position.subtract(carrierPosition);
            }
            float weight = smooth((now - shot.returning) / RETURN_TICKS);
            if (shot.last == null || weight >= 1) {
                restore(game);
                return;
            }
            var exit = new Pose(carrierPosition.add(shot.exitOffset), shot.exitYaw, shot.exitPitch);
            next = shot.player.getVehicle() == shot.vehicle && shot.previous.isFirstPerson()
                    ? exit.blendPastCarrier(nativePose, weight, shot.vehicle, shot.selfHulls) : exit.blend(nativePose, weight);
        } else {
            var actor = ClientScenes.actor(shot.player, partialTick);
            Vec3 feet;
            if (actor != null) {
                feet = actor.pose().position();
                shot.lastFeetOffset = feet.subtract(carrierPosition);
            } else if (shot.lastFeetOffset != null) {
                // Hold the last actor contact across a 1-tick walk->mount packet boundary.
                feet = carrierPosition.add(shot.lastFeetOffset);
            } else {
                feet = shot.player.getPosition(partialTick);
            }
            Vec3 focus = feet.add(0, 0.97, 0);
            double elapsed = Double.isNaN(shot.sampledAt) ? 1 : Mth.clamp(now - shot.sampledAt, 0, 1);
            Vec3 normal = EntryPath.outward(shot.vehicle, feet, shot.bounds);
            if (Double.isNaN(shot.sampledAt)) {
                int seat = shot.exiting ? shot.vehicle.getSeatIndex(shot.player) : -1;
                if (seat >= 0) {
                    Vec3 nativeExit = shot.vehicle.getDismountLocationForIndex(shot.player, seat);
                    Vec3 exitSide = EntryPath.outward(shot.vehicle, nativeExit, shot.bounds);
                    normal = exitSide.lengthSqr() > 0.015 ? exitSide : nativeExit.subtract(feet);
                }
                Vec3 direction = normal.lengthSqr() > 0.015 ? normal : feet.subtract(carrierPosition);
                float inward = direction.horizontalDistanceSqr() > 0.015
                        ? yaw(-direction.x, -direction.z) : nativePose.yaw;
                shot.quarterTurn = Mth.wrapDegrees(inward - shot.vehicle.getYRot()) < 0 ? -19 : 19;
                shot.sideYaw = inward + shot.quarterTurn;
                shot.followDistance = Mth.clamp(3.15F + 0.18F * shot.vehicle.getBbWidth(), 3.45F, 4.65F);
            } else if (normal.lengthSqr() > 0.02
                    && (shot.exiting || ClientScenes.actor(shot.vehicle, partialTick) == null)) {
                float nextSide = yaw(-normal.x, -normal.z) + shot.quarterTurn;
                shot.sideYaw = Mth.approachDegrees(shot.sideYaw, nextSide, (float) (3.5 * elapsed));
            }
            float angle = shot.sideYaw;
            Vec3 cameraPoint = port.superbwarfareBoarding$orbit(focus, angle, 16, shot.followDistance);
            float clearPitch = clearHullPitch(focus, cameraPoint, 16, shot.nearbyHulls, 0.40);
            // Keep the orbit camera outside the target vehicle's own wings, hull walls, and turret.
            clearPitch = clearHullPitch(focus, cameraPoint, clearPitch, shot.selfHulls, 0.22);
            shot.orbitPitch = Mth.approach(shot.orbitPitch, clearPitch, (float) (2.2 * elapsed));
            shot.sampledAt = now;
            if (shot.orbitPitch > 16.01F) {
                cameraPoint = port.superbwarfareBoarding$orbit(focus, angle, shot.orbitPitch, shot.followDistance);
            }
            float weight = smooth((now - shot.start) / PULL_BACK_TICKS);
            Pose departure = shot.origin == null ? nativePose : shot.origin;
            Pose follow = new Pose(cameraPoint, angle, shot.orbitPitch);
            next = shot.exiting && shot.previous.isFirstPerson()
                    ? departure.blendPastCarrier(follow, weight, shot.vehicle, shot.selfHulls) : departure.blend(follow, weight);
            shot.exitYaw = next.yaw;
            shot.exitPitch = next.pitch;
        }
        shot.last = next;
        boolean hasClearance = next.position.distanceToSqr(nativePose.position) >= SELF_CLEARANCE * SELF_CLEARANCE
                && next.position.distanceToSqr(shot.player.getEyePosition(partialTick)) >= SELF_CLEARANCE * SELF_CLEARANCE;
        boolean showRider = nativeDetached || hasClearance;
        game.options.setCameraType(showRider ? CameraType.THIRD_PERSON_BACK : shot.previous);
        port.superbwarfareBoarding$apply(next.position, next.yaw, next.pitch, showRider);
    }

    private static float clearHullPitch(Vec3 focus, Vec3 cameraPoint, float currentPitch, AABB[] hulls, double pad) {
        float clearPitch = currentPitch;
        Vec3 rayStart = focus.lerp(cameraPoint, 0.25);
        for (AABB hull : hulls) {
            AABB inflated = hull.inflate(pad);
            if (!inflated.contains(cameraPoint) && inflated.clip(rayStart, cameraPoint).isEmpty()) continue;
            var hit = inflated.clip(rayStart, cameraPoint).orElse(cameraPoint);
            double span = Math.max(0.85, hit.subtract(focus).horizontalDistance());
            float slope = (float) Math.toDegrees(Math.atan2(hull.maxY + 0.50 - focus.y, span));
            clearPitch = Math.max(clearPitch, Math.min(34, slope));
        }
        return clearPitch;
    }

    private static float yaw(double x, double z) {
        return (float) Math.toDegrees(Mth.atan2(-x, z));
    }

    private static float smooth(double fraction) {
        float t = (float) Mth.clamp(fraction, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static void restore(Minecraft game) {
        if (session == null) return;
        ACTIVE_SEATS.remove(session.vehicle.getUUID());
        game.options.setCameraType(session.previous);
        session = null;
    }

    private record Pose(Vec3 position, float yaw, float pitch) {
        Pose blend(Pose to, float weight) {
            return new Pose(position.lerp(to.position, weight), Mth.rotLerp(weight, yaw, to.yaw),
                    Mth.lerp(weight, pitch, to.pitch));
        }

        Pose blendPastCarrier(Pose to, float weight, VehicleEntity carrier, AABB[] hulls) {
            Pose straight = blend(to, weight);
            Vec3 middleStart = position.lerp(to.position, 0.14);
            Vec3 middleEnd = position.lerp(to.position, 0.86);
            double ceiling = Double.NEGATIVE_INFINITY;
            for (AABB box : hulls) {
                AABB shell = box.inflate(0.16);
                if (shell.contains(straight.position) || shell.contains(middleStart)
                        || shell.clip(middleStart, middleEnd).isPresent()) {
                    ceiling = Math.max(ceiling, shell.maxY);
                }
            }
            if (!Double.isFinite(ceiling)) return straight;
            Vec3 midpoint = position.lerp(to.position, 0.5);
            double rise = Mth.clamp(ceiling + 0.30 - midpoint.y, 0, 1.8);
            Vec3 raised = straight.position.add(0, rise * 4 * weight * (1 - weight), 0);
            var safe = carrier.level().clip(new ClipContext(straight.position, raised,
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, carrier)).getLocation();
            return new Pose(safe, straight.yaw, straight.pitch);
        }
    }

    private static final class Session {
        final LocalPlayer player;
        final VehicleEntity vehicle;
        final CameraType previous;
        final long start;
        final Pose origin;
        final AABB bounds;
        final boolean exiting;
        long lastActive;
        long returning = -1;
        double sampledAt = Double.NaN;
        float sideYaw;
        float quarterTurn;
        float orbitPitch = 16;
        float followDistance;
        AABB[] nearbyHulls = new AABB[0];
        AABB[] selfHulls = new AABB[0];
        Vec3 lastFeetOffset;
        Pose last;
        Vec3 exitOffset;
        float exitYaw;
        float exitPitch;
        boolean sawKeyRelease;
        boolean skipSent;

        Session(LocalPlayer player, VehicleEntity vehicle, CameraType previous, long tick, Pose origin, boolean exiting) {
            this.player = player;
            this.vehicle = vehicle;
            this.previous = previous;
            this.origin = origin;
            this.exiting = exiting;
            bounds = EntryPath.footprint(vehicle);
            start = lastActive = tick;
        }
    }
}
