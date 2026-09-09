package mcsoc.bedwars.gamestate

import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.utils.Team
import net.minecraft.ChatFormatting
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
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.gamerules.GameRules
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.toKotlinUuid

val DEATHMATCH_TIME = 10.minutes // change if i'm wrong
const val BORDER_SIZE: Double = 300.0 // change if needed
const val RESPAWN_TIME: Int = 5

class GameManager {
    companion object {
        fun setupGame(level: ServerLevel, start_pos: Position) {
            val level_mod_data = level.gameState
            if (level_mod_data.getGamePhase() != GamePhase.INACTIVE) {
                endGame(level)
            }

            val start_block_pos = BlockPos.containing(start_pos)

            // later add a command that can modify number of teams (seperate to assign_teams)
            TeamEffects.createTeamsWithPlayers(level, 2)
            // TODO val map = level_mod_data.getLoadedMapData()

            // set difficulty to peaceful/easy maybe?
            // maybe disable mob spawning

            val worldborder = level.worldBorder
            worldborder.setCenter(start_block_pos.x.toDouble(), start_block_pos.z.toDouble())
            worldborder.size = BORDER_SIZE

            // distribute players to teams + reset player stuff

            // generate map?

            val gamerules = level.server.gameRules
            gamerules.set(GameRules.IMMEDIATE_RESPAWN, true, level.server)
            gamerules.set(GameRules.KEEP_INVENTORY, true, level.server)

            level_mod_data.resetGameTime()
            level_mod_data.setGamePhase(GamePhase.STARTING)
        }

        fun endGame(world: ServerLevel) {
            // Triggered by command or on win condition, clean up stuff
            val level_mod_data = world.gameState
            level_mod_data.clearActivePlayers()
            // clear teams - todo

            level_mod_data.setGamePhase(GamePhase.INACTIVE)
            level_mod_data.setGamePeriod(GamePeriod.INACTIVE)
            for (player in world.players()) {
                // p.teleportTo(x, y, z) tp to lobby coordinates... figure out later
                player.setGameMode(GameType.SPECTATOR)
            }

            world.worldBorder.size = 59999968.0
            world.worldBorder.setCenter(0.0, 0.0)
        }

        private fun start(world: ServerLevel) {
            val player_manager = world.server.playerList
            val level_mod_data = world.gameState
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
                        1.0F, 1.0F, world.getRandom().nextLong()
                    )
                )
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

            // bedhunt code for kill tracking, to be updated
//            if (death_source.entity is ServerPlayer) {
//                val killer = death_source.entity as ServerPlayer
//                level_mod_data.setPlayerKills(killer.uuid, level_mod_data.getPlayerKills(killer.uuid) + 1)
//
//                if (!should_respawn) {
//                    level_mod_data.setPlayerFinalKills(killer.uuid, level_mod_data.getPlayerFinalKills(killer.uuid) + 1)
//                }
//            }

            // used in bedhunt to drop player inventory on death - can probably be removed here, although, maybe this should ensure if player died to void
            // maybe money (gold, iron diamonds emeralds) transfer to killer? I'm leaving this code here for reference in case we need to index
            // over a player's inventory to do something like this. Note this could probably be moved into the eliminate player function as
            // it was only originally here to make the player drop items at death location
//            if (!should_respawn) {
//                player.inventory.forEachIndexed { i, stack ->
//                    if (!stack.isEmpty) {
//                        val vanishingCurse = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE)
//
//                        // Check if the item stack contains the Curse of Vanishing
//                        if (EnchantmentHelper.getItemEnchantmentLevel(vanishingCurse, stack) > 0) {
//                            player.inventory.setItem(i, ItemStack.EMPTY);
//                        } else {
//                            player.drop(stack, true, false);
//                            player.inventory.setItem(i, ItemStack.EMPTY);
//                        }
//                    }
//                }
//            }


            player.inventory.clearContent()
            level_mod_data.downgradeItems(player)

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
//                player.teleportTo(base_position.x.toDouble(), base_position.y.toDouble(), base_position.z.toDouble())

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
            // notify eliminate player of their kill stats - TODO
//            player.sendSystemMessage(Component.literal("Kills: " + level_mod_data.getPlayerKills(player.uuid) + " Final Kills: " + level_mod_data.getPlayerFinalKills(player.uuid)))

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

            // for stats branch
//            val top_killers = level_mod_data.getActivePlayers().map { player -> Pair(world.getPlayerByUUID(player)?.scoreboardName, level_mod_data.getPlayerKills(player)) }.sortedByDescending { p -> p.second }.take(3)
//            val top_final_killers = level_mod_data.getActivePlayers().map { player -> Pair(world.getPlayerByUUID(player)?.scoreboardName, level_mod_data.getPlayerFinalKills(player)) }.sortedByDescending { p -> p.second }.take(3)

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

//                player.sendSystemMessage(Component.literal("Top Killers:"))
//                for (i in 0..2) player.sendSystemMessage(Component.literal(top_killers[i].first + ": " + top_killers[i].second))
//                player.sendSystemMessage(Component.literal("Top Final Killers"))
//                for (i in 0..2) player.sendSystemMessage(Component.literal(top_final_killers[i].first + ": " + top_final_killers[i].second))
            }
            level_mod_data.setGamePhase(GamePhase.ENDED)
        }

        fun afterBedBreak(world: ServerLevel, breaker: ServerPlayer, team: Team) {
            val level_mod_data = world.gameState

            // Remnant bedhunt code to prevent afterBedBreak being called repeatedly after a bed is broken
            // Should not be needed if afterBedBreak is correctly called... after a bed break is registered
            // If bed breaking is detected every tick, something like this will be needed
            // if (!SavedModData.isTeamBaseIntact(team)) return

            // note for myself later in kill stats, add a way to track bed breaks + attribute void final kills to bed breaker

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
//                            val base_position = SavedModData.getTeamBasePosition(SavedModData.getPlayerTeam(uuid))
//                            player.teleportTo(base_position.x.toDouble(), base_position.y.toDouble(), base_position.z.toDouble())

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