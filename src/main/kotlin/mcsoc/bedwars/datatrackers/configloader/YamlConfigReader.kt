package mcsoc.bedwars.datatrackers.configloader

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import kotlinx.serialization.KSerializer
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import java.io.File


abstract class YamlConfigReader<T : LoadedConfigExposer<T>>(
    override val config_filename: String,
    private val config_serialiser: KSerializer<T>
) : ConfigLoader<T>(config_serialiser) {     
    private val yaml_reader = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = true
        )
    )
    
    override fun getConfigData(): T {
        return config_path.inputStream().use{
            yaml_reader.decodeFromStream(config_serialiser, it)
        }
    }
    
    override fun saveConfigData(config: T) {
        val config_file = File(config_path.absolutePathString())
        config_file.outputStream().use{
            yaml_reader.encodeToStream(config_serialiser, loaded_config, it)
        }
    }
}