package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.world.InteractionResult

/**
 * Function to register all item interaction events for the plugin
 */
fun registerItemCallbacks() {
    // Alive UseItemCallbacks
    UseItemCallback.EVENT.register {player, level, hand ->
        if (ModDataTracker.isPlayerAlive(player)) {
            InteractionResult.SUCCESS
        } else {
            InteractionResult.PASS
        }
    }
}
