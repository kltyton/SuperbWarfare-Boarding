package com.kltyton.superbwarfareboarding.mixin.client;

import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakerOptions;
import com.kltyton.superbwarfareboarding.boarding.CabDoor;
import com.kltyton.superbwarfareboarding.boarding.VehicleMotion;
import com.kltyton.superbwarfareboarding.client.model.BoardingGeometry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps targeted bones separate when Superb Warfare merges static vehicle geometry. */
@Mixin(value = VehicleModelReloadListener.class, remap = false)
abstract class BoardingVehicleBakeMixin {
    @WrapOperation(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v2/common/model/baked/BakerOptions;withPreservedBoneRegexes(Ljava/util/Set;)Lcom/github/mcmodderanchor/simplebedrockmodel/v2/common/model/baked/BakerOptions;", remap = false),
            remap = false)
    private BakerOptions superbwarfareBoarding$preserveHinges(BakerOptions options, Set<String> preserved,
                                                             Operation<BakerOptions> original) {
        var names = new HashSet<>(preserved);
        names.addAll(CabDoor.hinges());
        names.addAll(VehicleMotion.motionBones());
        return original.call(options, names);
    }

    @WrapOperation(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v2/common/model/baked/BakedBedrockModel;bake(Lcom/github/mcmodderanchor/simplebedrockmodel/v1/common/resource/pojo/BedrockModelPOJO;Lcom/github/mcmodderanchor/simplebedrockmodel/v2/common/model/baked/BakerOptions;)Lcom/github/mcmodderanchor/simplebedrockmodel/v2/common/model/baked/BakedBedrockModel;",
                    remap = false), remap = false)
    private BakedBedrockModel superbwarfareBoarding$placeHinges(BedrockModelPOJO model, BakerOptions options,
                                                               Operation<BakedBedrockModel> original,
                                                               @Local(argsOnly = true) ResourceManager manager,
                                                               @Local(index = 10) ResourceLocation location) {
        var gson = ((VehicleModelReloadListener) (Object) this).getGson();
        return original.call(BoardingGeometry.prepare(model, location, manager, gson), options);
    }
}
