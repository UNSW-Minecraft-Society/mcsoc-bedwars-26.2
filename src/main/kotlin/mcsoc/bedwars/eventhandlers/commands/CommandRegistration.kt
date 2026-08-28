package mcsoc.bedwars.eventhandlers.commands


import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import mcsoc.bedwars.gui.ShopGui
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.network.chat.Component
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
                    .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                    .then(Commands.argument("number_of_teams", IntegerArgumentType.integer())
                        .executes(CommandActions::assignTeams)
                    )
                )
                .then(Commands.literal("open_shop_gui").executes(ShopGui::displayShop))
                .then(Commands.literal("test_simple_gui").executes(ShopGui::testSimpleGui))

                .then(Commands.literal("test_simple_gui_4").executes(ShopGui::testSimpleGui4))

                .then(
                    Commands.literal("upgrade").then(
                        Commands.argument("type", StringArgumentType.word())
                            .suggests(UpgradeItemsSuggestionProvider)
                            .executes {
                                val player = it.source.player ?: run {
                                    it.source.sendFailure(Component.literal("Command must be run by a player"))
                                    return@executes 0
                                }
                                val input = StringArgumentType.getString(it, "type")
                                val type = UpgradeItemType.entries.firstOrNull { name ->
                                    name.name.equals(input, ignoreCase = true)
                                } ?: run {
                                    player.sendSystemMessage(Component.literal("$input is not a valid upgrade"))
                                    return@executes 0
                                }

                                ModDataTracker.upgradeItem(player, type)
                                1
                            }
                    )
                )
                .then(Commands.literal("resettools").executes {
                    val player = it.source.playerOrException
                    ModDataTracker.clearItems(player)
                    1
                })
        )
    }
}