package com.kltyton.superbwarfareboarding.mixin.client;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.kltyton.interactiveanimation.client.playback.ClientActor;
import com.kltyton.interactiveanimation.client.render.LegacyActorRenderer;
import com.kltyton.superbwarfareboarding.boarding.CabDoor;
import com.kltyton.superbwarfareboarding.boarding.EntryPath;
import com.kltyton.superbwarfareboarding.boarding.VehicleMotion;
import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds only the boarding tracks to Superb Warfare's native vehicle and wheel poses. */
@Mixin(value = GeoVehicleRenderer.class, remap = false)
abstract class BoardingVehicleRendererMixin {
    @Unique
    private boolean superbwarfareBoarding$noBackfaceCull;

    @Inject(method = "render(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), remap = false)
    private void superbwarfareBoarding$beginRender(VehicleEntity vehicle, float entityYaw, float partialTick,
                                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                   CallbackInfo ci) {
        this.superbwarfareBoarding$noBackfaceCull = superbwarfareBoarding$disableBackfaceCull(vehicle);
    }

    @Inject(method = "render(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"), remap = false)
    private void superbwarfareBoarding$endRender(VehicleEntity vehicle, float entityYaw, float partialTick,
                                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                 CallbackInfo ci) {
        this.superbwarfareBoarding$noBackfaceCull = false;
    }

    @WrapOperation(method = "render(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/atsuishio/superbwarfare/client/model/entity/VehicleModelInstance;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/RenderType;II)V", remap = false), remap = false)
    private void superbwarfareBoarding$renderNoBackfaceCull(VehicleModelInstance model, PoseStack poseStack,
                                                            MultiBufferSource buffer, RenderType cubeRenderType,
                                                            RenderType meshRenderType, int packedLight,
                                                            int packedOverlay, Operation<Void> original) {
        if (this.superbwarfareBoarding$noBackfaceCull) {
            model.renderToBuffer(poseStack, buffer, cubeRenderType, meshRenderType, packedLight, packedOverlay,
                    1.0F, 1.0F, 1.0F, 1.0F, true);
            return;
        }
        original.call(model, poseStack, buffer, cubeRenderType, meshRenderType, packedLight, packedOverlay);
    }

    @WrapOperation(method = "renderEmissive",
            at = @At(value = "INVOKE", target = "Lcom/atsuishio/superbwarfare/client/model/entity/VehicleModelInstance;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/RenderType;II)V", remap = false), remap = false)
    private void superbwarfareBoarding$renderEmissiveNoBackfaceCull(VehicleModelInstance model, PoseStack poseStack,
                                                                    MultiBufferSource buffer, RenderType cubeRenderType,
                                                                    RenderType meshRenderType, int packedLight,
                                                                    int packedOverlay, Operation<Void> original) {
        if (this.superbwarfareBoarding$noBackfaceCull) {
            model.renderToBuffer(poseStack, buffer, cubeRenderType, meshRenderType, packedLight, packedOverlay,
                    1.0F, 1.0F, 1.0F, 1.0F, true);
            return;
        }
        original.call(model, poseStack, buffer, cubeRenderType, meshRenderType, packedLight, packedOverlay);
    }

    @Unique
    private static boolean superbwarfareBoarding$disableBackfaceCull(VehicleEntity vehicle) {
        if (BoardingView.isAnimating(vehicle)) return true;
        var actor = LegacyActorRenderer.current();
        return actor != null && actor.visual() == vehicle;
    }
    @WrapOperation(method = "render(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/atsuishio/superbwarfare/client/renderer/entity/GeoVehicleRenderer;transformCustomModelPart(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;Lcom/atsuishio/superbwarfare/client/model/entity/VehicleModelInstance;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", remap = false), remap = false)
    private void superbwarfareBoarding$vehiclePose(GeoVehicleRenderer<?> renderer, VehicleEntity vehicle,
                                          VehicleModelInstance model, PoseStack poseStack, float yaw,
                                          float partialTick, Operation<Void> original) {
        original.call(renderer, vehicle, model, poseStack, yaw, partialTick);
        var actor = LegacyActorRenderer.current();
        if (actor == null || actor.visual() != vehicle || !actor.role().equals("primary")) return;
        for (var entry : actor.bones().entrySet()) {
            String channel = entry.getKey();
            var pose = entry.getValue().sample(actor.seconds());
            if (channel.equals(VehicleMotion.WEIGHT)) {
                var bone = superbwarfareBoarding$bone(model, VehicleMotion.loadBearingBone(vehicle), actor);
                if (bone != null) superbwarfareBoarding$apply(bone, pose.position(), pose.rotation());
            } else if (channel.equals(VehicleMotion.ACCESS)) {
                Vec3 riderLocal = superbwarfareBoarding$riderLocal(vehicle, partialTick);
                for (var joint : VehicleMotion.accessJoints(vehicle, riderLocal)) {
                    var bone = superbwarfareBoarding$bone(model, joint.name(), actor);
                    if (bone == null) continue;
                    superbwarfareBoarding$restoreBone(bone);
                    Vec3 slide = joint.slidePerNinetyDeg();
                    if (slide.lengthSqr() > 1.0E-6) {
                        double fraction = pose.rotation().y / 90.0;
                        bone.x += (float) (slide.x * fraction);
                        bone.y += (float) (slide.y * fraction);
                        bone.z += (float) (slide.z * fraction);
                    }
                    Vec3 axis = joint.openingAxis();
                    double ratio = axis.length();
                    if (ratio > 1.0E-6) {
                        bone.rotation.rotateAxis((float) Math.toRadians(pose.rotation().y * ratio),
                                (float) (-axis.x / ratio), (float) (-axis.y / ratio), (float) (axis.z / ratio));
                    }
                }
            } else {
                var side = CabDoor.forTrack(channel);
                if (side == null) continue;
                var name = side.modelBone(vehicle);
                if (name == null) continue;
                var bone = superbwarfareBoarding$bone(model, name, actor);
                if (bone != null) superbwarfareBoarding$apply(bone, Vec3.ZERO, pose.rotation());
            }
        }
    }

    @Unique
    private static Vec3 superbwarfareBoarding$riderLocal(VehicleEntity vehicle, float partialTick) {
        for (var player : vehicle.level().players()) {
            if (BoardingView.ownsVehicleContact(vehicle, player)) {
                return EntryPath.toLocal(vehicle, player.getPosition(partialTick));
            }
        }
        return null;
    }

    @Unique
    private static BoneState superbwarfareBoarding$bone(VehicleModelInstance model, String name, ClientActor actor) {
        var bone = model.getBone(name);
        if (bone == null) actor.missingBone(name);
        return bone;
    }

    @Unique
    private static void superbwarfareBoarding$apply(BoneState bone, Vec3 position, Vec3 angles) {
        superbwarfareBoarding$restoreBone(bone);
        bone.x -= (float) position.x;
        bone.y += (float) position.y;
        bone.z += (float) position.z;
        bone.rotation.rotateXYZ((float) -Math.toRadians(angles.x), (float) -Math.toRadians(angles.y),
                (float) Math.toRadians(angles.z));
    }

    @Unique
    private static void superbwarfareBoarding$restoreBone(BoneState bone) {
        float x = bone.x, y = bone.y, z = bone.z;
        Quaternionf original = new Quaternionf(bone.rotation);
        LegacyActorRenderer.restoreAfterDraw(() -> {
            bone.x = x;
            bone.y = y;
            bone.z = z;
            bone.rotation.set(original);
        });
    }
}
