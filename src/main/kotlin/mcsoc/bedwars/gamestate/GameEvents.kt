package mcsoc.bedwars.gamestate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance


private interface GameEventCompanion<T : GameEvent> {
    val id: String
    val codec: MapCodec<T>
}

sealed class GameEvent(protected val tickNumber: Long, private val id: String): Comparable<GameEvent> {
    companion object {
        val REGISTRY: Map<String, MapCodec<out GameEvent>> by lazy {
            GameEvent::class.sealedSubclasses
                .mapNotNull { it.companionObjectInstance as? GameEventCompanion<*> }
                .associate { it.id to it.codec }
        }
        val CODEC: Codec<GameEvent> = Codec.STRING.dispatch(GameEvent::id, REGISTRY::get)
    }

    abstract fun trigger(level: ServerLevel)
    
    fun hasExpired(currTickNumber: Long): Boolean {
        BedwarsPlugin.LOGGER.info("event time left: {}", tickNumber - currTickNumber)
        return tickNumber <= currTickNumber
    }

    override fun compareTo(other: GameEvent): Int {
        return tickNumber.compareTo(other.tickNumber)
    }

    class EntityExpiryEvent(tickNumber: Long, val entityId: UUID): GameEvent(tickNumber, id) {
        companion object : GameEventCompanion<EntityExpiryEvent> {
            override val id: String = "golem"
            override val codec: MapCodec<EntityExpiryEvent> = RecordCodecBuilder.mapCodec{it.group(
                Codec.LONG.fieldOf("time").forGetter(EntityExpiryEvent::tickNumber),
                UUIDUtil.CODEC.fieldOf("id").forGetter(EntityExpiryEvent::entityId)
            ).apply(it, ::EntityExpiryEvent)}
        }
        override fun trigger(level: ServerLevel) {
            // lazy kill impl, could add effects
            val golem = level.getEntity(entityId) ?: return
            golem.kill(level)
            // TODO("Not yet implemented")
        }
    }
}

