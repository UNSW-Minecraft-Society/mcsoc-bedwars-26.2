package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.blockProtection
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.level.ServerLevel

fun registerBlockBreakEvents() {
    PlayerBlockBreakEvents.BEFORE.register{level, player, pos, state, block_entity ->
        if (level !is ServerLevel) return@register false
        level.blockProtection.isBlockBreakAllowed(pos)
    }
}