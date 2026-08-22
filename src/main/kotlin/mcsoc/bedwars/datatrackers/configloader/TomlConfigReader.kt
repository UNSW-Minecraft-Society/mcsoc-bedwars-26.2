package mcsoc.bedwars.datatrackers.configloader

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.file.TomlFileWriter
import com.akuleshov7.ktoml.source.decodeFromStream
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


object TomlConfigReader : PluginConfigLoader() {
    const val CONFIG_FILE = "config.toml"
    override fun getConfigPath(): Path = super.getConfigPath() / CONFIG_FILE
    
    override fun getConfigData(): ConfigDataExposer {
        return config_path.inputStream().use {
            TomlFileReader(
                TomlInputConfig.compliant(ignoreUnknownNames = true)
            ).decodeFromStream(ConfigData.serializer(), it)
        }
    }
    
    override fun saveConfigData() {
        TomlFileWriter().apply{
            this.encodeToFile(ConfigData.serializer(), loaded_config as ConfigData, config_path.absolutePathString())
        }
    }
}