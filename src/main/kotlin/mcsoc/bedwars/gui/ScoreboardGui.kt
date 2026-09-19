package mcsoc.bedwars.gui

import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.clock
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.utils.formatMMSS
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria

object ScoreboardGui {
    private val TITLE = "Vedwars"

    fun displayScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard
        // Clear stuff
        clearScoreboard(level)
        // Set title
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, getOrCreateDummyObjective(scoreboard))
        // Add lines
        val lines = getLines(level)

        for ((index, line) in lines.withIndex()) {
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(line), getOrCreateDummyObjective(scoreboard)).set(lines.size - index)
        }
    }

    fun getOrCreateDummyObjective(scoreboard: Scoreboard) : Objective {
        return scoreboard.getObjective(TITLE) ?: scoreboard.addObjective(TITLE,
            ObjectiveCriteria.DUMMY, Component.literal(TITLE),
            ObjectiveCriteria.RenderType.INTEGER, true, null)
    }

    private fun getLines(level: ServerLevel): List<String> {
        val lines = mutableListOf<String>()

        lines += getGamePeriodLine(level)
        lines += ""
        for (team in level.gameState.getActiveTeams()) {
            var teamStatus : String
            if (level.gameState.getBedDestroyed(team)) {
                val teamPlayers = level.gameState.getPlayersInTeam(team)
                val numAlive = teamPlayers.count {
                    level.getPlayerByUUID(it)?.let { player -> !level.gameState.isPlayerEliminated(player) } ?: false
                }
                teamStatus = if (numAlive > 0) "$§e$numAlive left" else "§c✖ eliminated"
            } else teamStatus = "§a✔ bed active"
            lines += team.getName() + " " + teamStatus
        }
        return lines
    }

    private fun getGamePeriodLine(level: ServerLevel): String {
        val nextPeriod = level.gameState.getGamePeriod().next ?: return "err1"
        val time = level.clock.time - (nextPeriod.startTime ?: return "err2")
        return nextPeriod.title + " in " + time.formatMMSS
    }

    fun clearScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard
        scoreboard.removeObjective(getOrCreateDummyObjective(scoreboard))
    }
}