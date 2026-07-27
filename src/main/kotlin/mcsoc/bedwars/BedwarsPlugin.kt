package mcsoc.bedwars

import mcsoc.bedwars.eventhandlers.registerBlockBreakEvents
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object BedwarsPlugin : ModInitializer {
	const val MOD_ID: String = "bedwars-plugin"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!")

        registerBlockBreakEvents()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
