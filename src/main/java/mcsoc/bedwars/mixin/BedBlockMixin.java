package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import mcsoc.bedwars.datatrackers.GamePeriod;
import mcsoc.bedwars.datatrackers.LevelData;


@Mixin(BedBlock.class)
public class BedBlockMixin {
    @Inject(at = @At("HEAD"), method = "useWithoutItem", cancellable = true) 
    public void removeUse(BlockState state, final Level level, BlockPos pos, final Player player, final BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level instanceof ServerLevel slevel && LevelData.getGameState(slevel).getGamePeriod() != GamePeriod.INACTIVE) cir.setReturnValue(InteractionResult.PASS);
    }
}
