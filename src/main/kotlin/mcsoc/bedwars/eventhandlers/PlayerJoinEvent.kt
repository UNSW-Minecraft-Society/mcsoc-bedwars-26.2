package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.server.network.ServerPlayerConnection


fun registerPlayerJoinEvent() {
    ServerPlayerEvents.JOIN.register { player ->
        ModDataTracker.addActivePlayer(player.uuid)
    }
}