package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.PermissionLevel
import mcsoc.bedwars.eventhandlers.commands.CommandActions
import mcsoc.bedwars.eventhandlers.commands.LoadedMapSuggestionProvider
import mcsoc.bedwars.eventhandlers.commands.AvailableStructureSuggestionProvider
import mcsoc.bedwars.gui.ShopGui
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val POSITION_ARGUMENT = "pos"
const val FIRST_POSITION_ARGUMENT = "pos1"
const val SECOND_POSITION_ARGUMENT = "pos2"

const val MAP_NAME_ARGUMENT = "name"

const val SOME_ARGUMENT = "some"

/**
 * Function to register commands for the plugin
 */
fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
    dispatcher.register(Commands.literal(ROOT_NODE)
        .then(Commands.literal("set_protection_zone")
            .then(Commands.argument(FIRST_POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .then(Commands.argument(SECOND_POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes(CommandActions::setProtectionZone)
                )
            )
        )
        .then(Commands.literal("list_protection_zones")
        .executes(CommandActions::listProtectionZones)
        )
        .then(Commands.literal("place_structure")
            .then(Commands.argument(MAP_NAME_ARGUMENT, StringArgumentType.string())
            .suggests(AvailableStructureSuggestionProvider())
                .then(Commands.argument(POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes(CommandActions::placeStructure)
                )   
            )
        )
        .then(Commands.literal("place_map")
            .then(Commands.argument(MAP_NAME_ARGUMENT, StringArgumentType.string())
            .suggests(LoadedMapSuggestionProvider())
                .then(Commands.argument(POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes(CommandActions::placeMap)
                )
            )
        )
        .then(Commands.literal("reload")
        .requires{it.permissionContext.permissionLevel().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS)}
        .executes(CommandActions::reload)
        )
        .then(Commands.literal("ping")
        .requires{it.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}
        .executes(CommandActions::ping)
            .then(Commands.argument(SOME_ARGUMENT, StringArgumentType.word())
            .suggests(ExampleSuggestionProvider())
            .executes(CommandActions::pingWord)
            )
        )
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
        .then(Commands.literal("open_shop_gui")
        .executes(ShopGui::displayShop)
        )
        .then(Commands.literal("test_simple_gui")
        .executes(ShopGui::testSimpleGui)
        )
        .then(Commands.literal("test_simple_gui_4")
        .executes(ShopGui::testSimpleGui4)
        )

    }
}