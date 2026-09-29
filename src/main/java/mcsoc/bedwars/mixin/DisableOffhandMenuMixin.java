package mcsoc.bedwars.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class DisableOffhandMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onMenuClicked(int slotId, int button, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            boolean isOffhandSwapHotkey = (containerInput == ContainerInput.SWAP && button == 40);
            boolean isDirectOffhandClick = (slotId == InventoryMenu.SHIELD_SLOT && serverPlayer.containerMenu == (AbstractContainerMenu) (Object) this);

            if (isOffhandSwapHotkey || isDirectOffhandClick) {
                ci.cancel();
                serverPlayer.containerMenu.broadcastChanges();
            }
        }
    }
}

