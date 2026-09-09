package mcsoc.bedwars.gamestate

import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.Position
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.GameType
import kotlin.time.Duration.Companion.minutes


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

            TeamEffects.createTeamsWithPlayers(level)
            // TODO val map = level_mod_data.getLoadedMapData()

            // set difficulty to peaceful/easy maybe?
            // maybe disable mob spawning

            val worldborder = level.worldBorder
            worldborder.setCenter(start_block_pos.x.toDouble(), start_block_pos.z.toDouble())
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
                    player.setGameMode(GameType.SURVIVAL)
                }
            }

            // generate map?

            level_mod_data.resetGameTime()
            level_mod_data.setGamePhase(GamePhase.STARTING)
        }

        fun endGame(level: ServerLevel) {
            // Triggered by command or on win condition, clean up stuff
            val level_mod_data = level.gameState
            level_mod_data.clearActivePlayers()
            level.generatorState.clearGenerators()
            // clear teams - todo

            level_mod_data.setGamePhase(GamePhase.INACTIVE)
            level_mod_data.setGamePeriod(GamePeriod.INACTIVE)
            for (player in level.players()) {
                // p.teleportTo(x, y, z) tp to lobby coordinates... figure out later
                player.setGameMode(GameType.SPECTATOR)
            }

            level.worldBorder.size = ServerLevel.ACROSS_THE_WHOLE_WORLD.toDouble()
            level.worldBorder.setCenter(0.0, 0.0)
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

        fun tick(world: ServerLevel) {
            val player_manager = world.server.playerList
            world.generatorState.tick()
            val level_mod_data = world.gameState
            level_mod_data.tick()
            level_mod_data.tickTeams(world)
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