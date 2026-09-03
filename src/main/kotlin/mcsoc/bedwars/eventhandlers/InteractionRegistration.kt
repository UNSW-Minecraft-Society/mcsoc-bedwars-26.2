package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.entities.CustomEntityInteractions
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult

/**
 * Function to register all item interaction events for the plugin
 */
fun registerItemCallbacks() {
    // Alive UseItemCallbacks
    UseItemCallback.EVENT.register {player, level, hand ->
        if (level is ServerLevel && level.gameState.isPlayerAlive(player)) {
            InteractionResult.SUCCESS
        } else {
            InteractionResult.PASS
        }
    }
}

fun registerEntityCallbacks() {
    UseEntityCallback.EVENT.register { player, level, hand, entity, hitResult ->
        return@register CustomEntityInteractions.triggerShopkeeperOpen(player, level, hand, entity)
    }
}
