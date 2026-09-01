package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.getModData
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult

/**
 * Function to register all item interaction events for the plugin
 */
fun registerItemCallbacks() {
    // Alive UseItemCallbacks
    UseItemCallback.EVENT.register {player, level, hand ->
        if (level is ServerLevel && level.getModData().isPlayerAlive(player)) {
            InteractionResult.SUCCESS
        } else {
            InteractionResult.PASS
        }
    }
}
