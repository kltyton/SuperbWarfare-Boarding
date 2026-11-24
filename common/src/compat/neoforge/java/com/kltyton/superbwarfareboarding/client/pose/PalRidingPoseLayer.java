package com.kltyton.superbwarfareboarding.client.pose;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.interactiveanimation.client.playback.ClientScenes;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * PAL animation layer (priority 850, below AnimCue's 900 cutscene layer) that replaces vanilla
 * straight-legged sitting with vehicle-specific bent-knee driving/piloting/gunner postures.
 */
final class PalRidingPoseLayer implements IAnimation {
    private final Entity entity;
    private final boolean extendedBend;
    private float partialTick;
    private VehicleRidingPose.Snapshot current;

    PalRidingPoseLayer(Entity entity, boolean extendedBend) {
        this.entity = entity;
        this.extendedBend = extendedBend;
    }

    @Override
    public void setupAnim(AnimationData data) {
        partialTick = data.getPartialTick();
        current = resolve();
    }

    @Override
    public boolean isActive() {
        current = resolve();
        return current != null;
    }

    private VehicleRidingPose.Snapshot resolve() {
        if (!(entity instanceof Player rider)) return null;
        if (!(rider.getVehicle() instanceof VehicleEntity vehicle)) return null;
        // Let AnimCue cutscenes (boarding / dismount) take full control while playing
        if (ClientScenes.actor(entity, partialTick) != null) return null;
        return VehicleRidingPose.sample(rider, vehicle, partialTick);
    }

    @Override
    public PlayerAnimBone get3DTransform(PlayerAnimBone bone) {
        VehicleRidingPose.Snapshot snap = current != null ? current : resolve();
        if (snap == null) return bone;

        String name = bone.getName();
        if (name.equals("head")) {
            // Keep player's natural look rotation; only weld neck socket position to reclined/leaned torso!
            bone.updatePosition(snap.headPosX(), snap.headPosY(), snap.headPosZ());
            return bone;
        }
        if (name.equals("body")) {
            // Keep avatar root neutral (seat offset is owned by Superb Warfare's passenger positioning)
            bone.setToInitialPose();
            clearBend(bone);
            return bone;
        }

        VehicleRidingPose.BonePose pose = switch (name) {
            case "torso" -> snap.torso();
            case "left_arm" -> snap.leftArm();
            case "right_arm" -> snap.rightArm();
            case "left_leg" -> snap.leftLeg();
            case "right_leg" -> snap.rightLeg();
            default -> null;
        };
        if (pose == null) return bone;

        bone.setToInitialPose();
        clearBend(bone);
        bone.updatePosition(pose.posX(), pose.posY(), pose.posZ());
        bone.updateRotation(rad(pose.rotX()), rad(pose.rotY()), rad(pose.rotZ()));
        float bx = rad(pose.bendX());
        float by = rad(pose.bendY());
        float bz = rad(pose.bendZ());
        bone.bend = bx;
        if (extendedBend && bone instanceof PalMoreBendHolder holder) {
            holder.palMore$setBend(bx, by, bz);
            holder.palMore$setBendVectorOverride(true);
        }
        return bone;
    }

    private void clearBend(PlayerAnimBone bone) {
        bone.bend = 0f;
        if (extendedBend && bone instanceof PalMoreBendHolder holder) {
            holder.palMore$setBend(0f, 0f, 0f);
            holder.palMore$setBendVectorOverride(false);
        }
    }

    private static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }
}
