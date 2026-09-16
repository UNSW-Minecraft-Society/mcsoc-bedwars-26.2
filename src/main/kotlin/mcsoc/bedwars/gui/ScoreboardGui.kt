package mcsoc.bedwars.gui

import mcsoc.bedwars.datatrackers.gameState
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria

object ScoreboardGui {
    private val objectives: MutableList<Objective> = mutableListOf()

    fun initialiseScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard

        objectives += scoreboard.addDummyObjective("Testig")
        objectives += scoreboard.addDummyObjective("Testig2")

        display(level)
    }

    fun Scoreboard.addDummyObjective(name: String, displayName: Component) = this.addObjective(
        name, ObjectiveCriteria.DUMMY, Component.literal(name),
        ObjectiveCriteria.RenderType.INTEGER, true, null
    )

    fun clearScoreboard(level: ServerLevel) {
        val scoreboard = level.scoreboard
        for (objective in objectives)
            scoreboard.removeObjective(objective)
        objectives.clear()
    }

    fun Scoreboard.addDummyObjective(name: String) = this.addDummyObjective(name, Component.literal(name))

    fun display(level: ServerLevel) {
        val scoreboard = level.scoreboard
        for (objective in objectives) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective)
        }
    }
}