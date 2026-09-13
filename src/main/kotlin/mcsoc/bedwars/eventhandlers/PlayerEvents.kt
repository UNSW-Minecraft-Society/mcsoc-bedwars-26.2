package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.gamestate.GameManager
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.server.level.ServerPlayer


fun registerAfterDeathEvent() {
    ServerLivingEntityEvents.AFTER_DEATH.register{maybe_player, death_source ->
        if (maybe_player is ServerPlayer) GameManager.handlePlayerDeath(maybe_player, death_source)
    }
}


fun registerAfterRespawnEvent() {
    ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
        GameManager.handlePlayerRespawn(newPlayer)
    }
}


fun registerPlayerJoinEvent() {
    /* TODO have players given info on join
     * If joining between games, tell them to ready up with /bedwars join
     * If joining during a game, tell them to wait until this game finishes
     * ect. There could also be some thing to reroute them to any inactive worlds?
     */
}