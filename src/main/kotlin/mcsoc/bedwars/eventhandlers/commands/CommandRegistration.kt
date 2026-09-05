package mcsoc.bedwars.eventhandlers.commands


import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val SOME_ARGUMENT = "some"
const val UPGRADE_TYPE_ARG = "type"

/**
 * Function to register commands for the plugin
 */
fun registerCommands() {
    CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, selection ->
        dispatcher.register(
            Commands.literal(ROOT_NODE)
                .then(
                    Commands.literal("ping")
                        .requires { it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) }
                        .executes(CommandActions::ping)
                        .then(
                            Commands.argument(SOME_ARGUMENT, StringArgumentType.word())
                                .suggests(ExampleSuggestionProvider())
                                .executes(CommandActions::pingWord)
                        )
                        .then(Commands.literal("start")
                            .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                            .executes(CommandActions::start))
                        .then(Commands.literal("end")
                            .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                            .executes(CommandActions::end))
                )
                .then(Commands.literal("join").executes(CommandActions::join))
                .then(Commands.literal("leave").executes(CommandActions::leave))
                .then(Commands.literal("get_team").executes(CommandActions::getTeam))
                .then(
                    Commands.literal("assign_teams")
                        .requires { source ->
                            source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)
                        }
                        .then(
                            Commands.argument("number_of_teams", IntegerArgumentType.integer())
                                .executes(CommandActions::assignTeams)
                        )
                )
                .then(Commands.literal("upgrade")
                    .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                    .then(Commands.argument(UPGRADE_TYPE_ARG, StringArgumentType.word())
                        .suggests(UpgradeItemsSuggestionProvider())
                        .executes(CommandActions::upgradeItem)
                    )
                )
                .then(Commands.literal("reset_upgrades")
                    .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                    .executes(CommandActions::resetUpgrades)
                )
        )
    }
}