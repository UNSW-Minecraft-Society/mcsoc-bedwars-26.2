package mcsoc.bedwars

import org.slf4j.LoggerFactory
import net.minecraft.resources.Identifier
import net.fabricmc.api.ModInitializer
import mcsoc.bedwars.datatrackers.configloader.PluginConfigLoader
import mcsoc.bedwars.datatrackers.configloader.TomlConfigReader
import mcsoc.bedwars.datatrackers.configloader.YamlConfigReader
import mcsoc.bedwars.eventhandlers.registerCommands


object BedwarsPlugin : ModInitializer {
	const val CONFIG_PATH = "bedwars"
	const val MOD_ID: String = "bedwars-plugin"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!")

		// register eventhandlers
        registerCommands()

        TomlConfigReader.initialise()
        YamlConfigReader.initialise()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
