package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.blockProtection
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.gamestate.GameManager
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.state.BlockState


private fun isBedBreakAllowed(level: ServerLevel, player: Player, pos: BlockPos, state: BlockState): Boolean {
    val game_data = level.gameState
    val team = game_data.getPlayersTeam(player.uuid)
    val bed_pos = game_data.getTeamBedPosition(team)
    return !(pos == bed_pos || pos.relative(BedBlock.getConnectedDirection(state)) == bed_pos)
}

fun registerBlockBreakEvents() {
    PlayerBlockBreakEvents.BEFORE.register{level, player, pos, state, block_entity ->
        if (level !is ServerLevel) return@register false
        
        (state.`is`(BlockTags.BEDS) && isBedBreakAllowed(level, player, pos, state)) || 
            level.blockProtection.isBlockBreakAllowed(pos)
    }

    PlayerBlockBreakEvents.AFTER.register{level, player, pos, state, entity ->
        if (level !is ServerLevel || player !is ServerPlayer) return@register
        val game_data = level.gameState
        
        if (state.`is`(BlockTags.BEDS)) {
            for (team in game_data.getActiveTeams()) {
                val bed_pos = game_data.getTeamBedPosition(team)
                if (pos == bed_pos || pos.relative(BedBlock.getConnectedDirection(state)) == bed_pos) {
                    GameManager.afterBedBreak(level, player, team)
                    break
                }
            }
        }
    }
}