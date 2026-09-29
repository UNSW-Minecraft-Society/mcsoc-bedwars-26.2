package mcsoc.bedwars.mixin;

import mcsoc.bedwars.utils.ItemUtilsKt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void preventUndroppableHotbarDrop(boolean all, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ItemStack selected = self.getInventory().getSelectedItem();

        if (!selected.isEmpty() && ItemUtilsKt.hasTag(selected, "undroppable", "true")) {
            ci.cancel();
        }
    }
}