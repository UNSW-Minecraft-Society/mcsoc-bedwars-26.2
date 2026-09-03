package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.entities.CustomEntityType
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import java.util.UUID


private data class LevelTiedData(
    val game_state: ModDataTracker = ModDataTracker(),
    // put other level-tied data trackers here
    val custom_entity_data: MutableMap<UUID, CustomEntityType> = mutableMapOf<UUID, CustomEntityType>()
)

private val level_data_map: MutableMap<ResourceKey<Level>, LevelTiedData> = mutableMapOf()
val ServerLevel.gameState get() = level_data_map.getOrPut(this.dimension()){LevelTiedData()}.game_state
val ServerLevel.customEntityData get() = level_data_map.getOrPut(this.dimension()){LevelTiedData()}.custom_entity_data
// put other level-tied data getters here