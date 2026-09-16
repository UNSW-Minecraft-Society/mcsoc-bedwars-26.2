package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.world.entity.Entity
import java.util.UUID

enum class CustomEntityType(val title: String) {
    PLAYER_SHOPKEEPER("Player Shopkeeper"),
    TEAM_SHOPKEEPER("Team Shopkeeper"),
    DREAM_DEFENDER("Dream Defender"),
    DEATHMATCH_DRAGON("Deathmatch Dragon");

    companion object {
        val CODEC: Codec<CustomEntityType> = Codec.STRING.xmap(::valueOf, CustomEntityType::name)
    }
}

internal interface CustomEntityHolder {
    val custom_entity_types: MutableMap<UUID, CustomEntityType>
    val custom_entity_team_data: MutableMap<UUID, Team>
}

internal interface CustomEntityExposer {
    fun getEntityType(id: UUID): CustomEntityType?
    fun getEntityType(entity: Entity): CustomEntityType? = getEntityType(entity.uuid)
    fun getEntityTeam(id: UUID): Team?
    fun getEntityTeam(entity: Entity): Team? = getEntityTeam(entity.uuid)
    fun addEntity(id: UUID, type: CustomEntityType)
    fun addEntity(entity: Entity, type: CustomEntityType) = addEntity(entity.uuid, type)
    fun addTeamEntity(id: UUID, type: CustomEntityType, team: Team)
    fun addTeamEntity(entity: Entity, type: CustomEntityType, team: Team) = addTeamEntity(entity.uuid, type, team)
    fun getEntityIds(): List<UUID>
    fun getEntityIds(type: CustomEntityType): List<UUID>
    fun removeEntity(id: UUID)
}

class CustomEntityDataTracker : LevelTiedData, CustomEntityExposer, CustomEntityHolder {
    override val custom_entity_types: MutableMap<UUID, CustomEntityType> = mutableMapOf()
    override val custom_entity_team_data: MutableMap<UUID, Team> = mutableMapOf()
    override val type get() = LevelDataType.CustomEntityData

    companion object {
        val CODEC: MapCodec<CustomEntityDataTracker> = RecordCodecBuilder.mapCodec {it.group(
            Codec.unboundedMap<UUID, CustomEntityType>(
                UUIDUtil.STRING_CODEC,
                CustomEntityType.CODEC
            ).fieldOf("custom_entity_types").forGetter(CustomEntityDataTracker::custom_entity_types)
        ).apply(it, ::CustomEntityDataTracker)}
    }

    private constructor(custom_entity_types: MutableMap<UUID, CustomEntityType>) {
        this.custom_entity_types += custom_entity_types
    }

    internal constructor() : this(mutableMapOf<UUID, CustomEntityType>())

    override fun getEntityType(id: UUID): CustomEntityType? {
        return custom_entity_types[id]
    }

    override fun getEntityTeam(id: UUID): Team? {
        return custom_entity_team_data[id]
    }

    override fun addEntity(id: UUID, type: CustomEntityType) {
        custom_entity_types[id] = type
    }

    override fun addTeamEntity(
        id: UUID,
        type: CustomEntityType,
        team: Team
    ) {
        addEntity(id, type)
        custom_entity_team_data[id] = team
    }

    override fun getEntityIds(): List<UUID> {
        return custom_entity_types.keys.toList()
    }

    override fun getEntityIds(type: CustomEntityType): List<UUID> {
        return custom_entity_types.filter { entry -> entry.value == type }.keys.toList()
    }

    override fun removeEntity(id: UUID) {
        custom_entity_types.remove(id)
    }
}