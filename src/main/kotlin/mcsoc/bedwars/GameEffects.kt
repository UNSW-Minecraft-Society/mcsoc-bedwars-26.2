package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.getModData
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import kotlin.uuid.toKotlinUuid

// replace with config
// useful if method for switching teams is added
const val MAX_TEAM_PLAYERS = 4

object TeamEffects {
    // On start of game run this to add players (probably just active)
    fun createTeamsWithPlayers(level: ServerLevel, numTeams: Int) {
        val mod_level_data = level.getModData()
        val players = mod_level_data.getActivePlayers()
        mod_level_data.initialiseTeams(numTeams)
        val teams = mod_level_data.getActiveTeams()

        players.shuffled().forEachIndexed { index, player ->
            val team = teams[index % numTeams]
            mod_level_data.addPlayer(player.toKotlinUuid(), team)
        }
    }


    fun destroyBed(level: ServerLevel, team: Team) {
        val mod_level_data = level.getModData()
        mod_level_data.setBedAlive(team, false)
        for (player in mod_level_data.getPlayersInTeam(team)) {
            TODO()
            // other things related to bed destruction like title and sound
        }
    }
}