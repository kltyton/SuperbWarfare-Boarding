package com.kltyton.superbwarfareboarding.mixin.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** During boarding, the reserved rider's scripted approach owns contact with this chair. */
@Mixin(value = WheelChairEntity.class, remap = false)
abstract class WheelchairTouchMixin {
    @Inject(method = {"playerTouch", "m_6123_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void superbwarfareBoarding$boardingContact(Player player, CallbackInfo callback) {
        var chair = (WheelChairEntity) (Object) this;
        if (player instanceof ServerPlayer rider && BoardingScenes.ownsChairContact(chair, rider)) callback.cancel();
    }
}
