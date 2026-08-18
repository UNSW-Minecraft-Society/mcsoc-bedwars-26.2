package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.gui.ShopGui
import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions


const val ROOT_NODE = "bedwars"


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
        dispatcher.register(Commands.literal(ROOT_NODE)
        .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
            .then(Commands.literal("open_shop_gui").executes(ShopGui::displayShop))
            .then(Commands.literal("test_simple_gui").executes(ShopGui::testSimpleGui))

            .then(Commands.literal("test_simple_gui_4").executes(ShopGui::testSimpleGui4))

            // only test commands
            // use: /bedwars upgradetool [Axe|Pickaxe|...]
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
