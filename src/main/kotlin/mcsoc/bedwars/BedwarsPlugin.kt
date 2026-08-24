package mcsoc.bedwars

import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import org.slf4j.LoggerFactory
import net.minecraft.resources.Identifier
import net.fabricmc.api.ModInitializer
import mcsoc.bedwars.eventhandlers.registerCommands


import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier

import mcsoc.bedwars.eventhandlers.registerBlockBreakEvents
import mcsoc.bedwars.eventhandlers.registerCommands
import net.minecraft.world.level.Level
import java.nio.file.Files
import kotlin.io.path.div


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
        registerBlockBreakEvents()
        registerCommands()

        BedwarsConfigData.initialise()
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
