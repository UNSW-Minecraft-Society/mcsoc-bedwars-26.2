package mcsoc.bedwars.mixin;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(RecipeManager.class)
public class DisableCraftingMixin {
    @Inject(at = @At("HEAD"), method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void blockCrafting(
        RecipeType<T> type, I input, Level level, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
            // handles players placing items in the crafting menu
            if (type == RecipeType.CRAFTING) {
                cir.setReturnValue(Optional.empty());
            }
        }
}