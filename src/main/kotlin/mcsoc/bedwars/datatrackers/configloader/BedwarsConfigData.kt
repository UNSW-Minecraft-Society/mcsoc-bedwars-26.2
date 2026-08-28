package mcsoc.bedwars.datatrackers.configloader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader
import mcsoc.bedwars.utils.MapData

@Serializable
data class LoadedDebugConfig(
    val debug: Boolean = true
)

@Serializable
data class LoadedPluginConfig(
    @SerialName("Debug")
    val debug: LoadedDebugConfig = LoadedDebugConfig()
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
    val map_data: Map<String, MapData>
}

object BedwarsConfigData : BedwarsConfigExposer {
    val plugin_config: LoadedPluginConfig 
        get() = LoadedPluginConfig.Reader.loaded_config
    val map_config: LoadedMapConfig
        get() = LoadedMapConfig.Reader.loaded_config
    
    override val debug: Boolean
        get() = plugin_config.debug.debug
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