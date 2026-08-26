package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.items.BedwarsItems
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer


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

    fun giveFireball(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player
        if (player is ServerPlayer && player.addItem(BedwarsItems.fireballItemStack())) {
            return 0
        }
        return 1
    }
}

