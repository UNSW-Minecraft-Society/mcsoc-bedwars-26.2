package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.blockProtection
import mcsoc.bedwars.datatrackers.blockprotection.BlockProtectionTracker
import mcsoc.bedwars.datatrackers.blockprotection.ProtectionZone
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import kotlin.uuid.toKotlinUuid


private fun setProtectionZoneMsg(p1: BlockPos, p2: BlockPos) = 
    Component.literal("Created new protection zone between ${p1.format} and ${p2.format}")

private fun listProtectionZoneMsg(zone: ProtectionZone): Component {
    val p1 = BlockPos.containing(zone.box.minPosition)
    val p2 = BlockPos.containing(zone.box.maxPosition)
    return Component.literal("  ID: ${zone.id}, from ${p1.format} to ${p2.format}")
}


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
        ctx.source.level.gameState.addActivePlayer(player.uuid)
        player.sendSystemMessage(Component.literal("You have joined the bedwars lobby").withColor(TextColor.GREEN))

        return 1
    }

    fun leave(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }

        ctx.source.level.gameState.removeActivePlayer(player.uuid)
        player.sendSystemMessage(Component.literal("You have left the bedwars lobby").withColor(TextColor.RED))

        return 1
    }

    fun assignTeams(ctx: CommandContext<CommandSourceStack>): Int {
        val input = IntegerArgumentType.getInteger(ctx, "number_of_teams")
        TeamEffects.createTeamsWithPlayers(ctx.source.level, input)
        return 1
    }

    fun getTeam(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }

        val team = ctx.source.level.gameState.getPlayersTeam(player.uuid.toKotlinUuid())
        player.sendSystemMessage(Component.literal("Your team is ${team.name}").withColor(TextColor.GREEN))

        return 1
    }

    fun start(ctx: CommandContext<CommandSourceStack>): Int {
        IntegerArgumentType.getInteger(ctx, "num_teams")
        GameManager.setupGame(ctx.source.level, ctx.source.position)
        return 1
    }

    fun end(ctx: CommandContext<CommandSourceStack>): Int {
        GameManager.endGame(ctx.source.level)
        return 1
    }
    fun upgradeItem(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        val input = StringArgumentType.getString(ctx, UPGRADE_TYPE_ARG)
        val type = try {
            UpgradeItemType.valueOf(input)
        } catch (e: IllegalArgumentException) {
            player.sendSystemMessage(Component.literal("$input is not a valid upgrade"))
            return 0
        }

        ctx.source.level.gameState.upgradeItem(player, type)
        return 1
    }

    fun resetUpgrades(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        ctx.source.level.gameState.clearItems(player)
        return 1
    }

    fun setProtectionZone(ctx: CommandContext<CommandSourceStack>): Int {
        val p1 = BlockPosArgument.getBlockPos(ctx, FIRST_POSITION_ARGUMENT)
        val p2 = BlockPosArgument.getBlockPos(ctx, SECOND_POSITION_ARGUMENT)
        val res = ctx.source.level.blockProtection.registerProtectionZone(p1, p2)
        
        ctx.source.sendSuccess({setProtectionZoneMsg(p1, p2)}, true)
        return 1
    }
    
    fun listProtectionZones(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        source.sendSystemMessage(Component.literal("Protected Zones:"))
        ctx.source.level.blockProtection.getProtectionZones().forEach{z -> source.sendSystemMessage(listProtectionZoneMsg(z))}
        return 1
    }

}

