package mcsoc.bedwars.eventhandlers

import com.mojang.brigadier.arguments.IntegerArgumentType
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.server.permissions.Permissions
import kotlin.uuid.toKotlinUuid


const val ROOT_NODE = "bedwars"


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, selection ->
        dispatcher.register(
            Commands.literal(ROOT_NODE)
                .then(
                    // Usage: /bedwars join
                    Commands.literal("join").executes {
                        val player = it.source.player ?: run {
                            it.source.sendFailure(Component.literal("Command must be run by a player"))
                            return@executes 0
                        }

                        ModDataTracker.addActivePlayer(player.uuid)
                        player.sendSystemMessage(
                            Component.literal("You have joined the bedwars lobby").withColor(TextColor.GREEN)
                        )
                        1
                    }
                )
                .then(
                    // usage /bedwars leave
                    Commands.literal("leave").executes {
                        val player = it.source.player ?: run {
                            it.source.sendFailure(Component.literal("Command must be run by a player"))
                            return@executes 0
                        }

                        ModDataTracker.removeActivePlayer(player.uuid)
                        // todo add other things when a player leaves
                        player.sendSystemMessage(
                            Component.literal("You have left the bedwars lobby").withColor(TextColor.RED)
                        )
                        1
                    }
                )
                .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                .then(
                    Commands.literal("assign_teams")
                        // usage: /bedwars assign_teams <number of teams>
                        .then(
                            Commands.argument("number_of_teams", IntegerArgumentType.integer())
                                .executes {
                                    val input = IntegerArgumentType.getInteger(it, "number_of_teams")
                                    TeamEffects.createTeamsWithPlayers(input)
                                    1
                                }
                        
                        // usage: /bedwars get_team
                        ).then(
                            Commands.literal("get_team").executes {
                                val player = it.source.player ?: run {
                                    it.source.sendFailure(Component.literal("Command must be run by a player"))
                                    return@executes 0
                                }

                                val team = ModDataTracker.getPlayersTeam(player.uuid.toKotlinUuid())
                                player.sendSystemMessage(
                                    Component.literal("Your team is ${team.name}").withColor(TextColor.GREEN)
                                )
                                1
                            }
                        )
                )
        )
    }
}