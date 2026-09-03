package mcsoc.bedwars.gamestate

import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.gameState
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.Position
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.level.GameType
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.toKotlinUuid

val DEATHMATCH_TIME = 10.minutes // change if i'm wrong
const val BORDER_SIZE: Double = 300.0 // change if needed

class GameManager {
    companion object {
        fun setupGame(level: ServerLevel, start_pos: Position) {
            val level_mod_data = level.gameState
            if (level_mod_data.getGamePhase() != GamePhase.INACTIVE) {
                endGame(level)
            }

            val start_block_pos = BlockPos.containing(start_pos)
            // TODO val map = level_mod_data.getLoadedMapData()
            level_mod_data.initialiseTeams(2)

            // set difficulty to peaceful/easy maybe?
            // maybe disable mob spawning

            val worldborder = level.worldBorder
            worldborder.setCenter(start_block_pos.x.toDouble(), start_block_pos.z.toDouble())
            worldborder.size = BORDER_SIZE

            // distribute players to teams + reset player stuff

            // generate map?

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

            val player_team = level_mod_data.getPlayersTeam(player.uuid.toKotlinUuid())
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

            // store player's death position to summon lightning later. Due to the nature of this event handler,
            // all players are forced to enter "DEAD" state upon death.
            level_mod_data.setPlayerDead(player, player.position())
        }

        fun handlePlayerRespawn(player: ServerPlayer) {
            val level_mod_data = player.level().gameState
            if (level_mod_data.getGamePhase() != GamePhase.ACTIVE) return

            level_mod_data.downgradeItems(player)

            player.setGameMode(GameType.SPECTATOR)

            if (!level_mod_data.getBedDestroyed(level_mod_data.getPlayersTeam(player.uuid.toKotlinUuid()))) {
                // tp above map
//                player.teleportTo(base_position.x.toDouble(), base_position.y.toDouble(), base_position.z.toDouble())

                level_mod_data.setPlayerRespawning(player)
                // notify player how much time left in respawn maybe? either that or just display a fat ass word RESPAWNING
                // i need to check hypixel bedwars again...
                // todo
//                player.connection.send(
//                    ClientboundSetTitleTextPacket(
//                        Component.literal((level_mod_data.getRespawnTime().toString()))
//                    )
//                )
            } else {
                eliminatePlayer(player)
            }
        }

        fun eliminatePlayer(player: ServerPlayer) {
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

            // check if a team has won - urgent todo
//            val winning_team = checkPlayersLeftOnTeam(world,world.players(), SavedModData.getPlayerTeam(player.uuid)) ?: return
//            winGame(world, winning_team)
        }

        fun tick(world: ServerLevel) {
            val player_manager = world.server.playerList
            val level_mod_data = world.gameState
            level_mod_data.tick()
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