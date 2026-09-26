package mcsoc.bedwars.gamestate

import mcsoc.bedwars.BedwarsPlugin
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
import kotlin.collections.forEach

import kotlin.time.Duration.Companion.seconds

const val BORDER_SIZE: Double = 300.0 // change if needed
val RESPAWN_TIME = 5.seconds

val RESPAWN_TIME_MESSAGE = {seconds_left: Int -> "${ChatFormatting.YELLOW}You will respawn in ${ChatFormatting.RED}${seconds_left} ${ChatFormatting.YELLOW}seconds!"}


private fun ServerLevel.getActivePlayers(): Iterable<ServerPlayer> = this.gameState.getActivePlayers().mapNotNull(this.server.playerList::getPlayer)


// TODO make these RNG
private fun ServerPlayer.getSelfDeathMessage(killer: UUID?): Component {
    if (killer != null && this.level().customEntityData.getEntityType(killer) != null) {
        return (this.displayName as MutableComponent)
            .append("${ChatFormatting.GRAY} tried to befriend a ")
            .append(getKillerName(this.level(), killer))
            .append("${ChatFormatting.GRAY}.")
    }
    return (this.displayName as MutableComponent)
        .append("${ChatFormatting.GRAY} should have been more careful!")
}
private fun ServerPlayer.getSelfFinalDeathMessage(killer: UUID?): Component {
    if (killer != null && this.level().customEntityData.getEntityType(killer) != null) {
        return (this.displayName as MutableComponent)
            .append("${ChatFormatting.GRAY} couldn't handle the ")
            .append(getKillerName(this.level(), killer))
            .append("${ChatFormatting.GRAY}.")
    }
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
        return Component.literal("${entityTeam?.chatColour ?: ChatFormatting.WHITE}$entityName")
    }
    return Component.literal("Someone")
}


class GameManager {
    companion object {
        fun setupGame(mapName: String, level: ServerLevel, startPos: BlockPos) {

            val gameState = level.gameState
            if (gameState.getGamePhase() != GamePhase.INACTIVE) {
                endGame(level)
            }

            BedwarsConfigData.placeMap(mapName, level, startPos)
            gameState.map_centre = startPos

            TeamEffects.createTeamsWithPlayers(level)
            // TODO val map = level_mod_data.getLoadedMapData()

            // set difficulty to peaceful/easy maybe?
            // maybe disable mob spawning

            val worldborder = level.worldBorder
            val pos = Vec3.atBottomCenterOf(startPos)
            worldborder.setCenter(pos.x(), pos.z())
            worldborder.size = BORDER_SIZE

            // distribute players to teams + reset player stuff
            for (team in gameState.getActiveTeams()) {
                val spawn = gameState.getTeamSpawn(team)
                for (player in gameState.getPlayersInTeam(team)) {
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
            gamerules.set(GameRules.NATURAL_HEALTH_REGENERATION, true, level.server)

            // sets time to sunrise (maybe change to noon?)
            val clock = level.dimensionType().defaultClock().orElseThrow()
            level.clockManager().setTotalTicks(clock, 0)

            // Clears the weather
            level.resetWeatherCycle()

            level.clock.reset()
            gameState.setGamePhase(GamePhase.STARTING)
            gameState.setGamePeriod(GamePeriod.INACTIVE)
            level.eventQueue.queueGameStartCounter(10.seconds)
        }

        fun endGame(level: ServerLevel) {
            // Display stats
            val gameState = level.gameState
            broadcastGameStats(level)

            // Triggered by command or on win condition, clean up stuff
            level.blockProtection.protectionEnabled = false

            val customEntityData = level.customEntityData
            level.eventQueue.reset()

            for (player in gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer)) {
                player.inventory.clearContent()
                player.setGameMode(GameType.SPECTATOR)
            }
            gameState.clearActivePlayers()
            // clear teams - todo

            gameState.setGamePhase(GamePhase.INACTIVE)
            gameState.setGamePeriod(GamePeriod.INACTIVE)

            // Clears Active players, all teams data and player data
            gameState.resetModData()
            level.generatorState.clearGenerators()
            level.generatorState.resetGenUpgrades()
            
            // Clear entities
            for (id in customEntityData.getEntityIds()) {
                val entity = level.getEntity(id)
                entity?.kill(level)
                customEntityData.removeEntity(id)
            }

            ScoreboardGui.clearScoreboard(level)

            level.worldBorder.size = ServerLevel.ACROSS_THE_WHOLE_WORLD.toDouble()
            level.worldBorder.setCenter(0.0, 0.0)
        }

        fun start(level: ServerLevel) {
            level.blockProtection.protectionEnabled = true

            val gameState = level.gameState
            gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{ player ->
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
                gameState.setPlayerAlive(player)
                player.setGameMode(GameType.SURVIVAL)
                val spawn = gameState.getTeamSpawn(gameState.getPlayersTeam(player.uuid))
                player.teleportTo(
                    level, spawn.x, spawn.y, spawn.z,
                    setOf(), 0F, 0F, true
                )
                player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(gameState.map_centre))
                player.inventory.clearContent()
                player.enderChestInventory.clearContent()
                player.health = player.maxHealth
                gameState.updateItems(player)
            }
            // tp players to spawn points
            // start generators
            // maybe show a title saying game begin or something
            // maybe a little tooltip in the bottom left
            ScoreboardGui.displayScoreboard(level)

            level.gameRules.set(GameRules.PVP, true, level.server)

            level.clock.reset()
            gameState.setGamePhase(GamePhase.ACTIVE)
            gameState.setGamePeriod(GamePeriod.INITIAL)
        }

        fun handlePlayerJoin(scoreboard: ServerScoreboard, player: ServerPlayer) {
            val gameState = player.level().gameState
            if (gameState.getGamePhase() != GamePhase.ACTIVE) return
            if (player.uuid !in gameState.getActivePlayers()) {
                player.inventory.clearContent()
                player.removeAllEffects()
                player.setGameMode(GameType.SPECTATOR)
                // set player team to spectator?
                scoreboard.removePlayerFromTeam(player.scoreboardName)
            }
        }

        fun handlePlayerDeath(player: ServerPlayer, death_source: DamageSource) {
            val level = player.level()
            val gameState = level.gameState
            if (gameState.getGamePhase() != GamePhase.ACTIVE) return

            val playerTeam = gameState.getPlayersTeam(player.uuid)
            val bedDestroyed = gameState.getBedDestroyed(playerTeam)

            // store player's death position to summon lightning later. Due to the nature of this event handler,
            // all players are forced to enter "DEAD" state upon death.
            gameState.setPlayerDead(player, player.position())
            
            val killer: UUID = (player.killCredit as? ServerPlayer)?.uuid ?: gameState.getBedBreaker(playerTeam) ?: run {
                val maybeKiller = death_source.entity?.uuid
                if (bedDestroyed) {
                    level.getActivePlayers().forEach{it.sendSystemMessage(player.getSelfFinalDeathMessage(maybeKiller))}
                } else {
                    level.getActivePlayers().forEach{it.sendSystemMessage(player.getSelfDeathMessage(maybeKiller))}
                }

                // Downgrade or like reset player item upgrades on death (as done below wow copy and paste how smart)
                player.inventory.clearContent()
                gameState.downgradeItems(player)
                gameState.incrementPlayerDeaths(player.uuid)

                return
            }
            // return BedwarsPlugin.LOGGER.error("handlePlayerDeath player: ${player.name.string}, source: ${death_source.msgId}: ", IllegalStateException("Cannot destroy bed without breaker?"))

            // Need to playtest see if final kill off void death transfers loot
            gameState.incrementPlayerKills(killer)

            if (bedDestroyed) {
                gameState.incrementPlayerFinalKills(killer)
                level.getActivePlayers().forEach{it.sendSystemMessage(player.getFinalKillMessage(killer))}
            } else {
                level.getActivePlayers().forEach{it.sendSystemMessage(player.getKillMessage(killer))}
            }

            val killer_player = player.level().getPlayerByUUID(killer);
            BedwarsPlugin.LOGGER.info("Gonna transfer items to ${killer_player}, is null: ${killer_player != null}")
            if (killer_player != null && gameState.isPlayerAlive(killer_player)) {
                for (stack in player.inventory) {
                    if (!stack.isEmpty && stack.item in arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD)) {
                        killer_player.inventory.add(stack)
                        BedwarsPlugin.LOGGER.info("adding $stack")
                    }
                }
            }

            // Downgrade or like reset player item upgrades on death
            player.inventory.clearContent()
            gameState.downgradeItems(player)
            gameState.incrementPlayerDeaths(player.uuid)
        }

        fun handlePlayerRespawn(player: ServerPlayer) {
            val gameState = player.level().gameState
            if (gameState.getGamePhase() != GamePhase.ACTIVE) return

            player.setGameMode(GameType.SPECTATOR)

            if (!gameState.getBedDestroyed(gameState.getPlayersTeam(player.uuid))) {
                player.level().eventQueue.queuePlayerRespawn(RESPAWN_TIME, player.uuid)
                // tp above map
                val respawnPosition: Vec3 = Vec3.atBottomCenterOf(gameState.map_centre.offset(0, 30, 0))
                player.teleportTo(player.level(), respawnPosition.x, respawnPosition.y, respawnPosition.z,
                        setOf(), 0F, 0F, true
                )

                gameState.setPlayerRespawning(player)
                gameState.resetPlayerRespawnTime(player)
            } else {
                eliminatePlayer(player)
            }
        }

        private fun eliminatePlayer(player: ServerPlayer) {
            val gameState = player.level().gameState
            val playerDeathPosition = gameState.getPlayerDeathPosition(player)

            gameState.setPlayerEliminated(player)
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
            lightning.setPos(playerDeathPosition)
            level.addFreshEntity(lightning)
            level.players().forEach { p ->
                p.sendSystemMessage(Component.literal(player.scoreboardName + " has been eliminated!"))
            }
            // notify eliminated player of their stats - change to align more closely to hypixel later
            // player.sendSystemMessage(Component.literal("Kills: " + level_mod_data.getPlayerKills(player) + " Final Kills: " + level_mod_data.getPlayerFinalKills(player) + " Deaths: " + level_mod_data.getPlayerDeaths(player)))

            val winningTeam = checkPlayersLeftOnTeam(level,gameState.getPlayersTeam(player.uuid)) ?: return
            winGame(level, winningTeam)
        }

        private fun checkPlayersLeftOnTeam(level: ServerLevel, playerDownTeam: Team): Team? {
            val gameState = level.gameState
            if (gameState.getGamePhase() != GamePhase.ACTIVE) return null
            var gameIsWon = true
            var winningTeam = Team.NONE
            gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{ player ->
                val playerTeam = gameState.getPlayersTeam(player.uuid)

                if (playerTeam == Team.NONE) return@forEach

                if (!(gameState.isPlayerEliminated(player))) {
                    if (winningTeam == Team.NONE) {
                        winningTeam = playerTeam
                    } else if (playerTeam != winningTeam) {
                        gameIsWon = false
                    }
                }
            }

            if (!gameIsWon) return null
            if (winningTeam == Team.NONE) throw IllegalStateException()
            return winningTeam
        }

        private fun compileGameStats(level: ServerLevel): MutableList<Component> {
            val gameState = level.gameState
            val statsList: MutableList<Component> = mutableListOf()
            statsList += Component.literal("${ChatFormatting.UNDERLINE}Game Statistics:")

            for (team in gameState.getActiveTeams()) {
                for (uuid in gameState.getPlayersInTeam(team)) {
                    val name = (level.getPlayerByUUID(uuid)?.name ?: Component.literal(uuid.toString())) as MutableComponent
                    statsList.add(name.append(Component.literal("\n" +
                            " - Kills: ${gameState.getPlayerKills(uuid)}\n" +
                            " - Final Kills: ${gameState.getPlayerFinalKills(uuid)}\n" +
                            " - Deaths: ${gameState.getPlayerDeaths(uuid)}\n" +
                            " - Beds Destroyed: ${gameState.getPlayerBedsDestroyed(uuid)}\n"
                    )))
                }
            }
            return statsList
        }

        private fun broadcastGameStats(level: ServerLevel) {
            val stats = compileGameStats(level)
            BedwarsPlugin.LOGGER.info("{}", stats.joinToString("\n"))
            for (player in level.players()) {
                stats.forEach{
                    player.sendSystemMessage(it)
                }
            }
        }

        private fun winGame(level: ServerLevel, winningTeam: Team) {
            val gameState = level.gameState

            gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{ player ->
                player.connection.send(
                    ClientboundClearTitlesPacket(true)
                )
                player.connection.send(
                    ClientboundSetTitlesAnimationPacket(0, 100, 0)
                )
                if (gameState.getPlayersTeam(player.uuid) == winningTeam) {
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

            }
            broadcastGameStats(level)
            ScoreboardGui.displayScoreboard(level)
            gameState.setGamePhase(GamePhase.ENDED)
            gameState.setGamePeriod(GamePeriod.INACTIVE)
        }

        fun afterBedBreak(level: ServerLevel, breaker: ServerPlayer, team: Team) {
            val gameState = level.gameState

            // Remnant bedhunt code to prevent afterBedBreak being called repeatedly after a bed is broken
            // Should not be needed if afterBedBreak is correctly called... after a bed break is registered
            // If bed breaking is detected every tick, something like this will be needed
            // if (!SavedModData.isTeamBaseIntact(team)) return

            gameState.setBedBreaker(team, breaker.uuid)
            gameState.incrementPlayerBedsDestroyed(breaker.uuid)
            gameState.setBedAlive(team, false)

            gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach { p ->
                if (gameState.getPlayersTeam(p.uuid) == team) {
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
            val gameState = level.gameState
            level.eventQueue.tick()
            if (gameState.getGamePhase() == GamePhase.INACTIVE) return

            if (gameState.getGamePhase() == GamePhase.ACTIVE) {
                for (i in 0 until level.clock.timerTick) {
                    level.generatorState.tick()
                    gameState.tick()
                    gameState.tickTeams(level)
                }
                ScoreboardGui.displayScoreboard(level)
            }

            val time = level.clock.time
            if (level.clock.timerSecond > 0) {
                if (gameState.getGamePhase() == GamePhase.ACTIVE) {
                    // periodic things to hit when game active
                    val nextPeriod = gameState.getGamePeriod().next
                    if (nextPeriod?.startTime != null && time >= nextPeriod.startTime) {
                        GameEffects.triggerNewPeriod(level, nextPeriod)
                        gameState.setGamePeriod(nextPeriod)
                        gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach { p ->
                            p.sendSystemMessage(Component.literal(nextPeriod.notif))
                        }
                    }
                }
            }
        }
    }
}