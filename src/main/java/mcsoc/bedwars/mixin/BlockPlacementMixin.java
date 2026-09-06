package mcsoc.bedwars.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcsoc.bedwars.datatrackers.LevelData;


@Mixin(BlockBehaviour.class)
public class BlockPlacementMixin {
    @Inject(at = @At("HEAD"), method = "canBeReplaced", cancellable = true)
    public void preventBlockReplace(final BlockState state, final BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
        if ((context.getLevel() instanceof ServerLevel level) && 
                !state.getBlock().equals(Blocks.AIR) && (
                    state.getFluidState().isEmpty() ||
                    !LevelData.getBlockProtection(level).isBlockBreakAllowed(context.getClickedPos())
                )
        ) {
            cir.setReturnValue(false);
        }
    }
}
