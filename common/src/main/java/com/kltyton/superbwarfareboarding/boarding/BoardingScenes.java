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
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class BoardingScenes {
    private static final String ANIMATION_FILE = "superbwarfare_boarding:animations/boarding.animation.json";
    private static final String APPROACH_FILE = "superbwarfare_boarding:animations/boarding_approach.animation.json";
    private static final String DOOR_FILE = "superbwarfare_boarding:animations/boarding_cab_door.animation.json";
    public static final String RIDER = "superbwarfare_boarding_rider";
    private static final Map<MinecraftServer, Map<UUID, Run>> ACTIVE = new HashMap<>();
    private static BiPredicate<VehicleEntity, Entity> clientContact = (vehicle, rider) -> false;

    private BoardingScenes() {}

    public static void registerClientContact(BiPredicate<VehicleEntity, Entity> contact) {
        clientContact = contact;
    }

    /** The scene already sweeps the rider's box against blocks and sets its position. */
    public static boolean ownsVehicleContact(VehicleEntity vehicle, Entity entity) {
        if (vehicle.level().isClientSide) return clientContact.test(vehicle, entity);
        if (!(entity instanceof ServerPlayer rider)) return false;
        var scenes = ACTIVE.get(rider.serverLevel().getServer());
        Run run = scenes == null ? null : scenes.get(vehicle.getUUID());
        return (run != null && run.rider == rider && playing(run.scene))
                || DismountScenes.ownsVehicleContact(vehicle, rider);
    }

    public static void initialize() {
        InteractiveScenes.register(BoardingScenes.class, ANIMATION_FILE);
        InteractiveScenes.register(BoardingScenes.class, APPROACH_FILE);
        InteractiveScenes.register(BoardingScenes.class, DOOR_FILE);
        DismountScenes.register();
    }

    /** Called from Superb Warfare's accepted mount branch, after its native interaction checks. */
    public static boolean board(ServerPlayer rider, VehicleEntity vehicle, BooleanSupplier nativeMount) {
        var server = rider.serverLevel().getServer();
        var active = ACTIVE.computeIfAbsent(server, ignored -> new HashMap<>());
        Run existing = active.get(vehicle.getUUID());
        if (existing != null) {
            if (existing.rider == rider && playing(existing.scene)) {
                if (vehicle.level().getGameTime() - existing.startedTick >= 3) {
                    active.remove(vehicle.getUUID());
                    return existing.skip();
                }
            }
            return true;
        }
        if (active.values().stream().anyMatch(run -> run.rider == rider)) return false;
        int seat = vehicle.getOrderedPassengers().indexOf(null);
        if (rider.isPassenger() || seat < 0 || seat >= vehicle.computed().seats().size() || vehicle.getLocked()) {
            return nativeMount.getAsBoolean();
        }

        var route = new SeatRoute(vehicle, rider, seat);
        if (!route.hasRoute()) return nativeMount.getAsBoolean();
        var run = new Run(rider, vehicle, route, nativeMount);
        if (!playing(run.scene)) return run.mount();
        active.put(vehicle.getUUID(), run);
        return true;
    }

    /** The reserved rider crosses the chair's touch area before the native mount can own that contact. */
    public static boolean ownsChairContact(VehicleEntity vehicle, ServerPlayer rider) {
        if (DismountScenes.ownsVehicleContact(vehicle, rider)) return true;
        var scenes = ACTIVE.get(rider.serverLevel().getServer());
        Run run = scenes == null ? null : scenes.get(vehicle.getUUID());
        return run != null && run.rider == rider && run.route.profile() == BoardingProfile.CHAIR && run.route.available();
    }

    public static void tick(MinecraftServer server) {
        DismountScenes.tick(server);
        var scenes = ACTIVE.get(server);
        if (scenes == null) return;
        scenes.values().removeIf(run -> !playing(run.scene));
        if (scenes.isEmpty()) ACTIVE.remove(server);
    }

    public static void shutdown(MinecraftServer server) {
        DismountScenes.shutdown(server);
        var scenes = ACTIVE.remove(server);
        if (scenes != null) scenes.values().forEach(run -> run.scene.cancel());
    }

    static boolean playing(SceneHandle scene) {
        return scene != null && (scene.state() == SceneHandle.State.APPROACHING || scene.state() == SceneHandle.State.PLAYING);
    }

    private static final class Run {
        final ServerPlayer rider;
        final VehicleEntity vehicle;
        final SeatRoute route;
        final BooleanSupplier nativeMount;
        final Bindings binding;
        final long startedTick;
        SceneHandle scene;
        boolean attemptedMount;
        boolean mounted;

        Run(ServerPlayer rider, VehicleEntity vehicle, SeatRoute route, BooleanSupplier nativeMount) {
            this.rider = rider;
            this.vehicle = vehicle;
            this.route = route;
            this.nativeMount = nativeMount;
            this.startedTick = vehicle.level().getGameTime();
            binding = Bindings.named(Map.of(RIDER, rider));
            if (route.strides() == 0) {
                startMount();
            } else {
                var subject = new SceneSubject(vehicle.level(), route::walkFrame, route::walkUnblocked,
                        vehicle, 0, false, () -> {}, () -> {});
                var options = PlaybackOptions.DEFAULT.camera(PlaybackOptions.CameraMode.VANILLA).markers(marker -> {
                    if (marker.instruction().equals("enter")) {
                        marker.playback().cancel();
                        startMount();
                    } else {
                        BoardingSounds.play(marker.instruction(), rider, route);
                    }
                });
                scene = InteractiveScenes.play(subject, new SceneId(APPROACH_FILE, route.approachAnimation()),
                        binding, options);
            }
        }

        void startMount() {
            route.beginMount();
            if (!route.mountUnblocked()) {
                mount();
                return;
            }
            var subject = new SceneSubject(vehicle.level(), route::mountFrame, route::mountUnblocked,
                    vehicle, 0, false, () -> {}, () -> {});
            var options = PlaybackOptions.DEFAULT.camera(PlaybackOptions.CameraMode.VANILLA).markers(marker -> {
                if (marker.instruction().equals("mount")) {
                    mount();
                } else {
                    BoardingSounds.play(marker.instruction(), rider, route);
                }
            });
            var door = route.door();
            if (door == null) {
                scene = InteractiveScenes.play(subject, new SceneId(ANIMATION_FILE, route.profile().animation()), binding, options);
            } else {
                scene = InteractiveScenes.play(subject, new SceneId(DOOR_FILE, door.animation(vehicle)), binding, options);
            }
        }

        boolean skip() {
            if (scene != null) scene.cancel();
            return mount();
        }

        private boolean mount() {
            if (!attemptedMount && route.available()) {
                attemptedMount = true;
                mounted = nativeMount.getAsBoolean();
            }
            return mounted;
        }
    }
}
