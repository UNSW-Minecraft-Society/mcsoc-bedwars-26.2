package mcsoc.bedwars

import com.mojang.datafixers.util.Pair
import mcsoc.bedwars.datatrackers.*
import mcsoc.bedwars.datatrackers.timetracker.GamePeriod
import mcsoc.bedwars.entities.spawnWakingWither
import mcsoc.bedwars.gamestate.BORDER_SIZE
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.inWholeTicks
import mcsoc.bedwars.utils.sendTitle
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.*


// replace with config
// useful if method for switching teams is added
const val MAX_TEAM_PLAYERS = 4
const val MIN_DEATHMATCH_BORDER_SIZE = 5.0
val DEATHMATCH_BORDER_TIME by lazy {(GamePeriod.TERMINAL.startTime!! - GamePeriod.DEATHMATCH.startTime!!).inWholeTicks}

object TeamEffects {
    // On start of game run this to add players (probably just active)
    fun createTeamsWithPlayers(level: ServerLevel) {
        val playerState = level.playerState
        val teamState = level.teamState
        val players = level.activePlayers.get()
        val teams = teamState.getActiveTeams().toList()
        val num_teams = teams.size
        players.filter{playerState.getPlayersTeam(it) == Team.NONE}.shuffled().forEachIndexed { index, player ->
            val team = teams[index % num_teams]
            teamState.addPlayer(player, team, level.scoreboard, level.getPlayerByUUID(player)?.scoreboardName)
            playerState.setTeam(player, team)
        }
    }
    
    fun assignPlayersToTeam(level: ServerLevel, team: Team, players: Iterable<UUID>) {
        for (player in players) {
            level.activePlayers.add(player)
            level.teamState.addPlayer(player, team, level.scoreboard, level.getPlayerByUUID(player)?.scoreboardName)
            level.playerState.setTeam(player, team)
        }
    }
}

private val emptyArmourSlots: List<Pair<EquipmentSlot, ItemStack>> = listOf(
    Pair(EquipmentSlot.HEAD, ItemStack.EMPTY),
    Pair(EquipmentSlot.CHEST, ItemStack.EMPTY),
    Pair(EquipmentSlot.LEGS, ItemStack.EMPTY),
    Pair(EquipmentSlot.FEET, ItemStack.EMPTY),
)

object GameEffects {
    @JvmStatic
    fun ServerPlayer.broadcastInvisibility() {
        this.level().chunkSource.sendToTrackingPlayers(this, ClientboundSetEquipmentPacket(this.id, emptyArmourSlots))
    }

    fun triggerNewPeriod(level: ServerLevel, nextPeriod: GamePeriod) {
        val generatorState = level.generatorState
        when (nextPeriod) {
            GamePeriod.DIAMOND_II -> generatorState.upgradeGenerator(GeneratorType.DIAMOND)
            GamePeriod.EMERALD_II -> generatorState.upgradeGenerator(GeneratorType.EMERALD)
            GamePeriod.DIAMOND_III -> generatorState.upgradeGenerator(GeneratorType.DIAMOND)
            GamePeriod.EMERALD_III -> generatorState.upgradeGenerator(GeneratorType.EMERALD)
            GamePeriod.DEATHMATCH -> triggerDeathmatch(level)
            GamePeriod.TERMINAL -> triggerGameEnd(level)
            else -> {}
        }
    }

    fun triggerDeathmatch(level: ServerLevel) {
        val teamState = level.teamState
        teamState.getActiveTeams().forEach { team ->
            val bedPos = teamState.getTeamBedPosition(team)
            // destroy remaining beds
            if (!teamState.getBedDestroyed(team)) {
                teamState.setBedAlive(team, false)
                level.destroyBlock(bedPos, false)
            }
            // spawn dragon
            spawnWakingWither(level, Vec3.atCenterOf(bedPos.offset(0, 32, 0)))
            // notify players
            for (playerId in teamState.getPlayersInTeam(team)) {
                val player = level.getPlayerByUUID(playerId)
                player?.sendSystemMessage(Component.literal("All beds broken, wither spawned, world border shrinking."))
                if (player is ServerPlayer) {
                    player.sendTitle(Component.literal("${ChatFormatting.RED}DEATHMATCH"))
                }
            }
        }
        spawnWakingWither(level, Vec3.atCenterOf(level.mapData.map_centre.offset(0, 128, 0)))
        // shrink border
        val worldBorder = level.worldBorder
        worldBorder.lerpSizeBetween(BORDER_SIZE, MIN_DEATHMATCH_BORDER_SIZE, DEATHMATCH_BORDER_TIME, level.gameTime)
    }

    fun triggerGameEnd(level: ServerLevel) {
        level.getActivePlayers().forEach { it.kill(level) }
        GameManager.endGame(level)
    }
}