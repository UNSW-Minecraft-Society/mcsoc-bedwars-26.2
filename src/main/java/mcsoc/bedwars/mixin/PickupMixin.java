package mcsoc.bedwars.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public class PickupMixin {
    private static final double RADIUS = 2;

    @Inject(method = "playerTouch", at = @At("TAIL"))
    private void afterPickup(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            onPickup((ItemEntity) (Object) this, serverPlayer);
        }
    }

    private static void onPickup(ItemEntity item, ServerPlayer player) {
        if (!item.entityTags().contains("generator_item"))
            return;

        ServerLevel level = player.level();
        ItemStack stack = item.getItem().copy();

        level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(RADIUS))
                .forEach(p -> {
                    if (p == player)
                        return;

                    p.getInventory().add(stack.copy());
                    p.containerMenu.broadcastChanges();
                });
    }
}
