package mcsoc.bedwars.gamestate

import mcsoc.bedwars.GameEffects
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.blockProtection
import mcsoc.bedwars.datatrackers.clock
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.customEntityData
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.gui.ScoreboardGui
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.toBlockPos
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.*
import net.minecraft.server.ServerScoreboard
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.storage.LevelData
import net.minecraft.world.phys.Vec3
import java.util.UUID

import kotlin.time.Duration.Companion.seconds

const val BORDER_SIZE: Double = 300.0 // change if needed
val RESPAWN_TIME = 5.seconds

val RESPAWN_TIME_MESSAGE = {seconds_left: Int -> "${ChatFormatting.YELLOW}You will respawn in ${ChatFormatting.RED}${seconds_left} ${ChatFormatting.YELLOW}seconds!"}


private fun ServerLevel.getActivePlayers(): Iterable<ServerPlayer> = this.gameState.getActivePlayers().mapNotNull(this.server.playerList::getPlayer)


// TODO make these RNG
private fun ServerPlayer.getSelfDeathMessage(): Component {
    return (this.displayName as MutableComponent)
        .append("${ChatFormatting.GRAY} should have been more careful!")
}
private fun ServerPlayer.getSelfFinalDeathMessage(): Component {
    return (this.displayName as MutableComponent)
        .append("${ChatFormatting.GRAY} forgot that their bed was broken.")
}
private fun ServerPlayer.getKillMessage(killer: UUID): Component {
    return (this.displayName as MutableComponent)
        .append("${ChatFormatting.GRAY} slipped on ")
        .append(getKillerName(this.level(), killer))
        .append("${ChatFormatting.GRAY}'s banana peel.")
}
private fun ServerPlayer.getFinalKillMessage(killer: UUID): Component {
    return (this.displayName as MutableComponent)
        .append("${ChatFormatting.GRAY} was sent to the afterlife by ")
        .append(getKillerName(this.level(), killer))
        .append("${ChatFormatting.GRAY}.")
}
private fun getKillerName(level: ServerLevel, killer: UUID): Component {
    val maybePlayer = level.server.playerList.getPlayer(killer)
    val maybeCustomEntityType = level.customEntityData.getEntityType(killer)
    if (maybePlayer != null) {
        return maybePlayer.displayName
    } else if (maybeCustomEntityType != null) {
        val entityName = maybeCustomEntityType.title
        val entityTeam = level.customEntityData.getEntityTeam(killer)
        return Component.literal("${entityTeam?.chatColour ?: ""}$entityName")
    }
    return Component.literal("Someone")
}


class GameManager {
    companion object {
        fun setupGame(map_name: String, level: ServerLevel, start_pos: BlockPos) {

            val level_mod_data = level.gameState
            if (level_mod_data.getGamePhase() != GamePhase.INACTIVE) {
                endGame(level)
            }

            BedwarsConfigData.placeMap(map_name, level, start_pos)
            level_mod_data.map_centre = start_pos

            TeamEffects.createTeamsWithPlayers(level)
            // TODO val map = level_mod_data.getLoadedMapData()

            // set difficulty to peaceful/easy maybe?
            // maybe disable mob spawning

            val worldborder = level.worldBorder
            val pos = Vec3.atBottomCenterOf(start_pos)
            worldborder.setCenter(pos.x(), pos.z())
            worldborder.size = BORDER_SIZE

            // distribute players to teams + reset player stuff
            for (team in level_mod_data.getActiveTeams()) {
                val spawn = level_mod_data.getTeamSpawn(team)
                for (player in level_mod_data.getPlayersInTeam(team)) {
                    val player = level.server.playerList.getPlayer(player) ?: continue
                    player.setRespawnPosition(
                        ServerPlayer.RespawnConfig(
                            LevelData.RespawnData(
                                GlobalPos(
                                    level.dimension(), spawn.toBlockPos()
                                ), 0F, 0F
                            ), false
                        ), false
                    )
                }
            }

            // generate map?

            val gamerules = level.server.gameRules
            gamerules.set(GameRules.IMMEDIATE_RESPAWN, true, level.server)
            gamerules.set(GameRules.KEEP_INVENTORY, true, level.server)
            gamerules.set(GameRules.SPAWN_MOBS, false, level.server)
            gamerules.set(GameRules.ADVANCE_TIME, false, level.server)
            gamerules.set(GameRules.ADVANCE_WEATHER, false, level.server)
            gamerules.set(GameRules.SHOW_DEATH_MESSAGES, false, level.server)

            // sets time to sunrise (maybe change to noon?)
            val clock = level.dimensionType().defaultClock().orElseThrow()
            level.clockManager().setTotalTicks(clock, 0)

            // Clears the weather
            level.resetWeatherCycle()

            level.clock.reset()
            level_mod_data.setGamePhase(GamePhase.STARTING)
            level_mod_data.setGamePeriod(GamePeriod.INACTIVE)
            level.eventQueue.queueGameStartCounter(10.seconds)
        }

        fun endGame(level: ServerLevel) {
            // Triggered by command or on win condition, clean up stuff
            level.blockProtection.protectionEnabled = false
            
            val level_mod_data = level.gameState
            val level_mod_entity_data = level.customEntityData
            level.eventQueue.reset()
            level_mod_data.clearActivePlayers()
            // clear teams - todo

            level_mod_data.setGamePhase(GamePhase.INACTIVE)
            level_mod_data.setGamePeriod(GamePeriod.INACTIVE)
            for (player in level_mod_data.getActivePlayers().mapNotNull(level.server.playerList::getPlayer)) {
                player.inventory.clearContent()
                player.setGameMode(GameType.SPECTATOR)
            }

            // Clears Active players, all teams data and player data
            level_mod_data.resetModData()
            level.generatorState.clearGenerators()
            level.generatorState.resetGenUpgrades()
            
            // Clear entities
            for (id in level_mod_entity_data.getEntityIds()) {
                val entity = level.getEntity(id)
                entity?.kill(level)
                level_mod_entity_data.removeEntity(id)
            }

            ScoreboardGui.clearScoreboard(level)

            level.worldBorder.size = ServerLevel.ACROSS_THE_WHOLE_WORLD.toDouble()
            level.worldBorder.setCenter(0.0, 0.0)
        }

        fun start(level: ServerLevel) {
            level.blockProtection.protectionEnabled = true

            val level_mod_data = level.gameState
            level_mod_data.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{player ->
                player.connection.send(
                    ClientboundSetTitleTextPacket(
                        Component.literal("GO")
                    )
                )
                player.connection.send(
                    ClientboundSoundPacket(
                        Holder.direct(SoundEvents.BLAZE_SHOOT),
                        SoundSource.MASTER, player.x, player.y, player.z,
                        1.0F, 1.0F, level.getRandom().nextLong()
                    )
                )
                level_mod_data.setPlayerAlive(player)
                player.setGameMode(GameType.SURVIVAL)
                val spawn = level_mod_data.getTeamSpawn(level_mod_data.getPlayersTeam(player.uuid))
                player.teleportTo(
                    level, spawn.x, spawn.y, spawn.z,
                    setOf(), 0F, 0F, true
                )
                player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(level_mod_data.map_centre))
                player.inventory.clearContent()
                player.enderChestInventory.clearContent()
                player.health = player.maxHealth
                level_mod_data.updateItems(player)
            }
            // tp players to spawn points
            // start generators
            // maybe show a title saying game begin or something
            // maybe a little tooltip in the bottom left
            ScoreboardGui.displayScoreboard(level)

            level.clock.reset()
            level_mod_data.setGamePhase(GamePhase.ACTIVE)
            level_mod_data.setGamePeriod(GamePeriod.INITIAL)
        }

        fun handlePlayerJoin(scoreboard: ServerScoreboard, player: ServerPlayer) {
            val level_mod_data = player.level().gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return
            if (player.uuid !in level_mod_data.getActivePlayers()) {
                player.inventory.clearContent()
                player.removeAllEffects()
                player.setGameMode(GameType.SPECTATOR)
                // set player team to spectator?
                scoreboard.removePlayerFromTeam(player.scoreboardName)
            }
        }

        fun handlePlayerDeath(player: ServerPlayer, death_source: DamageSource) {
            val level = player.level()
            val level_mod_data = level.gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return

            val player_team = level_mod_data.getPlayersTeam(player.uuid)
            val bed_destroyed = level_mod_data.getBedDestroyed(player_team)

            // Downgrade or like reset player item upgrades on death
            player.inventory.clearContent()
            level_mod_data.downgradeItems(player)
            level_mod_data.incrementPlayerDeaths(player.uuid)

            // store player's death position to summon lightning later. Due to the nature of this event handler,
            // all players are forced to enter "DEAD" state upon death.
            level_mod_data.setPlayerDead(player, player.position())
            
            var killer: UUID = (player.killCredit as? ServerPlayer)?.uuid ?: level_mod_data.getBedBreaker(player_team) ?: run {
                if (bed_destroyed) {
                    level.getActivePlayers().forEach{it.sendSystemMessage(it.getSelfFinalDeathMessage())}
                } else {
                    level.getActivePlayers().forEach{it.sendSystemMessage(it.getSelfDeathMessage())}
                }
                return
            }
                    // return BedwarsPlugin.LOGGER.error("handlePlayerDeath player: ${player.name.string}, source: ${death_source.msgId}: ", IllegalStateException("Cannot destroy bed without breaker?"))

            // Need to playtest see if final kill off void death transfers loot
            level_mod_data.incrementPlayerKills(killer)

            if (bed_destroyed) {
                level_mod_data.incrementPlayerFinalKills(killer)
                level.getActivePlayers().forEach{it.sendSystemMessage(it.getFinalKillMessage(killer))}
            } else {
                level.getActivePlayers().forEach{it.sendSystemMessage(it.getKillMessage(killer))}
            }

            for (stack in player.inventory) {
                if (!stack.isEmpty && stack.item in arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD)) {
                    player.level().getPlayerByUUID(killer)?.inventory?.add(stack)
                }
            }
        }

        fun handlePlayerRespawn(player: ServerPlayer) {
            val level_mod_data = player.level().gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return

            player.setGameMode(GameType.SPECTATOR)

            if (!level_mod_data.getBedDestroyed(level_mod_data.getPlayersTeam(player.uuid))) {
                player.level().eventQueue.queuePlayerRespawn(RESPAWN_TIME, player.uuid)
                // tp above map
                val respawn_position: Vec3 = Vec3.atBottomCenterOf(level_mod_data.map_centre.offset(0, 30, 0))
                player.teleportTo(player.level(), respawn_position.x, respawn_position.y, respawn_position.z,
                        setOf(), 0F, 0F, true
                )

                level_mod_data.setPlayerRespawning(player)
                level_mod_data.resetPlayerRespawnTime(player)
            } else {
                eliminatePlayer(player)
            }
        }

        private fun eliminatePlayer(player: ServerPlayer) {
            val level_mod_data = player.level().gameState
            val player_death_position = level_mod_data.getPlayerDeathPosition(player)

            level_mod_data.setPlayerEliminated(player)
            player.connection.send(
                ClientboundClearTitlesPacket(true)
            )
            player.connection.send(
                ClientboundSetTitleTextPacket(
                    Component.literal(ChatFormatting.RED.toString() + "ELIMINATED")
                )
            )
            val level = player.level()
            val lightning = LightningBolt(EntityTypes.LIGHTNING_BOLT, level)
            lightning.setVisualOnly(true)
            lightning.setPos(player_death_position)
            level.addFreshEntity(lightning)
            level.players().forEach { p ->
                p.sendSystemMessage(Component.literal(player.scoreboardName + " has been eliminated!"))
            }
            // notify eliminated player of their stats - change to align more closely to hypixel later
            // player.sendSystemMessage(Component.literal("Kills: " + level_mod_data.getPlayerKills(player) + " Final Kills: " + level_mod_data.getPlayerFinalKills(player) + " Deaths: " + level_mod_data.getPlayerDeaths(player)))

            val winning_team = checkPlayersLeftOnTeam(level,level_mod_data.getPlayersTeam(player.uuid)) ?: return
            winGame(level, winning_team)
        }

        private fun checkPlayersLeftOnTeam(level: ServerLevel, player_down_team: Team): Team? {
            val level_mod_data = level.gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return null
            var game_is_won = true
            var winning_team = Team.NONE
            level_mod_data.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{player ->
                val player_team = level_mod_data.getPlayersTeam(player.uuid)

                if (player_team == Team.NONE) return@forEach

                if (!(level_mod_data.isPlayerEliminated(player))) {
                    if (winning_team == Team.NONE) {
                        winning_team = player_team
                    } else if (player_team != winning_team) {
                        game_is_won = false
                    }
                }
            }

            if (!game_is_won) return null
            if (winning_team == Team.NONE) throw IllegalStateException()
            return winning_team
        }

        private fun winGame(level: ServerLevel, winning_team: Team) {
            val level_mod_data = level.gameState
            val stats_list: MutableList<Component> = mutableListOf()

            for (team in level_mod_data.getActiveTeams()) {
                for (uuid in level_mod_data.getPlayersInTeam(team)) {
                    val name = (level.getPlayerByUUID(uuid)?.name ?: Component.literal(uuid.toString())) as MutableComponent
                    stats_list.add(name.append(Component.literal("\n" +
                        " - Kills: ${level_mod_data.getPlayerKills(uuid)}\n" +
                        " - Final Kills: ${level_mod_data.getPlayerFinalKills(uuid)}\n" +
                        " - Deaths: ${level_mod_data.getPlayerDeaths(uuid)}\n" +
                        " - Beds Destroyed: ${level_mod_data.getPlayerBedsDestroyed(uuid)}\n"
                    )))
                }
            }

            level_mod_data.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{player ->
                player.connection.send(
                    ClientboundClearTitlesPacket(true)
                )
                player.connection.send(
                    ClientboundSetTitlesAnimationPacket(0, 100, 0)
                )
                if (level_mod_data.getPlayersTeam(player.uuid) == winning_team) {
                    player.connection.send(
                        ClientboundSetTitleTextPacket(
                            Component.literal(ChatFormatting.YELLOW.toString() + "VICTORY!")
                        )
                    )
                } else {
                    player.connection.send(
                        ClientboundSetTitleTextPacket(
                            Component.literal(ChatFormatting.RED.toString() + "DEFEAT!")
                        )
                    )
                }

                stats_list.forEach(player::sendSystemMessage)
            }
            ScoreboardGui.displayScoreboard(level)
            level_mod_data.setGamePhase(GamePhase.ENDED)
            level_mod_data.setGamePeriod(GamePeriod.INACTIVE)
        }

        fun afterBedBreak(level: ServerLevel, breaker: ServerPlayer, team: Team) {
            val level_mod_data = level.gameState

            // Remnant bedhunt code to prevent afterBedBreak being called repeatedly after a bed is broken
            // Should not be needed if afterBedBreak is correctly called... after a bed break is registered
            // If bed breaking is detected every tick, something like this will be needed
            // if (!SavedModData.isTeamBaseIntact(team)) return

            level_mod_data.setBedBreaker(team, breaker.uuid)
            level_mod_data.incrementPlayerBedsDestroyed(breaker.uuid)
            level_mod_data.setBedAlive(team, false)

            level_mod_data.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach { p ->
                if (level_mod_data.getPlayersTeam(p.uuid) == team) {
                    p.connection.send(
                        ClientboundSetTitleTextPacket(
                            Component.literal(ChatFormatting.RED.toString() + "BED DESTROYED")
                        )
                    )

                }
                p.connection.send(
                    ClientboundSoundPacket(
                        Holder.direct(SoundEvents.ENDER_DRAGON_GROWL),
                        SoundSource.MASTER, p.x, p.y, p.z, 1.0F, 1.0F, level.getRandom().nextLong())
                )

                p.sendSystemMessage(Component.literal(team.name + " bed has been destroyed!"))
            }
        }

        fun tick(level: ServerLevel) {
            val level_mod_data = level.gameState
            level.eventQueue.tick()
            if (level_mod_data.getGamePhase() == GamePhase.INACTIVE) return

            if (level_mod_data.getGamePhase() == GamePhase.ACTIVE) {
                for (i in 0 until level.clock.timerTick) {
                    level.generatorState.tick()
                    level_mod_data.tick()
                    level_mod_data.tickTeams(level)
                }
                ScoreboardGui.displayScoreboard(level)
            }

            val time = level.clock.time
            if (level.clock.timerSecond > 0) {
                if (level_mod_data.getGamePhase() == GamePhase.ACTIVE) {
                    // periodic things to hit when game active
                    val nextPeriod = level_mod_data.getGamePeriod().next
                    if (nextPeriod?.startTime != null && time >= nextPeriod.startTime) {
                        GameEffects.triggerNewPeriod(level, nextPeriod)
                        level_mod_data.setGamePeriod(nextPeriod)
                    }
                }
            }
        }
    }
}