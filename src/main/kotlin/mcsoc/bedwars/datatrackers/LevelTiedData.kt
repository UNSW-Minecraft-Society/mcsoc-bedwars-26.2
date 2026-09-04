package mcsoc.bedwars.datatrackers

import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level


private data class LevelTiedData(
    val game_state: ModDataTracker = ModDataTracker()
    // put other level-tied data trackers here
)

private val level_data_map: MutableMap<ResourceKey<Level>, LevelTiedData> = mutableMapOf()
val ServerLevel.gameState get() = level_data_map.getOrPut(this.dimension()){LevelTiedData()}.game_state
// put other level-tied data getters here