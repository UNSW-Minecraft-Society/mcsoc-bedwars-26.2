package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.gamestate.GameManager
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.phys.Vec3
import kotlin.uuid.toKotlinUuid

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

fun onBedBreakAttempt() {
    PlayerBlockBreakEvents.BEFORE.register{level, player, pos, state, entity ->
        if (state.block !is BedBlock || level !is ServerLevel || level.gameState.getGamePhase() != GamePhase.ACTIVE || player !is ServerPlayer) {
            return@register true
        }

        val player_team = level.gameState.getPlayersTeam(player.uuid)
        level.gameState.getActiveTeams().forEach { team ->
            // get team bed position
            // val team_pos = level.gameState.getTeamBasePosition(team)
            // temp
            val team_pos = Vec3.ZERO

            val other_part = pos.relative(BedBlock.getConnectedDirection(state))

            // If the bed being broken is a team's bed
            if (team_pos == pos || team_pos == other_part) {
                if (player_team == team) {
                    // prevent player from breaking their own bed
                    return@register false
                } else {
                    GameManager.afterBedBreak(level, player, team)
                    return@register true
                }
            }
        }

        // If not one of the team's beds just let them break it
        return@register true
    }
}
