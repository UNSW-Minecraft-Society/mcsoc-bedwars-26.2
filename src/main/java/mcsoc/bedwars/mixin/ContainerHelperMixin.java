package mcsoc.bedwars.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


@Mixin(ContainerHelper.class)
public class ContainerHelperMixin {
   @Inject(at = @At("HEAD"), method = "removeItem", cancellable = true)
   private static void removeItem(final List<ItemStack> itemStacks, final int slot, final int count, CallbackInfoReturnable<ItemStack> cir) {
      try {
         ItemStack item = itemStacks.get(slot);
         if (item.is(ItemTags.MINING_ENCHANTABLE)) cir.setReturnValue(ItemStack.EMPTY);
         else if (item.is(ItemTags.SWORDS)) {
            itemStacks.set(slot, Items.WOODEN_SWORD.getDefaultInstance());
            cir.setReturnValue(item);
         }
      } finally {}
   }
}
