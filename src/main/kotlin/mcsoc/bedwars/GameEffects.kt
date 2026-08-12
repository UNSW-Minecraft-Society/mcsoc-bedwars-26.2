package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerPlayer


object TeamEffects {
    // replace with config
    const val MAX_TEAM_PLAYERS = 4
    
    fun createTeamsWithPlayers(players: List<ServerPlayer>, numTeams: Int) {
        ModDataTracker.initialiseNumTeams(numTeams)
        
        players.forEachIndexed { i, player ->
            ModDataTracker.addPlayer(player)
        }
    }
    
    
    fun destroyBed(team: Team) {
        ModDataTracker.setBedAlive(team, false)
        // other things related to bed destruction like title and sound
    }
}