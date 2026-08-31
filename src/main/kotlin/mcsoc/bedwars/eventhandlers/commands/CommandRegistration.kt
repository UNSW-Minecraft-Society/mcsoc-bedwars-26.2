package mcsoc.bedwars.eventhandlers.commands


import com.mojang.brigadier.arguments.IntegerArgumentType
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
    CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, selection ->
        dispatcher.register(
            Commands.literal(ROOT_NODE)
                .then(Commands.literal("ping")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) }
                    .executes(CommandActions::ping)
                    .then(Commands.argument(SOME_ARGUMENT, StringArgumentType.word())
                        .suggests(ExampleSuggestionProvider())
                        .executes(CommandActions::pingWord)
                    )
                )
                .then(Commands.literal("join").executes(CommandActions::join))
                .then(Commands.literal("leave").executes(CommandActions::leave))
                .then(Commands.literal("get_team").executes(CommandActions::getTeam))
                .then(Commands.literal("assign_teams")
                    .then(Commands.argument("number_of_teams", IntegerArgumentType.integer())
                        .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                        .executes(CommandActions::assignTeams)
                    )
                )
                .then(Commands.literal("give_fireball")
                    .requires {it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}
                    .executes(CommandActions::giveFireball))
                .then(Commands.literal("give_bridge_egg")
                    .requires {it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}
                    .executes(CommandActions::giveBridgeEgg))
                .then(Commands.literal("give_instant_tnt")
                    .requires {it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}
                    .executes(CommandActions::giveInstantTNT))
        )
    }
}