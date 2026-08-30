package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.server.network.ServerPlayerConnection


fun registerPlayerJoinEvent() {
//    removed in favour of adding active players on game start
//    ServerPlayerEvents.JOIN.register { player ->
//        ModDataTracker.addActivePlayer(player.uuid)
//    }
}