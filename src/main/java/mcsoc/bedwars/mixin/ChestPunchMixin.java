package mcsoc.bedwars.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class ChestPunchMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void depositOnPunch(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return;
        
        Level level = this.player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) return;

        ItemStack held = this.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty()) return;

        Container container = ChestBlock.getContainer(chestBlock, state, level, pos, true);
        if (container == null) return;

        ItemStack leftover = insertIntoContainer(container, held.copy());

        if (leftover.getCount() != held.getCount()) {
            this.player.setItemInHand(InteractionHand.MAIN_HAND, leftover.isEmpty() ? ItemStack.EMPTY : leftover);
            container.setChanged();
        }

        ci.cancel();
    }

    // ye idek what this does
    private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space > 0) {
                    int move = Math.min(space, stack.getCount());
                    slotStack.grow(move);
                    stack.shrink(move);
                }
            }
        }

        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                int move = Math.min(stack.getMaxStackSize(), stack.getCount());
                container.setItem(i, stack.split(move));
            }
        }

        return stack;
    }
}