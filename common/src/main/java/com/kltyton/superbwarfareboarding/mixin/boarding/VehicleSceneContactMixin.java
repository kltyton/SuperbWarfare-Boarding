package com.kltyton.superbwarfareboarding.mixin.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMotionUtils;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves one rider's contact motion to its validated route during a seat handoff. */
@Mixin(value = VehicleMotionUtils.class, remap = false)
abstract class VehicleSceneContactMixin {
    @Inject(method = "handleEntityObbCollision", at = @At("HEAD"), cancellable = true, remap = false)
    private void superbwarfareBoarding$useSceneContact(VehicleEntity vehicle, Entity entity, CallbackInfo callback) {
        if (BoardingScenes.ownsVehicleContact(vehicle, entity)) callback.cancel();
    }
}
