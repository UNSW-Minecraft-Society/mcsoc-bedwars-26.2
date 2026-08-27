package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
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

    fun addGeneratorAtPlayer(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val genArg = StringArgumentType.getString(ctx, "type")
        return addGenerator(player, genArg, player.blockPosition())
    }

    fun addGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val genArg = StringArgumentType.getString(ctx, "type")
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, "pos").above()        
        return addGenerator(player, genArg, pos)
    }

    fun removeGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, "pos").above()
        ModDataTracker.removeGenerator(Vec3.atBottomCenterOf(pos))
        return 1
    }

    fun upgradeGeneratorTier(ctx: CommandContext<CommandSourceStack>): Int {
        ModDataTracker.upgradeGeneratorTier()
        return 1
    }


    private fun addGenerator(player: ServerPlayer, type: String, block: BlockPos): Int {
        val position = Vec3.atBottomCenterOf(block)
        val success = ModDataTracker.addGenerator(type, position, player.level().dimension())

        if (!success) {
            player.sendSystemMessage(Component.literal("$type is not a valid generator"))
            return 0
        }

        player.sendSystemMessage(Component.literal("added $type generator at ${position.format}"))
        return 1
    }
}

