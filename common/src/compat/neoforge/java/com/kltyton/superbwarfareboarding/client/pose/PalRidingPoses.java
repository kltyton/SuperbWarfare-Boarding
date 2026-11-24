package com.kltyton.superbwarfareboarding.client.pose;

import com.zigythebird.playeranim.api.PlayerAnimationAccess;

/** Registers the vehicle seated riding-pose PAL layer on NeoForge 1.21.1. */
public final class PalRidingPoses {
    private static boolean registered;

    private PalRidingPoses() {}

    public static void register(boolean moreRotation) {
        if (registered) return;
        registered = true;
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((actor, stack) ->
                stack.addAnimLayer(850, new PalRidingPoseLayer(actor, moreRotation)));
    }
}
