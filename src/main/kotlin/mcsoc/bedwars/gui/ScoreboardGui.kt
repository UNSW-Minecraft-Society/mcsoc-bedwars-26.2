package mcsoc.bedwars.gui

import mcsoc.bedwars.datatrackers.gameState
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria

object ScoreboardGui {
    private val TITLE = "Vedwars"

    fun initialiseScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard

        // Set title
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, getOrCreateDummyObjective(scoreboard))
        // Add lines
        displayLines(level)
    }

    fun getOrCreateDummyObjective(scoreboard: Scoreboard) : Objective {
        return scoreboard.getObjective(TITLE) ?: scoreboard.addObjective(TITLE,
            ObjectiveCriteria.DUMMY, Component.literal(TITLE),
            ObjectiveCriteria.RenderType.INTEGER, true, null)
    }

    private fun displayLines(level: ServerLevel) {
        val scoreboard = level.scoreboard
        val lines = getLines(level)
        for ((index, line) in lines.withIndex()) {
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(line), getOrCreateDummyObjective(scoreboard)).set(lines.size - index)
        }
    }

    private fun getLines(level: ServerLevel): List<String> {
        val lines = mutableListOf<String>()
        lines += "Imagine this works:"
        lines += "12:25"
        lines += ""
        for (team in level.gameState.getActiveTeams()) {
            var teamStatus : String
            if (level.gameState.getBedDestroyed(team)) {
                val teamPlayers = level.gameState.getPlayersInTeam(team)
                val numAlive = teamPlayers.filter {
                    level.getPlayerByUUID(it)?.let { player -> !level.gameState.isPlayerEliminated(player) } ?: false
                }.count()
                teamStatus = "$numAlive left"
            } else teamStatus = "bed active"
            lines += team.getName() + teamStatus
        }
        return lines
    }

    fun clearScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard
        scoreboard.removeObjective(getOrCreateDummyObjective(scoreboard))
    }
}