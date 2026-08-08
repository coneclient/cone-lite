package net.cone.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.cone.misc.NickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void cone$nick(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component v = cir.getReturnValue();
        if (v != null) cir.setReturnValue(NickManager.rewrite(v));
    }
}
