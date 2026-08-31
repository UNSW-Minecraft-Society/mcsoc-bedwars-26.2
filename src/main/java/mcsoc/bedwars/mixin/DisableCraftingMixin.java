package mcsoc.bedwars.mixin;

import net.minecraft.recipe.RecipeInput;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(RecipeManager.class)
public class DisableCraftingMixin {
    @Inject(at = @At("HEAD"), method = "getFirstMatch", cancellable = true)
    private <I extends RecipeInput, T extends net.minecraft.recipe.Recipe<I>> void blockCrafting(RecipeType<T> type, I input, World world, CallbackInfoReturnable<Optional<ReciptEntry<T>>> cir) {
            if (type == RecipeType.CRAFTING) {
                cir.setReturnValue(Optional.empty());
            }
        }
}