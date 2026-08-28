package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import kotlin.uuid.toKotlinUuid


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

    fun join(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }

        ModDataTracker.addActivePlayer(player.uuid)
        player.sendSystemMessage(
            Component.literal("You have joined the bedwars lobby").withColor(TextColor.GREEN)
        )

        return 1
    }

    fun leave(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }

        ModDataTracker.removeActivePlayer(player.uuid)
        // todo add other things when a player leaves
        player.sendSystemMessage(
            Component.literal("You have left the bedwars lobby").withColor(TextColor.RED)
        )

        return 1
    }

    fun assignTeams(ctx: CommandContext<CommandSourceStack>): Int {
        val input = IntegerArgumentType.getInteger(ctx, "number_of_teams")
        TeamEffects.createTeamsWithPlayers(input)
        return 1
    }

    fun getTeam(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }

        val team = ModDataTracker.getPlayersTeam(player.uuid.toKotlinUuid())
        player.sendSystemMessage(
            Component.literal("Your team is ${team.name}").withColor(TextColor.GREEN)
        )

        return 1
    }

    fun upgradeItem(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        val input = StringArgumentType.getString(ctx, UPGRADE_TYPE_ARG)
        val type = UpgradeItemType.entries.firstOrNull { name ->
            name.name.equals(input, ignoreCase = true)
        } ?: run {
            player.sendSystemMessage(Component.literal("$input is not a valid upgrade"))
            return 0
        }

        ModDataTracker.upgradeItem(player, type)
        return 1
    }

    fun resetUpgrades(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        ModDataTracker.clearItems(player)
        return 1
    }


}

