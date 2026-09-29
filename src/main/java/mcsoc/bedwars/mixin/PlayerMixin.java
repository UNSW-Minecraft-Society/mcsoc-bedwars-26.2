package mcsoc.bedwars.mixin;

import mcsoc.bedwars.utils.ItemUtilsKt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void preventUndroppableDrop(ItemStack stack, boolean dropAround, CallbackInfoReturnable ci) {
        Player self = (Player) (Object) this;

        if (!stack.isEmpty() && ItemUtilsKt.hasTag(stack, "undroppable", "true")) {
            self.getInventory().add(stack);
            self.containerMenu.broadcastChanges();
            ci.cancel();
        }
    }
}