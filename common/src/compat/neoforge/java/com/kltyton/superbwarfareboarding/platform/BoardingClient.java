package com.kltyton.superbwarfareboarding.platform;

import com.atsuishio.superbwarfare.network.message.send.PlayerStopRidingMessage;
import com.kltyton.superbwarfareboarding.boarding.BoardingScenes;
import com.kltyton.superbwarfareboarding.client.camera.BoardingView;
import com.kltyton.superbwarfareboarding.client.pose.PalRidingPoses;
import com.kltyton.superbwarfareboarding.client.ui.BoardingHud;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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
        if (ModList.get().isLoaded("player_animation_library")) {
            PalRidingPoses.register(ModList.get().isLoaded("playeranimationlibrarymorerotation"));
        }
    }

    private BoardingClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        BoardingView.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        BoardingHud.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void suppressSingleTapInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && BoardingView.isPlaying()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        BoardingView.restore(Minecraft.getInstance());
    }
}
