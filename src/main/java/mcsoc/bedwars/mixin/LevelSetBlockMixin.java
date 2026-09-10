package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import mcsoc.bedwars.datatrackers.LevelData;


@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    @Inject(at = @At("HEAD"), method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState newState, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level)(Object)this;
        if (!(self instanceof ServerLevel level)) return;
        
        var block_protection = LevelData.getBlockProtection(level);
        if (!(self.getBlockState(pos).canBeReplaced() || newState.is(BlockTags.AIR)) ||
                !(block_protection.isBlockPlacementAllowed(pos))
        ) {
            cir.setReturnValue(false);
        }
        block_protection.trackPlacedBlock(pos);
    }
}