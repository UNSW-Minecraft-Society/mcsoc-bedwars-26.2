package mcsoc.bedwars.eventhandlers.commands


import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import mcsoc.bedwars.gui.ShopGui
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val SOME_ARGUMENT = "some"
const val UPGRADE_TYPE_ARG = "type"
const val ENTITY_TYPE_ARG = "type2"
const val SHOP_TYPE_ARG = "type3"
const val POSITION_ARG = "pos"
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
                .then(Commands.literal("open_shop_gui").executes(CommandActions::openShop)
                    .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                    .then(Commands.argument(SHOP_TYPE_ARG, StringArgumentType.word())
                        .suggests(ShopTypeSuggestionProvider())
                        .executes(CommandActions::openShop)
                    )
                )
                .then(Commands.literal("test_simple_gui").executes(ShopGui::testSimpleGui))

                .then(Commands.literal("test_simple_gui_4").executes(ShopGui::testSimpleGui4))
                .then(Commands.literal("summon_shopkeeper")
                    .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)}
                    .then(Commands.argument(POSITION_ARG, Vec3Argument.vec3())
                        .then(Commands.argument(ENTITY_TYPE_ARG, StringArgumentType.word())
                            .suggests(EntityTypeSuggestionProvider())
                            .executes(CommandActions::summonShopkeeper)
                        )
                    )
                )
        )
    }
}