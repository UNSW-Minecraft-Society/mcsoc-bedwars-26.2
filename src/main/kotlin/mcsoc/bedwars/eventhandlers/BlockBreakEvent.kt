package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.blockprotection.BlockProtectionTracker
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents

fun registerBlockBreakEvents() {
    PlayerBlockBreakEvents.BEFORE.register{level, player, pos, state, block_entity ->
        BlockProtectionTracker.isBlockBreakAllowed(pos)
    }
}