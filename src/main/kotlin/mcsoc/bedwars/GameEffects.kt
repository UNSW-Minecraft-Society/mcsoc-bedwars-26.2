package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

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
            mod_level_data.addPlayer(player, team)
        }
    }


    fun destroyBed(level: ServerLevel, team: Team) {
        val mod_level_data = level.gameState
        mod_level_data.setBedAlive(team, false)
        for (player in mod_level_data.getPlayersInTeam(team)) {
            TODO()
            // other things related to bed destruction like title and sound
        }
    }
}