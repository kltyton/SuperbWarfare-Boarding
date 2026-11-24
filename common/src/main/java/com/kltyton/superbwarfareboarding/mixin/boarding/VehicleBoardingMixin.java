package com.kltyton.superbwarfareboarding.mixin.boarding;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Delays the accepted mount operation without intercepting the vehicle's other interactions. */
@Mixin(value = VehicleEntity.class, remap = false)
abstract class VehicleBoardingMixin {
    @WrapOperation(method = {"interact", "m_6096_"}, at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startRiding(Lnet/minecraft/world/entity/Entity;)Z", remap = false),
        @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_20329_(Lnet/minecraft/world/entity/Entity;)Z", remap = false)
    }, remap = false)
    private boolean superbwarfareBoarding$deferMount(Player player, Entity mount, Operation<Boolean> original) {
        if (player instanceof ServerPlayer rider && mount instanceof VehicleEntity vehicle) {
            return BoardingScenes.board(rider, vehicle, () -> original.call(player, mount));
        }
        return original.call(player, mount);
    }
}
