package mcsoc.bedwars.eventhandlers.commands


import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val SOME_ARGUMENT = "some"
const val UPGRADE_TYPE_ARG = "type"
const val GEN_TYPE_ARG = "type"
const val GEN_POS_ARG = "pos"
const val GEN_TEAM_ARG = "team"
const val GEN_ID_ARG = "id"

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
                .then(Commands.literal("generator")
                    .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .then(Commands.literal("add")
                        .then(Commands.argument(GEN_TYPE_ARG, StringArgumentType.word())
                            .suggests(GeneratorSuggestionProvider())
                            .executes(CommandActions::addGeneratorAtPlayer)
                            .then(Commands.argument(GEN_POS_ARG, BlockPosArgument.blockPos())
                                .executes(CommandActions::addGenerator)
                            )
                        )
                    ).then(Commands.literal("add_team")
                        .then(Commands.argument(GEN_POS_ARG, BlockPosArgument.blockPos())
                            .then(Commands.argument(GEN_TEAM_ARG, StringArgumentType.word())
                                .suggests(TeamSuggestionProvider())
                                .executes(CommandActions::addGeneratorForTeam)
                            )
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument(GEN_POS_ARG, BlockPosArgument.blockPos())
                            .executes(CommandActions::removeGenerator)
                        )
                        .then(Commands.literal("id")
                            .then(Commands.argument(GEN_ID_ARG, IntegerArgumentType.integer())
                                .executes(CommandActions::removeGeneratorById)
                            )
                        )
                    )
                    .then(Commands.literal("upgrade_tiers")
                        .then(Commands.argument(GEN_TYPE_ARG, StringArgumentType.word())
                            .suggests(GeneratorSuggestionProvider())
                            .executes(CommandActions::upgradeGeneratorTier)
                        )
                    )
                    .then(Commands.literal("upgrade_team_gen")
                        .then(Commands.argument(GEN_TEAM_ARG, StringArgumentType.word())
                            .suggests(TeamSuggestionProvider())
                            .executes(CommandActions::upgradeTeamGen)
                        )
                    )
                )
        )
    }
}