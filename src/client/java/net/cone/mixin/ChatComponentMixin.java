package net.cone.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.cone.misc.NickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyVariable(method = "addPlayerMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component cone$nickPlayer(Component msg) {
        return NickManager.rewrite(msg);
    }

    @ModifyVariable(method = "addServerSystemMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component cone$nickServer(Component msg) {
        return NickManager.rewrite(msg);
    }

    @ModifyVariable(method = "addClientSystemMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component cone$nickClient(Component msg) {
        return NickManager.rewrite(msg);
    }
}
