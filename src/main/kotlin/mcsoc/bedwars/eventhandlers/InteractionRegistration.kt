package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.items.CUSTOM_ITEM_TAG
import mcsoc.bedwars.items.CustomItemInteraction
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.projectile.ThrowableProjectile

/**
 * Function to register all item interaction events for the plugin
 */
fun registerItemCallbacks() {
    // Alive UseItemCallbacks
    UseItemCallback.EVENT.register {player, level, hand ->
        return@register CustomItemInteraction.triggerCustomItemEffect(player, level, hand)
    }
    UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
        return@register CustomItemInteraction.triggerCustomItemEffect(player, level, hand)
    }
    ThrowableProjectileTickCallback.EVENT.register { projectile ->
        return@register CustomItemInteraction.triggerCustomThrowableProjectileEffect(projectile)
    }
}

