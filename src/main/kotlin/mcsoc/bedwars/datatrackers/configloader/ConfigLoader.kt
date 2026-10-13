package mcsoc.bedwars.datatrackers.configloader

import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import mcsoc.bedwars.BedwarsPlugin
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.notExists


val config_dir 
    get() = FabricLoader.getInstance().configDir / BedwarsPlugin.CONFIG_PATH

interface LoadedConfigExposer<T: LoadedConfigExposer<T>>

abstract class ConfigLoader<T : LoadedConfigExposer<T>>(serialiser: KSerializer<T>) {
    internal abstract val config_filename: String
    internal val config_path: Path
        get() = config_dir / config_filename
    lateinit var loaded_config: T
    
    private fun loadConfigData() {
        loaded_config = try {
            getConfigData()
        } catch (e: Exception) {
            when (e) {
                is SerializationException -> BedwarsPlugin.LOGGER.error("Error while loading $config_path: ", e)
                is IllegalArgumentException -> BedwarsPlugin.LOGGER.error("Invalid config $config_path: ", e)
                is IOException -> BedwarsPlugin.LOGGER.error("Cannot read file $config_path: ", e)
                else -> throw e
            }
            defaultConfigData()
        }
        
        saveConfigToFile(loaded_config)
    }
    
    fun saveConfigToFile(config: T) {
        this.saveConfigData(config)
    }
    
    fun initialise() {
        config_path.createParentDirectories().takeIf{it.notExists()}?.createFile()
        loadConfigFromFile()
    }
    
    fun loadConfigFromFile() {
        this.loadConfigData()
    }

    internal abstract fun defaultConfigData(): T
    internal abstract fun getConfigData(): T
    internal abstract fun saveConfigData(config: T)
}