package mcsoc.bedwars.datatrackers.configloader

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.inputStream
import net.fabricmc.loader.api.FabricLoader
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.configloader.ConfigData
import mcsoc.bedwars.datatrackers.configloader.ConfigDataExposer
import mcsoc.bedwars.datatrackers.configloader.PluginConfigLoader
import java.io.File


object YamlConfigReader : PluginConfigLoader() {
    const val CONFIG_FILE = "config.yaml"
    
    private val yaml_reader = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = true
        )
    )
    
    override fun getConfigPath(): Path = super.getConfigPath() / CONFIG_FILE
    
    override fun getConfigData(): ConfigDataExposer {
        return config_path.inputStream().use{
            yaml_reader.decodeFromStream(ConfigData.serializer(), it)
        }
    }
    
    override fun saveConfigData() {
        val config_file = File(config_path.absolutePathString())
        config_file.outputStream().use{
            yaml_reader.encodeToStream(ConfigData.serializer(), loaded_config as ConfigData, it)
        }
    }
}