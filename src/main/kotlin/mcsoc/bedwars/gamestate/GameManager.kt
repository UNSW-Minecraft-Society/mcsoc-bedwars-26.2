package mcsoc.bedwars.gamestate

import com.jcraft.jorbis.Block
import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.utils.inWholeTicks
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.Position
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.collections.set
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

const val NUM_TEAMS = 2 // If we want to change number of teams later store this in data store and make a command able to change it while game inactive
val DEATHMATCH_TIME = 10.minutes // change if i'm wrong
const val BORDER_SIZE: Double = 300.0 // change if needed

class GameManager {
    companion object {
        fun setupGame(start_pos: Position, world: ServerLevel) {
            if (ModDataTracker.getGamePhase() != GamePhase.INACTIVE) {
                endGame(world)
            }

            val start_block_pos = BlockPos.containing(start_pos)
            val server = world.server
            ModDataTracker.initialiseTeams(NUM_TEAMS)

            // set difficulty to peaceful/easy maybe?
            // maybe disable mob spawning

            world.players().forEach { player ->
                ModDataTracker.addActivePlayer(player.uuid)
            }

            val worldborder = world.worldBorder
            worldborder.setCenter(start_block_pos.x.toDouble(), start_block_pos.z.toDouble())
            worldborder.size = BORDER_SIZE

            // distribute players to teams + reset player stuff

            // generate map?

            ModDataTracker.resetGameTime()
            ModDataTracker.setGamePhase(GamePhase.STARTING)
        }

        fun endGame(world: ServerLevel) {
            // Triggered by command or on win condition, clean up stuff
            ModDataTracker.clearActivePlayers()
            // clear teams - todo

            ModDataTracker.setGamePhase(GamePhase.INACTIVE)
            ModDataTracker.setGamePeriod(GamePeriod.INACTIVE)
            world.players().forEach { p ->
//                p.teleportTo(x, y, z) tp to lobby coordinates... figure out later
                p.setGameMode(GameType.ADVENTURE)
            }

            world.worldBorder.size = 59999968.0;
            world.worldBorder.setCenter(0.0, 0.0);
        }

        private fun start(world: ServerLevel) {
            world.players().forEach { p ->
                p.connection.send(
                    ClientboundSetTitleTextPacket(
                        Component.literal("GO")
                    )
                )
                p.connection.send(
                    ClientboundSoundPacket(
                        Holder.direct(SoundEvents.BLAZE_SHOOT),
                        SoundSource.MASTER, p.x, p.y, p.z, 1.0F, 1.0F, world.getRandom().nextLong())
                )
            }
            // tp players to spawn points
            // start generators
            // maybe show a title saying game begin or something
            // maybe a little tooltip in the bottom left
            ModDataTracker.resetGameTime()
            ModDataTracker.setGamePhase(GamePhase.ACTIVE)
            ModDataTracker.setGamePeriod(GamePeriod.ACTIVE)
        }

        fun tick(world: ServerLevel) {
            val time = ModDataTracker.getGameTime()
            if (ModDataTracker.getTimerSecond()) {
                if (ModDataTracker.getGamePhase() == GamePhase.STARTING) {
                    if (time.inWholeSeconds.toInt() == 10) {
                        start(world)
                    } else {
                        val time_left = (10.0 - time.inWholeSeconds).toInt()
                        world.players().forEach { p ->
                            p.connection.send(
                                ClientboundSetTitleTextPacket(
                                    Component.literal(time_left.toString())
                                )
                            )
                            p.connection.send(
                                ClientboundSoundPacket(
                                    Holder.direct(SoundEvents.NOTE_BLOCK_PLING.value()),
                                    SoundSource.MASTER, p.x, p.y, p.z, 1.0F, 1.0F, world.getRandom().nextLong())
                            )
                        }
                    }
                } else if (ModDataTracker.getGamePhase() == GamePhase.ACTIVE) {
                    // periodic things to hit when game active
                    if (time >= DEATHMATCH_TIME && ModDataTracker.getGamePeriod() == GamePeriod.ACTIVE) {
                        // trigger deathmatch, you can mess with the deathmatch time constant
                        ModDataTracker.setGamePeriod(GamePeriod.DEATHMATCH)

                        // Hi gabs im dumb and forgot how code works
                        // you'll probably want to trigger your deathmatch stuff elsewhere under the condition
                        // gameperiod is deathmatch
                    }
                }
            }
        }
    }
}