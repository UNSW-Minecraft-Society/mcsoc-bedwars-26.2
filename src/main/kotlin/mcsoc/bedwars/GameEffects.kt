package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.Team
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel

// replace with config
// useful if method for switching teams is added
const val MAX_TEAM_PLAYERS = 4

object TeamEffects {
    // On start of game run this to add players (probably just active)
    fun createTeamsWithPlayers(level: ServerLevel) {
        val mod_level_data = level.gameState
        val players = mod_level_data.getActivePlayers()
        val teams = mod_level_data.getActiveTeams()
        val num_teams = teams.size

        players.shuffled().forEachIndexed { index, player ->
            val team = teams[index % num_teams]
            mod_level_data.addPlayer(player, team, level.scoreboard, level.getPlayerByUUID(player)?.scoreboardName)
        }
    }
}

object GameEffects {
    fun triggerNewPeriod(level: ServerLevel, nextPeriod: GamePeriod) {
        val generatorTracker = level.generatorState
        when (nextPeriod) {
            GamePeriod.DIAMOND_II -> generatorTracker.upgradeGenerator(GeneratorType.DIAMOND)
            GamePeriod.EMERALD_II -> generatorTracker.upgradeGenerator(GeneratorType.EMERALD)
            GamePeriod.DIAMOND_III -> generatorTracker.upgradeGenerator(GeneratorType.DIAMOND)
            GamePeriod.EMERALD_III -> generatorTracker.upgradeGenerator(GeneratorType.EMERALD)
            GamePeriod.DEATHMATCH -> triggerDeathmatch(level)
            else -> {}
        }
    }

    fun triggerDeathmatch(level: ServerLevel) {
        val gameState = level.gameState
        // destroy remaining beds
        gameState.getActiveTeams().forEach { team ->
            if (!gameState.getBedDestroyed(team)) {
                gameState.setBedAlive(team, false)
            }
            
            for (player in gameState.getPlayersInTeam(team)) {
                level.getPlayerByUUID(player)?.sendSystemMessage(Component.literal("deathmatch has begun"))
                // todo other things related to bed destruction like title and sound
            }
        }
    }
}