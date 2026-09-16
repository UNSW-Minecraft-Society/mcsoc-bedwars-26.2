package mcsoc.bedwars.gui

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
        addLines(scoreboard, listOf("AAAA", "BBBB"))
    }

    fun getOrCreateDummyObjective(scoreboard: Scoreboard) : Objective {
        return scoreboard.getObjective(TITLE) ?: scoreboard.addObjective(TITLE,
            ObjectiveCriteria.DUMMY, Component.literal(TITLE),
            ObjectiveCriteria.RenderType.INTEGER, true, null)
    }

    private fun addLines(scoreboard: Scoreboard, lines: List<String>) {
        for ((index, line) in lines.withIndex()) {
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(line), getOrCreateDummyObjective(scoreboard)).set(index)
        }
    }

    fun clearScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard
        scoreboard.removeObjective(getOrCreateDummyObjective(scoreboard))
    }
}