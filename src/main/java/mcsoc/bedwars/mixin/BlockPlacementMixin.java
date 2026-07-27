package mcsoc.bedwars.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcsoc.bedwars.datatrackers.ModDataTracker;


@Mixin(BlockBehaviour.class)
public class BlockPlacementMixin {
    @Inject(at = @At("HEAD"), method = "onPlace", cancellable = true)
    public void checkIfBlockPlacementIsValid(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston, CallbackInfo ci) {
        if (!ModDataTracker.INSTANCE.isBlockPlacementAllowed(pos)) {
            ci.cancel();
        }
        ModDataTracker.INSTANCE.trackPlacedBlock(pos);
    }
}
