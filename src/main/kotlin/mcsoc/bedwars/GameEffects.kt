package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerPlayer
import kotlin.uuid.toKotlinUuid

// replace with config
// useful if method for switching teams is added
const val MAX_TEAM_PLAYERS = 4

object TeamEffects {
    // On start of game run this to add players (probably just active)
    fun createTeamsWithPlayers(numTeams: Int) {
        val players = ModDataTracker.getActivePlayers()
        ModDataTracker.initialiseTeams(numTeams)
        val teams = ModDataTracker.getActiveTeams()

        players.shuffled().forEachIndexed { index, player ->
            val team = teams[index % numTeams]
            ModDataTracker.addPlayer(player.toKotlinUuid(), team)
        }
    }


    fun destroyBed(team: Team) {
        ModDataTracker.setBedAlive(team, false)
        // other things related to bed destruction like title and sound
    }

    fun triggerDeathmatch() {
        // destroy remaining beds
        ModDataTracker.getActiveTeams().forEach { team ->
            if (!ModDataTracker.getBedDestroyed(team)) {
                destroyBed(team)
            }
        }
        // close border
        // spawn dragons

    }
}