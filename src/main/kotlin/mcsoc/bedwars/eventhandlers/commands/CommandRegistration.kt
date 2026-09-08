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
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"

const val SOME_ARGUMENT = "some"
const val BOOL_ARGUMENT = "bool"
const val UPGRADE_TYPE_ARG = "type"
const val FIRST_POSITION_ARGUMENT = "pos1"
const val SECOND_POSITION_ARGUMENT = "pos2"

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
            )
            .then(Commands.literal("start")
                .requires(GAMEMASTER_PERMS_REQUIREMENT)
                .executes(CommandActions::start))
            .then(Commands.literal("end")
                .requires(GAMEMASTER_PERMS_REQUIREMENT)
                .executes(CommandActions::end))
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
        )
    }
}