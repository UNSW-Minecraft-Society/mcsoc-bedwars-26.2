package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.utils.UUID_CODEC
import net.minecraft.world.entity.Entity
import java.util.UUID

enum class CustomEntityType(val title: String) {
    PLAYER_SHOPKEEPER("Player Shopkeeper"),
    TEAM_SHOPKEEPER("Team Shopkeeper");

    companion object {
        val CODEC: Codec<CustomEntityType> = Codec.STRING.xmap(::valueOf, CustomEntityType::name)
    }
}

internal interface CustomEntityHolder {
    val custom_entity_types: MutableMap<UUID, CustomEntityType>
}

internal interface CustomEntityExposer {
    fun getEntityType(id: UUID): CustomEntityType?
    fun getEntityType(entity: Entity): CustomEntityType?
    fun addEntity(id: UUID, type: CustomEntityType)
    fun addEntity(entity: Entity, type: CustomEntityType)
}

class CustomEntityDataTracker : LevelTiedData, CustomEntityExposer, CustomEntityHolder {
    override val custom_entity_types: MutableMap<UUID, CustomEntityType>
    override val type get() = LevelDataType.CustomEntityData

    companion object {
        val CODEC: Codec<CustomEntityDataTracker> = RecordCodecBuilder.create {it.group(
            Codec.unboundedMap<UUID, CustomEntityType>(
                UUID_CODEC,
                CustomEntityType.CODEC
            ).fieldOf("custom_entity_types").forGetter(CustomEntityDataTracker::custom_entity_types)
        ).apply(it, ::CustomEntityDataTracker)}
    }

    private constructor(custom_entity_types: MutableMap<UUID, CustomEntityType>) {
        this.custom_entity_types = custom_entity_types
    }

    internal constructor() : this(mutableMapOf<UUID, CustomEntityType>())

    override fun getEntityType(id: UUID): CustomEntityType? {
        return custom_entity_types[id]
    }

    override fun getEntityType(entity: Entity): CustomEntityType? {
        return getEntityType(entity.uuid)
    }

    override fun addEntity(id: UUID, type: CustomEntityType) {
        custom_entity_types[id] = type
    }

    override fun addEntity(entity: Entity, type: CustomEntityType) {
        addEntity(entity.uuid, type)
    }
}