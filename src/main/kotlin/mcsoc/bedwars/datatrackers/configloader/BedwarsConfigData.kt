package mcsoc.bedwars.datatrackers.configloader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.GamePeriod
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader
import mcsoc.bedwars.datatrackers.gameState
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


@Serializable
data class LoadedDebugConfig(
    val debug: Boolean = false
)

@Serializable
data class LoadedGameConfig(
    val diamondIITime: Duration = 3.minutes,
    val emeraldIITime: Duration = 4.minutes,
    val diamondIIITime: Duration = 6.minutes,
    val emeraldIIITime: Duration = 7.minutes,
    val deathmatchTime: Duration = 10.minutes,
    val gameEndTime: Duration = 15.minutes,
    val sillyMode: Boolean = false
)

@Serializable
data class LoadedPluginConfig(
    @SerialName("Debug")
    val debug: LoadedDebugConfig = LoadedDebugConfig(),
    @SerialName("Game")
    val game: LoadedGameConfig = LoadedGameConfig()
) : LoadedConfigExposer<LoadedPluginConfig> {
    object Reader : TomlConfigReader<LoadedPluginConfig>("config.toml", LoadedPluginConfig.serializer()) {
        override fun defaultConfigData(): LoadedPluginConfig {
            return LoadedPluginConfig()
        }
    }
}

@Serializable
data class LoadedMapConfig(
    val maps: Map<String, MapData> = mapOf(Pair("example", MapData()))
) : LoadedConfigExposer<LoadedMapConfig> {
    object Reader : YamlConfigReader<LoadedMapConfig>("maps.yml", LoadedMapConfig.serializer()) {
        override fun defaultConfigData(): LoadedMapConfig {
            return LoadedMapConfig()
        }
    }
}


interface BedwarsConfigExposer {
    val debug: Boolean
    val generator_times: Map<GamePeriod, Duration>
    val map_data: Map<String, MapData>
    
    fun placeMap(map_name: String, level: ServerLevel, pos: BlockPos): Boolean {
        return map_data[map_name]?.let {
            it.place(level, pos)
            true
        } ?: run{
            BedwarsPlugin.LOGGER.error("Map Loading Error: No map exists with id $map_name")
            false
        }
    }
}

object BedwarsConfigData : BedwarsConfigExposer {
    val plugin_config: LoadedPluginConfig 
        get() = LoadedPluginConfig.Reader.loaded_config
    val map_config: LoadedMapConfig
        get() = LoadedMapConfig.Reader.loaded_config
    
    override val debug: Boolean
        get() = plugin_config.debug.debug
    override val generator_times: Map<GamePeriod, Duration>
        get() = mapOf(
            GamePeriod.DIAMOND_II to plugin_config.game.diamondIITime,
            GamePeriod.EMERALD_II to plugin_config.game.emeraldIITime,
            GamePeriod.DIAMOND_III to plugin_config.game.diamondIIITime,
            GamePeriod.EMERALD_III to plugin_config.game.emeraldIIITime,
            GamePeriod.DEATHMATCH to plugin_config.game.deathmatchTime,
            GamePeriod.TERMINAL to plugin_config.game.gameEndTime
        )
    
    override val map_data: Map<String, MapData>
        get() = map_config.maps
        
    fun initialise() {
        LoadedPluginConfig.Reader.initialise()
        LoadedMapConfig.Reader.initialise()
        StructureLoader.initialise()
    }
    
    fun reloadConfig() {
        LoadedPluginConfig.Reader.loadConfigFromFile()
        LoadedMapConfig.Reader.loadConfigFromFile()
    }
}