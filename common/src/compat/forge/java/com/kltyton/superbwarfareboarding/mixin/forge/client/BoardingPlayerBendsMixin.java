package com.kltyton.superbwarfareboarding.mixin.forge.client;

import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.kltyton.superbwarfareboarding.boarding.DismountScenes;
import com.kltyton.superbwarfareboarding.client.compat.BoardingLegacyBends;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the optional legacy deformation after the native player setup has run. */
@Mixin(LivingEntityRenderer.class)
abstract class BoardingPlayerBendsMixin {
    @Shadow protected EntityModel<?> model;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER))
    private void superbwarfareBoarding$bendPlayer(LivingEntity entity, float yaw, float tick, PoseStack stack,
                                                   MultiBufferSource buffers, int light, CallbackInfo callback) {
        var actor = LegacyActorRenderer.current();
        if (actor == null || actor.visual() != entity || !(model instanceof PlayerModel<?> player)
                || !(actor.role().equals(BoardingScenes.RIDER) || actor.role().equals(DismountScenes.RIDER))) return;
        if (ModList.get().isLoaded("playeranimationlibrarymorerotation")
                && ModList.get().isLoaded("playeranimator") && ModList.get().isLoaded("bendylib")) {
            BoardingLegacyBends.apply(actor, player);
        }
    }
}
