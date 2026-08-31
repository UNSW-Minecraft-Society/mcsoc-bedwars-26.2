package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.items.CustomItemInteraction
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback

/**
 * Function to register all item interaction events for the plugin
 */
fun registerItemCallbacks() {
    // Alive UseItemCallbacks
    UseItemCallback.EVENT.register {player, level, hand ->
        return@register CustomItemInteraction.triggerCustomItemEffect(player, level, hand)
    }
    UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
        return@register CustomItemInteraction.triggerCustomItemEffect(player, level, hand, hitResult)
    }
    ThrowableProjectileTickCallback.EVENT.register { projectile ->
        return@register CustomItemInteraction.triggerCustomProjectileTickEffect(projectile)
    }
    ProjectileHitCallback.EVENT.register { projectile, result ->
        return@register CustomItemInteraction.triggerCustomProjectileHitEffect(projectile, result)
    }
}

