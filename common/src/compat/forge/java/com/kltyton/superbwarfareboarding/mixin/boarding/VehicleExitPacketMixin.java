package com.kltyton.superbwarfareboarding.mixin.boarding;

import com.atsuishio.superbwarfare.network.message.send.PlayerStopRidingMessage;
import com.kltyton.superbwarfareboarding.boarding.DismountScenes;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animates the voluntary exit packet, leaving the native ejection handler immediate. */
@Mixin(value = PlayerStopRidingMessage.class, remap = false)
abstract class VehicleExitPacketMixin {
    @Inject(method = "handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void superbwarfareBoarding$voluntaryExit(Supplier<NetworkEvent.Context> context, CallbackInfo callback) {
        var packet = (PlayerStopRidingMessage) (Object) this;
        var player = context.get().getSender();
        if (!packet.getEjection() && player != null && DismountScenes.begin(player, () -> packet.handler(context))) {
            callback.cancel();
        }
    }
}
