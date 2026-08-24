package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component


object CommandActions {    
    fun ping(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sendSystemMessage(Component.literal("pong!"))
        return 1
    }
    
    fun pingWord(ctx: CommandContext<CommandSourceStack>): Int {
        val word = StringArgumentType.getString(ctx, SOME_ARGUMENT)
        ctx.source.sendSystemMessage(Component.literal(word))
        return 1
    }
}

