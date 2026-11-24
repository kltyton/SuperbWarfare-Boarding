package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.interactiveanimation.api.Bindings;
import com.kltyton.interactiveanimation.api.InteractiveScenes;
import com.kltyton.interactiveanimation.api.PlaybackOptions;
import com.kltyton.interactiveanimation.api.SceneHandle;
import com.kltyton.interactiveanimation.api.SceneId;
import com.kltyton.interactiveanimation.api.SceneSubject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Runs the normal voluntary exit handler once, at the start of the step out. */
public final class DismountScenes {
    public static final String RIDER = "superbwarfare_boarding_exit_rider";
    private static final String FILE = "superbwarfare_boarding:animations/dismount.animation.json";
    private static final Map<MinecraftServer, Map<UUID, Run>> ACTIVE = new HashMap<>();
    private static final ThreadLocal<Run> NATIVE_EXIT = new ThreadLocal<>();

    private DismountScenes() {}

    static void register() {
        InteractiveScenes.register(DismountScenes.class, FILE);
    }

    public static boolean begin(ServerPlayer player, Runnable nativeExit) {
        var pending = NATIVE_EXIT.get();
        if (pending != null && pending.route.player == player) return false;
        var server = player.serverLevel().getServer();
        if (!server.isSameThread() || !player.isAlive()) return false;
        var active = ACTIVE.get(server);
        if (active != null) {
            Run existing = active.get(player.getUUID());
            if (existing != null && BoardingScenes.playing(existing.scene)) {
                if (player.level().getGameTime() - existing.startedTick >= 3) {
                    active.remove(player.getUUID());
                    existing.skip();
                }
                return true;
            }
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return false;
        var route = DismountRoute.find(player, vehicle);
        if (route == null) return false;
        var run = new Run(route, nativeExit);
        var subject = new SceneSubject(player.level(), route::frame, run::valid,
                vehicle, 0, false, () -> run.started = true, run::stop);
        var options = PlaybackOptions.DEFAULT.camera(PlaybackOptions.CameraMode.VANILLA).markers(run::marker);
        var binding = Bindings.named(Map.of(RIDER, player));
        run.scene = InteractiveScenes.play(subject, new SceneId(FILE, route.clip()), binding, options);
        if (!run.started) return false;
        if (BoardingScenes.playing(run.scene)) {
            ACTIVE.computeIfAbsent(server, ignored -> new HashMap<>()).put(player.getUUID(), run);
        }
        return true;
    }

    public static Vec3 departureFrom(VehicleEntity vehicle, LivingEntity passenger) {
        var run = NATIVE_EXIT.get();
        return run != null && run.onSeat && run.route.vehicle == vehicle && run.route.player == passenger
                ? run.route.departurePosition() : null;
    }

    static boolean ownsVehicleContact(VehicleEntity vehicle, ServerPlayer rider) {
        var active = ACTIVE.get(rider.serverLevel().getServer());
        var run = active == null ? null : active.get(rider.getUUID());
        return run != null && run.route.vehicle == vehicle && BoardingScenes.playing(run.scene);
    }

    static void tick(MinecraftServer server) {
        var active = ACTIVE.get(server);
        if (active == null) return;
        active.values().removeIf(run -> !BoardingScenes.playing(run.scene));
        if (active.isEmpty()) ACTIVE.remove(server);
    }

    static void shutdown(MinecraftServer server) {
        var active = ACTIVE.remove(server);
        if (active != null) active.values().forEach(run -> run.scene.cancel());
    }

    private static final class Run {
        final DismountRoute route;
        final Runnable nativeExit;
        final long startedTick;
        SceneHandle scene;
        boolean started;
        boolean leftSeat;
        boolean onSeat;
        boolean skipped;

        Run(DismountRoute route, Runnable nativeExit) {
            this.route = route;
            this.nativeExit = nativeExit;
            this.startedTick = route.player.level().getGameTime();
        }

        void skip() {
            if (skipped) return;
            skipped = true;
            if (scene != null) scene.cancel();
            var player = route.player;
            if (!leftSeat && player.isAlive() && route.seated()) {
                depart(false);
            }
            if (player.isAlive() && !player.isPassenger()) {
                Vec3 dest = route.landingPosition();
                player.setDeltaMovement(Vec3.ZERO);
                player.connection.teleport(dest.x, dest.y, dest.z, route.landingYaw(), player.getXRot());
            }
        }

        boolean valid() {
            var player = route.player;
            return player.isAlive() && player.level() == route.vehicle.level() && route.available()
                    && (leftSeat ? !player.isPassenger()
                            && EntryPath.clear(route.vehicle, player, player.position(), route.frame().position())
                            : route.seated());
        }

        void marker(PlaybackOptions.Marker marker) {
            if (marker.instruction().equals("leave_seat")) {
                depart(true);
            } else {
                BoardingSounds.play(marker.instruction(), route.player, route.profile, route.door,
                        VehicleMotion.accessJoints(route.vehicle));
            }
        }

        void stop() {
            if (started && !leftSeat && route.player.isAlive() && route.seated()) depart(false);
        }

        void depart(boolean animate) {
            if (leftSeat || !route.seated()) return;
            onSeat = animate;
            NATIVE_EXIT.set(this);
            try {
                nativeExit.run();
                leftSeat = !route.seated();
            } finally {
                NATIVE_EXIT.remove();
                onSeat = false;
            }
        }
    }
}
