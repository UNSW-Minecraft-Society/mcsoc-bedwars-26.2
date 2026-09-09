package mcsoc.bedwars.eventhandlers.commands


import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.BedwarsPlugin
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.resources.Identifier
import net.minecraft.server.permissions.PermissionLevel
import mcsoc.bedwars.gui.ShopGui
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val SOME_ARGUMENT = "some"
const val BOOL_ARGUMENT = "bool"
const val UPGRADE_TYPE_ARG = "type"
const val ENTITY_TYPE_ARG = "type2"
const val SHOP_TYPE_ARG = "type3"
const val POSITION_ARG = "pos"
const val CUSTOM_ITEM_ARG = "custom_item"
const val FIRST_POSITION_ARGUMENT = "pos1"
const val SECOND_POSITION_ARGUMENT = "pos2"
const val GEN_TYPE_ARG = "type"
const val GEN_POS_ARG = "pos"
const val GEN_TEAM_ARG = "team"
const val GEN_ID_ARG = "id"

val BEDWARS_GM_PERMISSION_NODE = Identifier.fromNamespaceAndPath(BedwarsPlugin.MOD_ID, "runner")
val GAMEMASTER_PERMS_REQUIREMENT: (CommandSourceStack) -> Boolean = {it.permissionContext.checkPermission(BEDWARS_GM_PERMISSION_NODE, PermissionLevel.GAMEMASTERS)}

/**
 * Function to register commands for the plugin
 */
fun registerCommands() {
    CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, selection ->
    dispatcher.register(
        Commands.literal(ROOT_NODE)
            .then(
                Commands.literal("ping")
                    .requires(GAMEMASTER_PERMS_REQUIREMENT)
                    .executes(CommandActions::ping)
                    .then(
                        Commands.argument(SOME_ARGUMENT, StringArgumentType.word())
                            .suggests(ExampleSuggestionProvider())
                            .executes(CommandActions::pingWord)
                    )
                    .then(Commands.literal("start")
                        .requires(GAMEMASTER_PERMS_REQUIREMENT)
                        .executes(CommandActions::start))
                    .then(Commands.literal("end")
                        .requires(GAMEMASTER_PERMS_REQUIREMENT)
                        .executes(CommandActions::end))
            )
            .then(Commands.literal("join")
            .executes(CommandActions::join)
            )
            .then(Commands.literal("leave")
            .executes(CommandActions::leave)
            )
            .then(Commands.literal("get_team")
            .executes(CommandActions::getTeam)
            )
            .then(Commands.literal("assign_teams")
            .requires(GAMEMASTER_PERMS_REQUIREMENT)
                .then(Commands.argument("number_of_teams", IntegerArgumentType.integer())
                .executes(CommandActions::assignTeams)
                )
            )
            .then(Commands.literal("upgrade")
            .requires(GAMEMASTER_PERMS_REQUIREMENT)
                .then(Commands.argument(UPGRADE_TYPE_ARG, StringArgumentType.word())
                    .suggests(UpgradeItemsSuggestionProvider())
                    .executes(CommandActions::upgradeItem)
                )
            )
            .then(Commands.literal("reset_upgrades")
            .requires(GAMEMASTER_PERMS_REQUIREMENT)
            .executes(CommandActions::resetUpgrades)
            )
            .then(Commands.literal("set_protection_zone")
                .requires(GAMEMASTER_PERMS_REQUIREMENT)
                .then(Commands.argument(FIRST_POSITION_ARGUMENT, BlockPosArgument.blockPos())
                    .then(Commands.argument(SECOND_POSITION_ARGUMENT, BlockPosArgument.blockPos())
                    .executes(CommandActions::setProtectionZone)
                    )
                )
            )
            .then(Commands.literal("list_protection_zones")
            .executes(CommandActions::listProtectionZones)
            )
            .then(Commands.literal("protection_state")
                .executes(CommandActions::getProtectionState)
                .then(Commands.literal("set")
                .requires(GAMEMASTER_PERMS_REQUIREMENT)
                    .then(Commands.argument(BOOL_ARGUMENT, BoolArgumentType.bool())
                    .executes(CommandActions::setProtectionState)
                    )
                )
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
                ).then(Commands.literal("add_team_gen")
                    .then(Commands.argument(GEN_POS_ARG, BlockPosArgument.blockPos())
                        .then(Commands.argument(GEN_TEAM_ARG, StringArgumentType.word())
                            .suggests(TeamSuggestionProvider())
                            .executes(CommandActions::addTeamGenerator)
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
            .then(Commands.literal("give_custom_item")
                .requires {it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}
                .then(Commands.argument(CUSTOM_ITEM_ARG, StringArgumentType.word())
                    .suggests(CustomItemsSuggestionsProvider())
                    .executes(CommandActions::giveCustomItem)
                )
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