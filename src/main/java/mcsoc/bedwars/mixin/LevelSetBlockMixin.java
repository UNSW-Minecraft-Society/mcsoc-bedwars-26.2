package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import mcsoc.bedwars.datatrackers.timetracker.GamePhase;
import mcsoc.bedwars.datatrackers.LevelData;


@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    private int MAX_HEIGHT = 50;

    @Inject(at = @At("HEAD"), method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState newState, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level)(Object)this;
        if (!(self instanceof ServerLevel level)) return;
        
        var block_protection = LevelData.getBlockProtection(level);

        /* if 
         * * block placement not allowed - fail
         * * block is replaced by itself - allow but don't track
         * * block is replaced and not replaceable - fail
        */

        var isAboveBuildHeigh = pos.getY() > LevelData.getMapData(level).getMap_centre().getY() + MAX_HEIGHT;
        var isGameActive = LevelData.getTimeState(level).getGamePhase().equals(GamePhase.ACTIVE);

        if (!(block_protection.isBlockPlacementAllowed(pos)) ||
            !(self.getBlockState(pos).canBeReplaced() || newState.is(BlockTags.AIR)) ||
            (isAboveBuildHeigh && isGameActive)
        ) { 
            cir.setReturnValue(false);
        } else if (!self.getBlockState(pos).getBlock().equals(newState.getBlock())) {
            block_protection.trackPlacedBlock(pos);
        }
    }
}