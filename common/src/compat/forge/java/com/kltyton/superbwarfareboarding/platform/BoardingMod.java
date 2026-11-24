package com.kltyton.superbwarfareboarding.platform;

import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("superbwarfare_boarding")
public final class BoardingMod {
    public BoardingMod() {
        BoardingScenes.initialize();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) BoardingScenes.tick(event.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> BoardingScenes.shutdown(event.getServer()));
    }
}
