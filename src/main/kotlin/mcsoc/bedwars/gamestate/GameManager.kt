package mcsoc.bedwars.gamestate

import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.blockProtection
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.toBlockPos
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.Position
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.Relative
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration.Companion.minutes


val DEATHMATCH_TIME = 10.minutes // change if i'm wrong
const val BORDER_SIZE: Double = 300.0 // change if needed
const val RESPAWN_TIME: Int = 5

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
                    player.teleportTo(
                        level, spawn.x, spawn.y, spawn.z, 
                        setOf(), 0F, 0F, true
                    )
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, pos)
                }
            }

            // generate map?

            val gamerules = level.server.gameRules
            gamerules.set(GameRules.IMMEDIATE_RESPAWN, true, level.server)
            gamerules.set(GameRules.KEEP_INVENTORY, true, level.server)

            level_mod_data.resetGameTime()
            level_mod_data.setGamePhase(GamePhase.STARTING)
        }

        fun endGame(level: ServerLevel) {
            // Triggered by command or on win condition, clean up stuff
            level.blockProtection.protectionEnabled = false
            
            val level_mod_data = level.gameState
            level_mod_data.clearActivePlayers()
            level.generatorState.clearGenerators()
            // clear teams - todo

            level_mod_data.setGamePhase(GamePhase.INACTIVE)
            level_mod_data.setGamePeriod(GamePeriod.INACTIVE)
            for (player in level.players()) {
                player.setGameMode(GameType.SPECTATOR)
            }

            level.worldBorder.size = ServerLevel.ACROSS_THE_WHOLE_WORLD.toDouble()
            level.worldBorder.setCenter(0.0, 0.0)
        }

        private fun start(level: ServerLevel) {
            level.blockProtection.protectionEnabled = true
            
            val player_manager = level.server.playerList
            val level_mod_data = level.gameState
            for (player_uuid in level_mod_data.getActivePlayers()) {
                val player = player_manager.getPlayer(player_uuid) ?: continue
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
                player.teleportTo(spawn.x, spawn.y, spawn.z)
            }
            // tp players to spawn points
            // start generators
            // maybe show a title saying game begin or something
            // maybe a little tooltip in the bottom left
            level_mod_data.resetGameTime()
            level_mod_data.setGamePhase(GamePhase.ACTIVE)
            level_mod_data.setGamePeriod(GamePeriod.ACTIVE)
        }

        fun handlePlayerDeath(player: ServerPlayer, death_source: DamageSource) {
            val level_mod_data = player.level().gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return

            val player_team = level_mod_data.getPlayersTeam(player.uuid)
            val bed_destroyed = level_mod_data.getBedDestroyed(player_team)

            var killer = player.killCredit

            // Need to playtest see if final kill off void death transfers loot

            if (killer is ServerPlayer) {
                level_mod_data.setPlayerKills(killer, level_mod_data.getPlayerKills(killer) + 1)

                if (bed_destroyed) {
                    level_mod_data.setPlayerFinalKills(killer, level_mod_data.getPlayerFinalKills(killer) + 1)
                }

                player.inventory.forEach{ stack ->
                    if (!stack.isEmpty) {
                        if (stack.item in arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD)) {
                            killer.inventory.add(stack)
                        }
                    }
                }
            } else if (bed_destroyed) {
                killer = player.level().getPlayerByUUID(level_mod_data.getBedBreaker(player_team) ?: throw IllegalArgumentException("Cannot destroy bed without breaker?")) as ServerPlayer
                level_mod_data.setPlayerKills(killer, level_mod_data.getPlayerKills(killer) + 1)
                level_mod_data.setPlayerFinalKills(killer, level_mod_data.getPlayerFinalKills(killer) + 1)
            }

            // Downgrade or like reset player item upgrades on death
            player.inventory.clearContent()
            level_mod_data.downgradeItems(player)
            level_mod_data.setPlayerDeaths(player, level_mod_data.getPlayerDeaths(player) + 1)

            // store player's death position to summon lightning later. Due to the nature of this event handler,
            // all players are forced to enter "DEAD" state upon death.
            level_mod_data.setPlayerDead(player, player.position())
        }

        fun handlePlayerRespawn(player: ServerPlayer) {
            val level_mod_data = player.level().gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return

            level_mod_data.downgradeItems(player)

            player.setGameMode(GameType.SPECTATOR)

            if (!level_mod_data.getBedDestroyed(level_mod_data.getPlayersTeam(player.uuid))) {
                // tp above map
                val respawn_position: Vec3 = Vec3.atBottomCenterOf(level_mod_data.map_centre.offset(0, 30, 0))
                player.teleportTo(respawn_position.x, respawn_position.y, respawn_position.z)

                level_mod_data.setPlayerRespawning(player)
                level_mod_data.resetPlayerRespawnTime(player)
                val respawn_time_message = ChatFormatting.YELLOW.toString() + "You will respawn in " + ChatFormatting.RED.toString() + RESPAWN_TIME.toString() + ChatFormatting.YELLOW.toString() + " seconds!"
                player.connection.send(
                    ClientboundSetTitlesAnimationPacket(0, 30, 0)
                )
                player.connection.send(
                    ClientboundSetSubtitleTextPacket(
                        Component.literal(respawn_time_message)
                    )
                )
                player.connection.send(
                    ClientboundSetTitleTextPacket(
                        Component.literal((ChatFormatting.RED.toString() + "YOU DIED!"))
                    )
                )
                player.sendSystemMessage(Component.literal(respawn_time_message))
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
            val world = player.level()
            val lightning = LightningBolt(EntityTypes.LIGHTNING_BOLT, world)
            lightning.setVisualOnly(true)
            lightning.setPos(player_death_position)
            world.addFreshEntity(lightning)
            world.players().forEach { p ->
                p.sendSystemMessage(Component.literal(player.scoreboardName + " has been eliminated!"))
            }
            // notify eliminated player of their stats - change to align more closely to hypixel later
            player.sendSystemMessage(Component.literal("Kills: " + level_mod_data.getPlayerKills(player) + " Final Kills: " + level_mod_data.getPlayerFinalKills(player) + " Deaths: " + level_mod_data.getPlayerDeaths(player)))

            val winning_team = checkPlayersLeftOnTeam(world,level_mod_data.getPlayersTeam(player.uuid)) ?: return
            winGame(world, winning_team)
        }

        private fun checkPlayersLeftOnTeam(world: ServerLevel, player_down_team: Team): Team? {
            val level_mod_data = world.gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return null
            var game_is_won = true
            var winning_team = Team.NONE
            level_mod_data.getActivePlayers().mapNotNull(world.server.playerList::getPlayer).forEach{player ->
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

        private fun winGame(world: ServerLevel, winning_team: Team) {
            val level_mod_data = world.gameState

            val stats_list = level_mod_data.getActivePlayers().mapNotNull(world.server.playerList::getPlayer).map { p ->
                Component.literal(
                    p.name.toString()
                            + " - Kills: "
                            + level_mod_data.getPlayerKills(p)
                            + " - Final Kills: " + level_mod_data.getPlayerFinalKills(p)
                            + " - Deaths: " + level_mod_data.getPlayerDeaths(p)
                            + " - Beds Destroyed: " + level_mod_data.getPlayerBedsDestroyed(p)
                )
            }

            world.server.playerList.players.forEach{player ->
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

                stats_list.forEach{stats_message ->
                    player.sendSystemMessage(stats_message)
                }
            }
            level_mod_data.setGamePhase(GamePhase.ENDED)
        }

        fun afterBedBreak(world: ServerLevel, breaker: ServerPlayer, team: Team) {
            val level_mod_data = world.gameState

            // Remnant bedhunt code to prevent afterBedBreak being called repeatedly after a bed is broken
            // Should not be needed if afterBedBreak is correctly called... after a bed break is registered
            // If bed breaking is detected every tick, something like this will be needed
            // if (!SavedModData.isTeamBaseIntact(team)) return

            level_mod_data.setBedBreaker(team, breaker.uuid)
            level_mod_data.setPlayerBedsDestroyed(breaker, level_mod_data.getPlayerBedsDestroyed(breaker) + 1)
            level_mod_data.setBedAlive(team, false)

            level_mod_data.getActivePlayers().mapNotNull(world.server.playerList::getPlayer).forEach { p ->
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
                        SoundSource.MASTER, p.x, p.y, p.z, 1.0F, 1.0F, world.getRandom().nextLong())
                )

                p.sendSystemMessage(Component.literal(team.name + " bed has been destroyed!"))
            }
        }

        fun tick(world: ServerLevel) {
            val level_mod_data = world.gameState
            if (level_mod_data.getGamePhase() == GamePhase.INACTIVE) return

            val player_manager = world.server.playerList
            world.generatorState.tick()

            level_mod_data.tick()
            level_mod_data.tickTeams(world)

            if (level_mod_data.getTimerTick()) {
                level_mod_data.getActivePlayers().mapNotNull(world.server.playerList::getPlayer).forEach { player ->
                    if (level_mod_data.isPlayerEliminated(player)) return@forEach

                    if (level_mod_data.isPlayerRespawning(player)) {
                        val seconds_left = level_mod_data.getPlayerRespawnSeconds(player)

                        if (seconds_left == 0) {
                            // tp player to base location for respawn
                            val centre = Vec3.atBottomCenterOf(level_mod_data.map_centre)
                            val spawn = level_mod_data.getTeamSpawn(level_mod_data.getPlayersTeam(player.uuid))
                            player.teleportTo(spawn.x, spawn.y, spawn.z)
                            player.lookAt(EntityAnchorArgument.Anchor.EYES, centre)

                            player.setGameMode(GameType.SURVIVAL)
                            level_mod_data.setPlayerAlive(player)
                            player.connection.send(
                                ClientboundClearTitlesPacket(true)
                            )
                            player.connection.send(
                                ClientboundSetTitlesAnimationPacket(10, 40, 10)
                            )
                            player.connection.send(
                                ClientboundSetTitleTextPacket(
                                    Component.literal((ChatFormatting.GREEN.toString() + "RESPAWNED!"))
                                )
                            )
                            player.sendSystemMessage(Component.literal(ChatFormatting.YELLOW.toString() + "You have respawned!"))
                        } else if (level_mod_data.playerTimerSecondPassed(player)) {
                            val respawn_time_message = ChatFormatting.YELLOW.toString() + "You will respawn in " + ChatFormatting.RED.toString() + seconds_left.toString() + ChatFormatting.YELLOW.toString() + " seconds!"
                            player.connection.send(
                                ClientboundSetTitlesAnimationPacket(0, 30, 0)
                            )
                            player.connection.send(
                                ClientboundSetSubtitleTextPacket(
                                    Component.literal(respawn_time_message)
                                )
                            )
                            player.connection.send(
                                ClientboundSetTitleTextPacket(
                                    Component.literal((ChatFormatting.RED.toString() + "YOU DIED!"))
                                )
                            )
                            player.sendSystemMessage(Component.literal(respawn_time_message))
                        }
                    }
                }
            }

            val time = level_mod_data.getGameTime()
            if (level_mod_data.getTimerSecond()) {
                if (level_mod_data.getGamePhase() == GamePhase.STARTING) {
                    if (time.inWholeSeconds.toInt() == 10) {
                        start(world)
                    } else {
                        val time_left = (10.0 - time.inWholeSeconds).toInt()
                        for (player_uuid in level_mod_data.getActivePlayers()) {
                            val player = player_manager.getPlayer(player_uuid) ?: continue    
                            player.connection.send(
                                ClientboundSetTitleTextPacket(
                                    Component.literal(time_left.toString())
                                )
                            )
                            player.connection.send(
                                ClientboundSoundPacket(
                                    Holder.direct(SoundEvents.NOTE_BLOCK_PLING.value()),
                                    SoundSource.MASTER, player.x, player.y, player.z,
                                    1.0F, 1.0F, world.getRandom().nextLong()
                                )
                            )
                        }
                    }
                } else if (level_mod_data.getGamePhase() == GamePhase.ACTIVE) {
                    // periodic things to hit when game active
                    if (time >= DEATHMATCH_TIME && level_mod_data.getGamePeriod() == GamePeriod.ACTIVE) {
                        // trigger deathmatch, you can mess with the deathmatch time constant
                        level_mod_data.setGamePeriod(GamePeriod.DEATHMATCH)

                        // Hi gabs im dumb and forgot how code works
                        // you'll probably want to trigger your deathmatch stuff elsewhere under the condition
                        // gameperiod is deathmatch
                    }
                }
            }
        }
    }
}