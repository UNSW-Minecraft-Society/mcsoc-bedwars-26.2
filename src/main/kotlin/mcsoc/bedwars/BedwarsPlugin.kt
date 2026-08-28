package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.eventhandlers.commands.registerCommands
import mcsoc.bedwars.eventhandlers.registerBlockBreakEvents
import mcsoc.bedwars.eventhandlers.commands.registerCommands
import mcsoc.bedwars.eventhandlers.registerItemCallbacks
import mcsoc.bedwars.eventhandlers.commands.registerCommands
import mcsoc.bedwars.eventhandlers.registerItemCallbacks
import mcsoc.bedwars.eventhandlers.registerPlayerJoinEvent
import mcsoc.bedwars.eventhandlers.AfterRespawnEvent
import mcsoc.bedwars.gui.ShopGui
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory


object BedwarsPlugin : ModInitializer {
	const val CONFIG_PATH = "bedwars"
	const val MOD_ID: String = "bedwars-plugin"

	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Bedwars plugin started")
		
		// register eventhandlers
        registerBlockBreakEvents()
        registerItemCallbacks()
        AfterRespawnEvent.registerEvent()
        
        registerCommands()
        
        BedwarsConfigData.initialise()
		registerItemCallbacks()
        registerPlayerJoinEvent()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
