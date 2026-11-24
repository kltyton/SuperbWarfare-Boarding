package com.kltyton.superbwarfareboarding.client.camera;

import net.minecraft.world.phys.Vec3;

/** Camera behavior shared by the two Minecraft camera implementations. */
public interface BoardingCamera {
    Vec3 superbwarfareBoarding$orbit(Vec3 focus, float yaw, float pitch, float distance);
    void superbwarfareBoarding$apply(Vec3 position, float yaw, float pitch, boolean detached);
}
