package mcsoc.bedwars.upgrades

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

enum class TrapUpgrade {
    BLINDNESS,
    COUNTER,
    REVEAL,
    MINING;
    
    fun doTrap(level: ServerLevel, players: List<ServerPlayer>) {
        // todo, should be abstract and each trap implements their own thing
    }
}