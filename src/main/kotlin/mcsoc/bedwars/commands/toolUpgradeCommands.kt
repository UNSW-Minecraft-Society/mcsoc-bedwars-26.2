package mcsoc.bedwars.commands

import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.ToolCategory
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions


fun registerToolCommands() {
    CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
        dispatcher.register(Commands.literal("bedwars")
            .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }

            // only test commands
            // use: /bedwars upgradetool [Axe|Pickaxe|...]
            .then(Commands.literal("upgradetool")
                .then(Commands.argument("tool", StringArgumentType.word()).executes { 
                    val player = it.source.playerOrException
                    val toolArg = StringArgumentType.getString(it, "tool")
                    
                    try {
                        ModDataTracker.upgradeTool(player, enumValueOf<ToolCategory>(toolArg.uppercase()))
                    } catch (e: Exception) {
                        player.sendSystemMessage(Component.literal("$toolArg is not a valid tool"))
                        e.printStackTrace()
                        return@executes 1
                    }
                    0
                })
            )
            
            .then(Commands.literal("resettools").executes { 
                val player = it.source.playerOrException
                ModDataTracker.clearTools(player)
                0
            })
            
        )
    }
}

