package mcsoc.bedwars.gamestate

import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.GameType

const val NUM_TEAMS = 2 // If we want to change number of teams later store this in data store and make a command able to change it while game inactive

class GameManager {
    companion object {
        fun startGame(world: ServerLevel) {
            setup(world)
            // countdown from 10

        }

        fun endGame(world: ServerLevel) {
            // Triggered by command or on win condition, clean up stuff


            ModDataTracker.setGamePhase(GamePhase.INACTIVE)
            world.players().forEach { p ->
//                p.teleportTo(x, y, z) tp to lobby coordinates... figure out later
                p.setGameMode(GameType.ADVENTURE)
            }
        }

        private fun setup(world: ServerLevel) {
            if (ModDataTracker.getGamePhase() != GamePhase.INACTIVE) {
                endGame(world)
            }

            ModDataTracker.initialiseTeams(NUM_TEAMS)

            // set active players -> take from list of players considered "ready"

            ModDataTracker.setGamePhase(GamePhase.STARTING)
        }

        fun tick(world: ServerLevel) {
            // some catch to start start the game

            ModDataTracker.setGamePhase(GamePhase.ACTIVE)

            // Add deathmatch later
        }
    }
}