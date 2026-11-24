package com.kltyton.superbwarfareboarding.boarding;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Plays spatial cues as the server advances through the authored boarding markers. */
final class BoardingSounds {
    private BoardingSounds() {}

    static void play(String instruction, ServerPlayer rider, SeatRoute route) {
        play(instruction, rider, route.profile(), route.door(), route.accessJoints());
    }

    static void play(String instruction, ServerPlayer rider, BoardingProfile profile,
                     CabDoor door, java.util.List<VehicleMotion.Joint> joints) {
        SoundEvent event;
        float volume;
        boolean atFeet = instruction.equals("footstep");
        if (atFeet) {
            BlockPos below = BlockPos.containing(rider.getX(), rider.getY() - 0.12, rider.getZ());
            event = rider.serverLevel().getBlockState(below).getSoundType().getStepSound();
            volume = 0.38F;
        } else {
            switch (instruction) {
                case "door_open", "door_close" -> {
                    if (door == null) return;
                    event = instruction.equals("door_open") ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE;
                    volume = 0.47F;
                }
                case "access_open", "access_close" -> {
                    if (joints.isEmpty()) return;
                    boolean opening = instruction.equals("access_open");
                    if (profile == BoardingProfile.BRADLEY_DOOR) {
                        event = opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE;
                    } else {
                        event = opening ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE;
                    }
                    volume = 0.39F;
                }
                case "board_step" -> {
                    event = switch (profile) {
                        case LADDER, HATCH -> SoundEvents.LADDER_STEP;
                        case DECK -> SoundEvents.WOOD_STEP;
                        default -> SoundEvents.METAL_STEP;
                    };
                    volume = 0.36F;
                }
                case "settle" -> {
                    event = SoundEvents.WOOL_STEP;
                    volume = 0.31F;
                }
                default -> {
                    return;
                }
            }
        }
        // The source is at the rider's point of contact, including entrances far from the vehicle origin.
        var point = atFeet ? rider.position() : rider.position().add(0, 0.7, 0);
        float pitch = 0.96F + rider.getRandom().nextFloat() * 0.08F;
        rider.serverLevel().playSound(null, point.x, point.y, point.z, event, SoundSource.PLAYERS, volume, pitch);
    }
}
