package mcsoc.bedwars.gamestate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.utils.INSTANT_CODEC
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance
import kotlin.time.Instant


private interface GameEventCompanion<T : GameEvent> {
    val id: String
    val codec: MapCodec<T>
}

internal sealed class GameEvent(protected val triggerTime: Instant, private val id: String): Comparable<GameEvent> {
    companion object {
        val REGISTRY: Map<String, MapCodec<out GameEvent>> by lazy {
            GameEvent::class.sealedSubclasses
                .mapNotNull { it.companionObjectInstance as? GameEventCompanion<*> }
                .associate { it.id to it.codec }
        }
        val CODEC: Codec<GameEvent> = Codec.STRING.dispatch(GameEvent::id, REGISTRY::get)
    }

    abstract fun trigger(level: ServerLevel)
    
    fun hasExpired(currTime: Instant): Boolean {
        return triggerTime <= currTime
    }

    override fun compareTo(other: GameEvent): Int {
        return triggerTime.compareTo(other.triggerTime)
    }

    class EntityExpiryEvent(triggerTime: Instant, val entityId: UUID): GameEvent(triggerTime, id) {
        companion object : GameEventCompanion<EntityExpiryEvent> {
            override val id: String = "golem"
            override val codec: MapCodec<EntityExpiryEvent> = RecordCodecBuilder.mapCodec{it.group(
                INSTANT_CODEC.fieldOf("time").forGetter(EntityExpiryEvent::triggerTime),
                UUIDUtil.CODEC.fieldOf("id").forGetter(EntityExpiryEvent::entityId)
            ).apply(it, ::EntityExpiryEvent)}
        }
        override fun trigger(level: ServerLevel) {
            // lazy kill impl, could add effects
            val golem = level.getEntity(entityId) ?: return
            golem.discard()
            // TODO("Not yet implemented")
        }
    }
}

