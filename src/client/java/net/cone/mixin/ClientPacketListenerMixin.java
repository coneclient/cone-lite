package net.cone.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.cone.core.ConeCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientPacketListenerMixin {
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void cone$onDisconnect(DisconnectionDetails details, CallbackInfo ci) {
        ConeCore.reconnect().onDisconnect(cone$voluntary(details));
    }

    private boolean cone$voluntary(DisconnectionDetails details) {
        try {
            if (details != null && details.reason() != null
                    && details.reason().getContents() instanceof TranslatableContents t
                    && "disconnect.transfer".equals(t.getKey())) {
                return true;
            }
            return (Object) this instanceof ClientPacketListenerAccessor a && a.cone$closed();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
