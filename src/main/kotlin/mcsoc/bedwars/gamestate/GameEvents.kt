package mcsoc.bedwars.gamestate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.customEntityData
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.utils.CODEC
import mcsoc.bedwars.utils.getProgressBar
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.Holder
import net.minecraft.core.UUIDUtil
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


private interface GameEventCompanion<T : GameEvent> {
    val id: String
    val codec: MapCodec<T>
}

fun <T : GameEvent> KClass<T>.getAllSealedSubclasses(): List<KClass<out T>> {
    return this.sealedSubclasses.flatMap {
        if (it.isSealed) {
            it.getAllSealedSubclasses()
        } else {
            listOf(it)
        }
    }
}

sealed class GameEvent(protected val triggerTime: Duration, private val id: String): Comparable<GameEvent> {
    companion object {
        val REGISTRY: Map<String, MapCodec<out GameEvent>> by lazy {
            GameEvent::class.getAllSealedSubclasses()
                .mapNotNull { it.companionObjectInstance as? GameEventCompanion<*> }
                .associate { it.id to it.codec }
        }
        val CODEC: Codec<GameEvent> = Codec.STRING.dispatch(GameEvent::id, REGISTRY::get)
    }
    

    abstract fun trigger(level: ServerLevel)
    
    fun hasExpired(currTime: Duration): Boolean {
        return triggerTime <= currTime
    }

    override fun compareTo(other: GameEvent): Int {
        return triggerTime.compareTo(other.triggerTime)
    }
    
    
    sealed class RecursiveGameEvent<T: GameEvent>(triggerTime: Duration, id: String, protected val count: Long, private val interval: Duration) : GameEvent(triggerTime, id) {
        protected abstract fun recurseTrigger(level: ServerLevel)
        protected abstract fun concludeTrigger(level: ServerLevel)

        protected abstract fun createEvent(triggerTime: Duration, count: Long): RecursiveGameEvent<T>
        
        override fun trigger(level: ServerLevel) {
            if (count > 0) {
                recurseTrigger(level)
                level.eventQueue.queueEvent(createEvent(triggerTime + interval, count - 1))
            } else {
                concludeTrigger(level)
            }
        }
    }

    class EntityExpiryEvent private constructor(triggerTime: Duration, val entityId: UUID, count: Long, val lifetime: Long) :
            RecursiveGameEvent<EntityExpiryEvent>(triggerTime, id, count, 1.seconds) {
        constructor(currTime: Duration, lifetime: Duration, entityId: UUID) : this(
            currTime,
            entityId,
            lifetime.inWholeSeconds,
            lifetime.inWholeSeconds
        )
        
        companion object : GameEventCompanion<EntityExpiryEvent> {
            override val id: String = "entityExpiry"
            override val codec: MapCodec<EntityExpiryEvent> = RecordCodecBuilder.mapCodec{it.group(
                Duration.CODEC.fieldOf("time").forGetter(EntityExpiryEvent::triggerTime),
                UUIDUtil.CODEC.fieldOf("id").forGetter(EntityExpiryEvent::entityId),
                Codec.LONG.fieldOf("depth").forGetter(EntityExpiryEvent::count),
                Codec.LONG.fieldOf("lifetime").forGetter(EntityExpiryEvent::count)
            ).apply(it, ::EntityExpiryEvent)}
        }

        override fun recurseTrigger(level: ServerLevel) {
            val entity = level.getEntity(entityId) ?: return
            val team = level.customEntityData.getEntityTeam(entity)
            BedwarsPlugin.LOGGER.info("$count/$lifetime = ${count.toDouble() / lifetime}")
            entity.customName = Component.literal("${team?.chatColour ?: ""}${getProgressBar(count.toDouble()/lifetime, 10)}")
        }
        override fun concludeTrigger(level: ServerLevel) {
            val entity = level.getEntity(entityId) ?: return
            entity.discard()
            // oooohh. performative coding.
        }
        override fun createEvent(triggerTime: Duration, count: Long) = EntityExpiryEvent(triggerTime, entityId, count, lifetime)
    }
    
    class RespawnCounterEvent private constructor(triggerTime: Duration, val player: UUID, count: Long) :
            RecursiveGameEvent<RespawnCounterEvent>(triggerTime, id, count, INTERVAL) {
        constructor(triggerTime: Duration, respawnTime: Duration, player: UUID) : this(
            triggerTime,
            player,
            (respawnTime / INTERVAL).toLong()
        )
        companion object : GameEventCompanion<RespawnCounterEvent> {
            val INTERVAL = 1.seconds
            
            override val id: String = "respawn"
            override val codec: MapCodec<RespawnCounterEvent> = RecordCodecBuilder.mapCodec{it.group(
                Duration.CODEC.fieldOf("time").forGetter(RespawnCounterEvent::triggerTime),
                UUIDUtil.CODEC.fieldOf("player").forGetter(RespawnCounterEvent::player),
                Codec.LONG.fieldOf("second").forGetter(RespawnCounterEvent::count)
            ).apply(it, ::RespawnCounterEvent)}
        }
        
        override fun recurseTrigger(level: ServerLevel) {
            val player = level.server.playerList.getPlayer(player) ?: return
            val respawn_time_message = RESPAWN_TIME_MESSAGE(count.toInt())
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
        override fun concludeTrigger(level: ServerLevel) {
            val player = level.server.playerList.getPlayer(player) ?: return
            val level_mod_data = level.gameState
            val spawn = level_mod_data.getTeamSpawn(level_mod_data.getPlayersTeam(player.uuid))
            player.teleportTo(
                level, spawn.x, spawn.y, spawn.z,
                setOf(), 0F, 0F, true
            )
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(level_mod_data.map_centre))

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
        }
        override fun createEvent(triggerTime: Duration, count: Long) = RespawnCounterEvent(triggerTime, player, count)
    }
    
    class GameStartCounterEvent private constructor(triggerTime: Duration, count: Long) :
            RecursiveGameEvent<GameStartCounterEvent>(triggerTime, id, count, INTERVAL) {
        constructor(triggerTime: Duration, startTime: Duration, _d: Unit = Unit) : this(
            triggerTime,
            (startTime / INTERVAL).toLong()
        )
        companion object : GameEventCompanion<GameStartCounterEvent> {
            val INTERVAL = 1.seconds
            
            override val id: String = "gameStart"
            override val codec: MapCodec<GameStartCounterEvent> = RecordCodecBuilder.mapCodec{it.group(
                Duration.CODEC.fieldOf("time").forGetter(GameStartCounterEvent::triggerTime),
                Codec.LONG.fieldOf("second").forGetter(GameStartCounterEvent::count)
            ).apply(it, ::GameStartCounterEvent)}
        }
        
        override fun recurseTrigger(level: ServerLevel) {
            level.gameState.getActivePlayers().mapNotNull(level.server.playerList::getPlayer).forEach{player ->
                player.connection.send(
                    ClientboundSetTitleTextPacket(
                        Component.literal(count.toString())
                    )
                )
                player.connection.send(
                    ClientboundSoundPacket(
                        Holder.direct(SoundEvents.NOTE_BLOCK_PLING.value()),
                        SoundSource.MASTER, player.x, player.y, player.z,
                        1.0F, 1.0F, level.getRandom().nextLong()
                    )
                )
            }
        }
        override fun concludeTrigger(level: ServerLevel) {
            GameManager.start(level)
        }
        override fun createEvent(triggerTime: Duration, count: Long) = GameStartCounterEvent(triggerTime, count)
    }
}