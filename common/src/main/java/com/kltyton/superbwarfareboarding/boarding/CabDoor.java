package com.kltyton.superbwarfareboarding.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;

/** Authored channels for existing hinged entrances in the stock vehicle models. */
public enum CabDoor {
    LEFT("superbwarfare_boarding:left_door", "animation.boarding.cab_left", "leftdoor", "door_left"),
    RIGHT("superbwarfare_boarding:right_door", "animation.boarding.cab_right", "rightdoor", "door_right");

    static final int DURATION_TICKS = 46;
    static final int FH77_DURATION_TICKS = 60;
    private static final Set<String> HINGES = Set.of(LEFT.pickupBone, LEFT.truckBone, RIGHT.pickupBone, RIGHT.truckBone);
    private final String track;
    private final String animation;
    private final String pickupBone;
    private final String truckBone;

    CabDoor(String track, String animation, String pickupBone, String truckBone) {
        this.track = track;
        this.animation = animation;
        this.pickupBone = pickupBone;
        this.truckBone = truckBone;
    }

    static CabDoor forSeat(VehicleEntity vehicle, int seat) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        if (key.toString().equals("superbwarfare:fh_77bw")) {
            return seat < 0 || seat == 1 ? null : seat == 4 ? RIGHT : LEFT;
        }
        if (seat < 0 || seat > 1 || !vehicle.isEnclosed(seat)) return null;
        var side = seat == 0 ? LEFT : RIGHT;
        return side.modelBone(vehicle) == null ? null : side;
    }

    public static CabDoor forTrack(String track) {
        if (LEFT.track.equals(track)) return LEFT;
        if (RIGHT.track.equals(track)) return RIGHT;
        return null;
    }

    public static Set<String> hinges() {
        return HINGES;
    }

    public String modelBone(VehicleEntity vehicle) {
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        if (!type.getNamespace().equals("superbwarfare")) return null;
        return switch (type.getPath()) {
            case "sodayo_pick_up", "sodayo_pick_up_hmg", "sodayo_pick_up_rocket", "sodayo_pick_up_tow" ->
                    pickupBone;
            case "truck" -> truckBone;
            case "fh_77bw" -> this == LEFT ? "door" : "door2";
            default -> null;
        };
    }

    boolean isPickup(VehicleEntity vehicle) {
        return pickupBone.equals(modelBone(vehicle));
    }

    boolean isFh77(VehicleEntity vehicle) {
        return "fh_77bw".equals(VehicleRigCatalog.id(vehicle));
    }

    int durationTicks(VehicleEntity vehicle) {
        return isFh77(vehicle) ? FH77_DURATION_TICKS : DURATION_TICKS;
    }

    String animation(VehicleEntity vehicle) {
        if (isFh77(vehicle)) return this == LEFT ? "animation.boarding.fh77_left" : "animation.boarding.fh77_right";
        if (!isPickup(vehicle)) return animation;
        return this == LEFT ? "animation.boarding.pickup_left" : "animation.boarding.pickup_right";
    }

    String dismountAnimation(VehicleEntity vehicle) {
        if (isFh77(vehicle)) return this == LEFT ? "animation.dismount.fh77_left" : "animation.dismount.fh77_right";
        if (isPickup(vehicle)) return this == LEFT ? "animation.dismount.pickup_left" : "animation.dismount.pickup_right";
        return this == LEFT ? "animation.dismount.cab_left" : "animation.dismount.cab_right";
    }
}
