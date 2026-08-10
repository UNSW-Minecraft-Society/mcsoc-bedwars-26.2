package mcsoc.bedwars.eventhandlers

import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, selection ->
        dispatcher.register(
            Commands.literal(ROOT_NODE)
                .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }

                // only test commands
                // use: /bedwars upgradetool [Axe|Pickaxe|...]
                .then(
                    Commands.literal("upgradetool").then(
                        Commands.argument("tool", StringArgumentType.word())
                            .suggests(UpgradeItemsSuggestionProvider)
                            .executes {
                                val player = it.source.playerOrException
                                val toolArg = StringArgumentType.getString(it, "tool")

                                val type = UpgradeItemType.entries.firstOrNull { entry ->
                                    entry.name.equals(toolArg, ignoreCase = true)
                                }

                                if (type == null) {
                                    player.sendSystemMessage(Component.literal("$toolArg is not a valid tool"))
                                    return@executes 1
                                }

                                ModDataTracker.upgradeItem(player, type)
                                0
                            }
                    )
                )

                .then(Commands.literal("resettools").executes {
                    val player = it.source.playerOrException
                    ModDataTracker.clearItems(player)
                    0
                })
        )
    }
}