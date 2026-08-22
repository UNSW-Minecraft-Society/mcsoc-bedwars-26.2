package mcsoc.bedwars.datatrackers.configloader

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.file.TomlFileWriter
import com.akuleshov7.ktoml.source.decodeFromStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import java.nio.file.Path

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.utils.CylindricalBlockPos
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.notExists

import org.slf4j.Logger
import java.io.IOException
import kotlin.io.path.div
import kotlin.math.PI


private const val CONFIG_FILE_PATH = "config.toml"
val base_structure_name = "base"
val mid_structure_name = "mid"
val diamond_structure_name = "diamond"
val misc_structure_name = "misc"


@Serializable
internal data class ConfigData(
    @SerialName("plugin")
    val config_data: PluginConfigData = PluginConfigData(), 
)


@Serializable
internal data class PluginConfigData(
    val debug: Boolean = false
)


object PluginConfigLoader {
    private val config_path: Path = FabricLoader.getInstance().configDir/BedwarsPlugin.CONFIG_PATH/CONFIG_FILE_PATH
    
    fun initialise() {
        config_path.createParentDirectories()
        config_path.takeIf{it.notExists()}?.createFile()
        loadDataFromFiles()
    }
    
    private lateinit var config: ConfigData
    private val config_data get() = config.config_data
    
    private fun loadConfigData() {
        config = try {
            config_path.inputStream().use {
                TomlFileReader(
                    TomlInputConfig.compliant(ignoreUnknownNames = true)
                ).decodeFromStream(ConfigData.serializer(), it)
            }
        } catch (e: Exception) {
            when (e) {
                is SerializationException -> BedwarsPlugin.LOGGER.error("Error while deserialising ConfigData: ", e)
                is IllegalArgumentException -> BedwarsPlugin.LOGGER.error("Invalid ConfigData: ", e)
                is IOException -> BedwarsPlugin.LOGGER.error("Cannot read config file: ", e)
                else -> throw e
            }
            ConfigData()
        }
        
        saveConfigData()
    }
    
    private fun saveConfigData() {
        TomlFileWriter().apply{
            this.encodeToFile(ConfigData.serializer(), config, config_path.absolutePathString())
        }
    }

    fun loadDataFromFiles() {
        this.loadConfigData()
        
        this.saveDataToFiles()
    }
    
    fun saveDataToFiles() {
        this.saveConfigData()
    }
}