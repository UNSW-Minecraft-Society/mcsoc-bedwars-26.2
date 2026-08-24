package mcsoc.bedwars.eventhandlers.commands


import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.PermissionLevel
import mcsoc.bedwars.eventhandlers.commands.CommandActions
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val SOME_ARGUMENT = "some"

/**
 * Function to register commands for the plugin
 */
fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
    dispatcher.register(Commands.literal(ROOT_NODE)
        .then(Commands.literal("ping")
        .requires{it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}
        .executes(CommandActions::ping)
            .then(Commands.argument(SOME_ARGUMENT, StringArgumentType.word())
            .suggests(ExampleSuggestionProvider())
            .executes(CommandActions::pingWord)
            )
        )
    )}
}