package com.kltyton.superbwarfareboarding.mixin.client;

import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Samples the original perspective including the mounted-vantage hook before applying the cinematic. */
@Mixin(GameRenderer.class)
abstract class BoardingGameRendererMixin {
    @WrapOperation(method = {"renderLevel", "m_109089_"}, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", remap = false),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;m_90575_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", remap = false)
    }, remap = false)
    private void superbwarfareBoarding$blendCamera(Camera camera, BlockGetter level, Entity entity, boolean detached,
                                                    boolean mirror, float partialTick, Operation<Void> original) {
        var perspective = BoardingView.originalPerspective(entity);
        if (perspective == null) {
            original.call(camera, level, entity, detached, mirror, partialTick);
            BoardingView.observe(Minecraft.getInstance(), entity, camera);
            return;
        }
        var game = Minecraft.getInstance();
        var display = game.options.getCameraType();
        game.options.setCameraType(perspective);
        try {
            original.call(camera, level, entity, !perspective.isFirstPerson(), perspective.isMirrored(), partialTick);
        } finally {
            game.options.setCameraType(display);
        }
        BoardingView.apply(game, camera, partialTick);
    }
}
