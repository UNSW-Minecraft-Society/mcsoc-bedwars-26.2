package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.items.CUSTOM_ITEM_TAG
import mcsoc.bedwars.items.CustomItemInteraction
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionResult

/**
 * Function to register all item interaction events for the plugin
 */
fun registerItemCallbacks() {
    // Alive UseItemCallbacks
    UseItemCallback.EVENT.register {player, level, hand ->
        if (ModDataTracker.isPlayerAlive(player)) {
            BedwarsPlugin.LOGGER.info("Player is alive")
            val item = player.getItemInHand(hand)
            if (item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.contains(CUSTOM_ITEM_TAG) ?: false) {
                val value = item.get(DataComponents.CUSTOM_DATA)
                    ?.copyTag()?.getString(CUSTOM_ITEM_TAG)?.get()
                BedwarsPlugin.LOGGER.info("Item has $CUSTOM_ITEM_TAG $value")
                CustomItemInteraction.triggerCustomItemEffect(player, level, item, value)
            } else {
                return@register InteractionResult.FAIL
            }
        } else {
            return@register InteractionResult.PASS
        }
    }
}
