package com.kltyton.superbwarfareboarding.client.compat;

import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendyCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Evaluates the same Bedrock bend channels on playerAnimator's existing BendyLib cuboids. */
public final class BoardingLegacyBends {
    private BoardingLegacyBends() {}

    public static void apply(ClientActor actor, PlayerModel<?> player) {
        bend(actor, "left_arm", player.leftArm, "left_sleeve", player.leftSleeve);
        bend(actor, "right_arm", player.rightArm, "right_sleeve", player.rightSleeve);
        bend(actor, "left_leg", player.leftLeg, "left_pants", player.leftPants);
        bend(actor, "right_leg", player.rightLeg, "right_pants", player.rightPants);
        bend(actor, "body", player.body, "jacket", player.jacket);
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
