package mcsoc.bedwars.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;


@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Inject(at = @At("HEAD"), method = "getDrops", cancellable = true)
    protected void getDrops(final BlockState state, final LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        
    } 
}
