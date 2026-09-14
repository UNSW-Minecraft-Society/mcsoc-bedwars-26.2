package mcsoc.bedwars.gamestate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.utils.CODEC
import net.minecraft.ChatFormatting
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.UUIDUtil
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


private interface GameEventCompanion<T : GameEvent> {
    val id: String
    val codec: MapCodec<T>
}

sealed class GameEvent(protected val triggerTime: Duration, private val id: String): Comparable<GameEvent> {
    companion object {
        val REGISTRY: Map<String, MapCodec<out GameEvent>> by lazy {
            GameEvent::class.sealedSubclasses
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
    
    
    abstract class RecursiveGameEvent<T: GameEvent>(triggerTime: Duration, id: String, protected val count: Long, private val interval: Duration) : GameEvent(triggerTime, id) {
        
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

    class EntityExpiryEvent private constructor(triggerTime: Duration, val entityId: UUID, count: Long) :
            RecursiveGameEvent<EntityExpiryEvent>(triggerTime, id, count, 1.seconds) {
        constructor(currTime: Duration, lifetime: Duration, entityId: UUID) : this(
            currTime,
            entityId,
            lifetime.inWholeSeconds
        )
        
        companion object : GameEventCompanion<EntityExpiryEvent> {
            override val id: String = "entityExpiry"
            override val codec: MapCodec<EntityExpiryEvent> = RecordCodecBuilder.mapCodec{it.group(
                Duration.CODEC.fieldOf("time").forGetter(EntityExpiryEvent::triggerTime),
                UUIDUtil.CODEC.fieldOf("id").forGetter(EntityExpiryEvent::entityId),
                Codec.LONG.fieldOf("depth").forGetter(EntityExpiryEvent::count)
            ).apply(it, ::EntityExpiryEvent)}
        }

        override fun recurseTrigger(level: ServerLevel) {
            val entity = level.getEntity(entityId) ?: return
            entity.customName = Component.literal("${ChatFormatting.RED}$count SECONDS")
        }
        override fun concludeTrigger(level: ServerLevel) {
            val entity = level.getEntity(entityId) ?: return
            entity.discard()
        }
        override fun createEvent(triggerTime: Duration, count: Long) = EntityExpiryEvent(triggerTime, entityId, count)
    }
    }
}

