package com.kltyton.superbwarfareboarding.mixin.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.superbwarfareboarding.boarding.DismountScenes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains the original seat until the first outgoing movement frame. */
@Mixin(value = VehicleEntity.class, remap = false)
abstract class VehicleDismountLocationMixin {
    @Inject(method = {"getDismountLocationForPassenger", "m_7688_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void superbwarfareBoarding$departurePosition(LivingEntity passenger, CallbackInfoReturnable<Vec3> result) {
        Vec3 seat = DismountScenes.departureFrom((VehicleEntity) (Object) this, passenger);
        if (seat != null) result.setReturnValue(seat);
    }
}
