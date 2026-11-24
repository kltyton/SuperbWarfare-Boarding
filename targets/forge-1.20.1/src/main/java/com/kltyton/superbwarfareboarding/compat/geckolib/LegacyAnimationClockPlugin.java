package com.kltyton.superbwarfareboarding.compat.geckolib;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Applies the Forge-only clock storage when the installed upstream pair needs it. */
public final class LegacyAnimationClockPlugin implements IMixinConfigPlugin {
    private static final String ACCESSOR = "com.atsuishio.superbwarfare.mixins.GeoModelAccessor";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var classes = MixinService.getService().getBytecodeProvider();
        try {
            var accessor = classes.getClassNode(ACCESSOR);
            boolean legacyClock = accessor.methods.stream()
                    .anyMatch(method -> method.name.equals("setAnimTime") && method.desc.equals("(D)V"))
                    && accessor.methods.stream()
                    .anyMatch(method -> method.name.equals("setLastGameTickTime") && method.desc.equals("(D)V"));
            if (!legacyClock) return false;
            var model = classes.getClassNode(targetClassName);
            return !clockField(model, "animTime") || !clockField(model, "lastGameTickTime");
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read the Superb Warfare and GeckoLib animation clock contract", error);
        }
    }

    private static boolean clockField(ClassNode model, String name) {
        return model.fields.stream().anyMatch(field -> field.name.equals(name) && field.desc.equals("D"));
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> targets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode target, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode target, String mixinClassName, IMixinInfo mixinInfo) {}
}
