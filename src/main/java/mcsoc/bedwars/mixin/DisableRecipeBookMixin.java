package mcsoc.bedwars.mixin;

import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class DisableRecipeBookMixin {
    @Inject(at = @At("HEAD"), method = "handlePlaceRecipe", cancellable = true)
    private void blockRecipe(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        // handles when players try to craft using the recipe book
            ci.cancel();
    }
}