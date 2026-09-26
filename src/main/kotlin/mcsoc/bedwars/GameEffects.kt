package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.entities.spawnWakingWither
import mcsoc.bedwars.gamestate.BORDER_SIZE
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.generators.GeneratorType
import com.mojang.datafixers.util.Pair
import mcsoc.bedwars.utils.Team
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.UUID


// replace with config
// useful if method for switching teams is added
const val MAX_TEAM_PLAYERS = 4
const val MIN_DEATHMATCH_BORDER_SIZE = 5.0
const val DEATHMATCH_BORDER_TIME = 5L*60*20

object TeamEffects {
    // On start of game run this to add players (probably just active)
    fun createTeamsWithPlayers(level: ServerLevel) {
        val mod_level_data = level.gameState
        val players = mod_level_data.getActivePlayers()
        val teams = mod_level_data.getActiveTeams().toList()
        BedwarsPlugin.LOGGER.info("List of teams ${teams.map{ team -> team.name }}")
        BedwarsPlugin.LOGGER.info("List of players $players")
        for (team in teams)
            BedwarsPlugin.LOGGER.info("Players in ${team.name} team ${mod_level_data.getPlayersInTeam(team)} (before)")
        val num_teams = teams.size
        players.filter{mod_level_data.getPlayersTeam(it) == Team.NONE}.shuffled().forEachIndexed { index, player ->
            BedwarsPlugin.LOGGER.info("Filtered player: $player")
            val team = teams[index % num_teams]
            mod_level_data.addPlayer(player, team, level.scoreboard, level.getPlayerByUUID(player)?.scoreboardName)
        }
        for (team in teams)
            BedwarsPlugin.LOGGER.info("Players in ${team.name} team ${mod_level_data.getPlayersInTeam(team)} (after)")
    }
    
    fun assignPlayersToTeam(level: ServerLevel, team: Team, players: Iterable<UUID>) {
        val mod_level_data = level.gameState
        for (player in players) {
            mod_level_data.addActivePlayer(player)
            mod_level_data.addPlayer(player, team, level.scoreboard, level.getPlayerByUUID(player)?.scoreboardName)
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
        val generatorTracker = level.generatorState
        when (nextPeriod) {
            GamePeriod.DIAMOND_II -> generatorTracker.upgradeGenerator(GeneratorType.DIAMOND)
            GamePeriod.EMERALD_II -> generatorTracker.upgradeGenerator(GeneratorType.EMERALD)
            GamePeriod.DIAMOND_III -> generatorTracker.upgradeGenerator(GeneratorType.DIAMOND)
            GamePeriod.EMERALD_III -> generatorTracker.upgradeGenerator(GeneratorType.EMERALD)
            GamePeriod.DEATHMATCH -> triggerDeathmatch(level)
            GamePeriod.TERMINAL -> triggerGameEnd(level)
            else -> {}
        }
    }

    fun triggerDeathmatch(level: ServerLevel) {
        val gameState = level.gameState
        gameState.getActiveTeams().forEach { team ->
            val bedPos = gameState.getTeamBedPosition(team)
            // destroy remaining beds
            if (!gameState.getBedDestroyed(team)) {
                gameState.setBedAlive(team, false)
                level.destroyBlock(bedPos, false)
            }
            // spawn dragon
            spawnWakingWither(level, Vec3.atCenterOf(bedPos.offset(0, 32, 0)))
            // notify players
            for (playerId in gameState.getPlayersInTeam(team)) {
                val player = level.getPlayerByUUID(playerId)
                player?.sendSystemMessage(Component.literal("All beds broken, wither spawned, world border shrinking."))
                if (player is ServerPlayer) {
                    player.connection.send(
                        ClientboundSetTitleTextPacket(
                            Component.literal("${ChatFormatting.RED}DEATHMATCH")
                        )
                    )
                }
            }
        }
        spawnWakingWither(level, Vec3.atCenterOf(gameState.map_centre.offset(0, 128, 0)))
        // shrink border
        val worldBorder = level.worldBorder
        worldBorder.lerpSizeBetween(BORDER_SIZE, MIN_DEATHMATCH_BORDER_SIZE, DEATHMATCH_BORDER_TIME, level.gameTime)
    }

    fun triggerGameEnd(level: ServerLevel) {
        val gameState = level.gameState
        for (playerId in gameState.getActivePlayers()) {
            val player = level.getPlayerByUUID(playerId) ?: continue
            player.kill(level)
        }
        GameManager.endGame(level)
    }
}