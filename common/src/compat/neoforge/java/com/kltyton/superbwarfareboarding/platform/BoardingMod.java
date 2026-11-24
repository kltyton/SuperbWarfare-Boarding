package com.kltyton.superbwarfareboarding.platform;

import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("superbwarfare_boarding")
public final class BoardingMod {
    public BoardingMod() {
        BoardingScenes.initialize();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> BoardingScenes.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> BoardingScenes.shutdown(event.getServer()));
    }
}
