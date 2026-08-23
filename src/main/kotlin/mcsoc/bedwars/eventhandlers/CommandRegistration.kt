package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.configloader.TomlConfigReader
import mcsoc.bedwars.datatrackers.configloader.YamlConfigReader
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel


const val ROOT_NODE = "bedwars"

fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
    dispatcher.register(Commands.literal(ROOT_NODE)
        .then(Commands.literal("reload")
        .requires{it.permissionContext.permissionLevel().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS)}
        .executes{ctx ->
            BedwarsConfigData.reloadConfig()
            1
        })
    )}
}