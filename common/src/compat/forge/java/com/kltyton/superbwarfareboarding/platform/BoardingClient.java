package com.kltyton.superbwarfareboarding.platform;

import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.send.PlayerStopRidingMessage;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "superbwarfare_boarding", value = Dist.CLIENT)
public final class BoardingClient {
    static {
        BoardingScenes.registerClientContact(BoardingView::ownsVehicleContact);
        BoardingView.registerDismountSkip(() -> NetworkRegistry.PACKET_HANDLER.sendToServer(new PlayerStopRidingMessage(false)));
    }

    private BoardingClient() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) BoardingView.tick(Minecraft.getInstance());
    }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        BoardingView.restore(Minecraft.getInstance());
    }
}
