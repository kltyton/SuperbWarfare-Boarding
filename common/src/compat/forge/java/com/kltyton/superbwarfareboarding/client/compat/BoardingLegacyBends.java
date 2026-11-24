package com.kltyton.superbwarfareboarding.client.compat;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendyCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import com.kltyton.superbwarfareboarding.client.pose.VehicleRidingPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Evaluates Bedrock bend channels and vehicle riding poses on Forge 1.20.1 PlayerModel + BendyLib. */
public final class BoardingLegacyBends {
    private BoardingLegacyBends() {}

    public static void apply(ClientActor actor, PlayerModel<?> player) {
        bend(actor, "left_arm", player.leftArm, "left_sleeve", player.leftSleeve);
        bend(actor, "right_arm", player.rightArm, "right_sleeve", player.rightSleeve);
        bend(actor, "left_leg", player.leftLeg, "left_pants", player.leftPants);
        bend(actor, "right_leg", player.rightLeg, "right_pants", player.rightPants);
        bend(actor, "body", player.body, "jacket", player.jacket);
    }

    public static void applyRidingPose(VehicleRidingPose.Snapshot snap, PlayerModel<?> player, boolean withBends) {
        player.head.x += snap.headPosX();
        player.head.y -= snap.headPosY();
        player.head.z += snap.headPosZ();
        player.hat.copyFrom(player.head);

        applyBone(snap.torso(), player.body, player.jacket, 0f, 0f, 0f, withBends);
        applyBone(snap.leftArm(), player.leftArm, player.leftSleeve, 5f, 2f, 0f, withBends);
        applyBone(snap.rightArm(), player.rightArm, player.rightSleeve, -5f, 2f, 0f, withBends);
        applyBone(snap.leftLeg(), player.leftLeg, player.leftPants, 1.9f, 12f, 0f, withBends);
        applyBone(snap.rightLeg(), player.rightLeg, player.rightPants, -1.9f, 12f, 0f, withBends);
    }

    public static void clearRidingBends(PlayerModel<?> player) {
        PalMoreBendyCuboids.clearPartState(player.body);
        PalMoreBendyCuboids.clearPartState(player.jacket);
        PalMoreBendyCuboids.clearPartState(player.leftArm);
        PalMoreBendyCuboids.clearPartState(player.leftSleeve);
        PalMoreBendyCuboids.clearPartState(player.rightArm);
        PalMoreBendyCuboids.clearPartState(player.rightSleeve);
        PalMoreBendyCuboids.clearPartState(player.leftLeg);
        PalMoreBendyCuboids.clearPartState(player.leftPants);
        PalMoreBendyCuboids.clearPartState(player.rightLeg);
        PalMoreBendyCuboids.clearPartState(player.rightPants);
    }

    private static void applyBone(VehicleRidingPose.BonePose pose, ModelPart base, ModelPart outer,
                                  float initX, float initY, float initZ, boolean withBends) {
        base.x = initX + pose.posX();
        base.y = initY - pose.posY();
        base.z = initZ + pose.posZ();
        base.xRot = (float) Math.toRadians(pose.rotX());
        base.yRot = (float) Math.toRadians(pose.rotY());
        base.zRot = (float) Math.toRadians(pose.rotZ());
        outer.copyFrom(base);
        if (withBends && (Math.abs(pose.bendX()) > 0.01f || Math.abs(pose.bendY()) > 0.01f || Math.abs(pose.bendZ()) > 0.01f)) {
            var state = new PalMoreBendState(
                    (float) Math.toRadians(pose.bendX()),
                    (float) Math.toRadians(pose.bendY()),
                    (float) Math.toRadians(pose.bendZ()),
                    true, 0, 0, 0, 1, 1, 1, false);
            PalMoreBendyCuboids.applyVectorBend(base, state);
            PalMoreBendyCuboids.applyVectorBend(outer, state);
        }
    }

    private static void bend(ClientActor actor, String bone, ModelPart base, String layerBone, ModelPart outer) {
        var inherited = actor.bones().get(bone);
        var explicit = actor.bones().get(layerBone);
        applyTo(actor, base, inherited);
        applyTo(actor, outer, explicit != null && explicit.hasBend() ? explicit : inherited);
    }

    private static void applyTo(ClientActor actor, ModelPart part, com.kltyton.interactiveanimation.scene.asset.BoneTrack track) {
        if (track == null || !track.hasBend()) return;
        var angle = track.bend().sample(actor.seconds()).scale(Math.PI / 180);
        PalMoreBendyCuboids.applyVectorBend(part, new PalMoreBendState((float) angle.x, (float) angle.y,
                (float) angle.z, true, 0, 0, 0, 1, 1, 1, false));
        LegacyActorRenderer.restoreAfterDraw(() -> PalMoreBendyCuboids.clearPartState(part));
    }
}
