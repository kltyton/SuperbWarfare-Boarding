package com.kltyton.superbwarfareboarding.mixin.forge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Restores the state Superb Warfare's custom item animation clock advances through its accessors. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.model.GeoModel", priority = 1100, remap = false)
abstract class SuperbWarfareAnimationClockMixin {
    @Unique private double animTime;
    @Unique private double lastGameTickTime;
}
