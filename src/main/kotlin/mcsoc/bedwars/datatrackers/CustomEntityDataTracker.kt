package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.entities.CustomEntityType
import net.minecraft.world.entity.Entity
import java.util.UUID

internal interface CustomEntityHolder {
    val customEntityTypes: MutableMap<UUID, CustomEntityType>
}

internal interface CustomEntityExposer {
    fun getEntityType(id: UUID): CustomEntityType?
    fun getEntityType(entity: Entity): CustomEntityType?
    fun addEntity(id: UUID, type: CustomEntityType)
    fun addEntity(entity: Entity, type: CustomEntityType)
}

class CustomEntityDataTracker : LevelTiedData, CustomEntityExposer, CustomEntityHolder {
    override val customEntityTypes: MutableMap<UUID, CustomEntityType> = mutableMapOf<UUID, CustomEntityType>()
    override val type get() = LevelDataType.CustomEntityData

    internal constructor()

    override fun getEntityType(id: UUID): CustomEntityType? {
        return customEntityTypes[id]
    }

    override fun getEntityType(entity: Entity): CustomEntityType? {
        return getEntityType(entity.uuid)
    }

    override fun addEntity(id: UUID, type: CustomEntityType) {
        customEntityTypes[id] = type
    }

    override fun addEntity(entity: Entity, type: CustomEntityType) {
        addEntity(entity.uuid, type)
    }
}