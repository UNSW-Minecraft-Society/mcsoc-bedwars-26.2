package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;


@Mixin(SnowLayerBlock.class)
public class SnowReplacementMixin {
    @Inject(at = @At("HEAD"), method = "canBeReplaced", cancellable = true)
    public void preventSnowReplace(BlockState state, BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
        if (!context.getItemInHand().is(Items.SNOW)) {
            cir.setReturnValue(false);
        }
    }
}
