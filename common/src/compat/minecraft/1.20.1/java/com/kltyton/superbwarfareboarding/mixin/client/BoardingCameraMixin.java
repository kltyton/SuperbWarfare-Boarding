package com.kltyton.superbwarfareboarding.mixin.client;

import com.kltyton.superbwarfareboarding.client.camera.BoardingCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Applies the cinematic pose using the game's third-person collision volume. */
@Mixin(Camera.class)
abstract class BoardingCameraMixin implements BoardingCamera {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void move(double forward, double up, double right);
    @Shadow public abstract Vec3 getPosition();
    @Shadow private double getMaxZoom(double distance) { throw new AssertionError(); }

    @Override
    public Vec3 superbwarfareBoarding$orbit(Vec3 focus, float yaw, float pitch, float distance) {
        setPosition(focus);
        setRotation(yaw, pitch);
        move(-getMaxZoom(distance), 0, 0);
        return getPosition();
    }

    @Override
    public void superbwarfareBoarding$apply(Vec3 position, float yaw, float pitch, boolean detached) {
        setPosition(position);
        setRotation(yaw, pitch);
        this.detached = detached;
    }
}
