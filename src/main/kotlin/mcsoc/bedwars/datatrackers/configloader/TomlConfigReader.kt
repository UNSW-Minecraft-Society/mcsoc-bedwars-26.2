package mcsoc.bedwars.datatrackers.configloader

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.file.TomlFileWriter
import com.akuleshov7.ktoml.source.decodeFromStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream


abstract class TomlConfigReader<T : LoadedConfigExposer<T>>(
    override val config_filename: String,
    private val config_serialiser: KSerializer<T>
) : ConfigLoader<T>(config_serialiser) {    
    override fun getConfigData(): T {
        return config_path.inputStream().use {
            TomlFileReader(
                TomlInputConfig.compliant(ignoreUnknownNames = true)
            ).decodeFromStream(config_serialiser, it)
        }
    }
    
    override fun saveConfigData(config: T) {
        TomlFileWriter().apply{
            this.encodeToFile(config_serialiser, config, config_path.absolutePathString())
        }
    }
}