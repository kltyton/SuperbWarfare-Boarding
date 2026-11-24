package com.kltyton.superbwarfareboarding.platform;

import com.atsuishio.superbwarfare.network.message.send.PlayerStopRidingMessage;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "superbwarfare_boarding", value = Dist.CLIENT)
public final class BoardingClient {
    static {
        BoardingScenes.registerClientContact(BoardingView::ownsVehicleContact);
        BoardingView.registerDismountSkip(() -> PacketDistributor.sendToServer(new PlayerStopRidingMessage(false)));
    }

    private BoardingClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        BoardingView.tick(Minecraft.getInstance());
    }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        BoardingView.restore(Minecraft.getInstance());
    }
}
