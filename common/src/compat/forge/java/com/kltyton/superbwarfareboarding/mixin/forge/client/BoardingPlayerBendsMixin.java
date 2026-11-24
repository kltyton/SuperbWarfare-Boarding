package com.kltyton.superbwarfareboarding.mixin.forge.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.kltyton.superbwarfareboarding.boarding.DismountScenes;
import com.kltyton.superbwarfareboarding.client.compat.BoardingLegacyBends;
import com.kltyton.superbwarfareboarding.client.pose.VehicleRidingPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies optional legacy deformation during cutscenes and vehicle-specific riding poses while seated. */
@Mixin(LivingEntityRenderer.class)
abstract class BoardingPlayerBendsMixin {
    @Shadow protected EntityModel<?> model;
    @Unique private boolean superbwarfareBoarding$appliedRidingBends;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER))
    private void superbwarfareBoarding$bendPlayer(LivingEntity entity, float yaw, float tick, PoseStack stack,
                                                   MultiBufferSource buffers, int light, CallbackInfo callback) {
        superbwarfareBoarding$appliedRidingBends = false;
        if (!(model instanceof PlayerModel<?> player)) return;
        boolean hasBends = ModList.get().isLoaded("playeranimationlibrarymorerotation")
                && ModList.get().isLoaded("playeranimator") && ModList.get().isLoaded("bendylib");

        var actor = LegacyActorRenderer.current();
        if (actor != null && actor.visual() == entity
                && (actor.role().equals(BoardingScenes.RIDER) || actor.role().equals(DismountScenes.RIDER))) {
            if (hasBends) BoardingLegacyBends.apply(actor, player);
            return;
        }
        if (actor == null && entity instanceof Player rider && rider.getVehicle() instanceof VehicleEntity vehicle) {
            BoardingLegacyBends.applyRidingPose(VehicleRidingPose.sample(rider, vehicle, tick), player, hasBends);
            superbwarfareBoarding$appliedRidingBends = hasBends;
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void superbwarfareBoarding$clearRidingBends(LivingEntity entity, float yaw, float tick, PoseStack stack,
                                                        MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (superbwarfareBoarding$appliedRidingBends && model instanceof PlayerModel<?> player) {
            BoardingLegacyBends.clearRidingBends(player);
            superbwarfareBoarding$appliedRidingBends = false;
        }
    }
}
