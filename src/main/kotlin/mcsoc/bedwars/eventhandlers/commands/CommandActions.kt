package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3


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

        val team = ModDataTracker.getPlayersTeam(player.uuid)
        player.sendSystemMessage(
            Component.literal("Your team is ${team.name}").withColor(TextColor.GREEN)
        )

        return 1
    }

    fun addGeneratorAtPlayer(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        return addGenerator(player, genArg, player.blockPosition())
    }

    fun addGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()        
        return addGenerator(player, genArg, pos)
    }
    
    fun addGeneratorForTeam(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()        
        return addGeneratorTeam(player, genArg, pos, teamArg)
    }

    fun removeGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()
        ModDataTracker.removeGenerator(Vec3.atBottomCenterOf(pos))
        return 1
    }
    
    fun removeGeneratorById(ctx: CommandContext<CommandSourceStack>): Int {
        val id: Int = IntegerArgumentType.getInteger(ctx, GEN_ID_ARG)
        ModDataTracker.removeGenerator(id)
        return 1
    }

    fun upgradeGeneratorTier(ctx: CommandContext<CommandSourceStack>): Int {
        ModDataTracker.upgradeGeneratorTier()
        return 1
    }
    
    fun upgradeTeamGen(ctx: CommandContext<CommandSourceStack>): Int {
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val team = ModDataTracker.getActiveTeams().find { it.getName() == teamArg }
        if (team == null) {
            ctx.source.sendSystemMessage(Component.literal("$teamArg is not a valid team"))
            return 0
        }
        
        ModDataTracker.upgradeTeamGenerators(team)
        return 1
    }
}


private fun addGenerator(player: ServerPlayer, type: String, block: BlockPos): Int {
    val position = Vec3.atBottomCenterOf(block)
    val genType = try {
        GeneratorType.valueOf(type.uppercase())
    } catch (e: IllegalArgumentException) {
        player.sendSystemMessage(Component.literal("$type is not a valid generator type"))
        return 0
    }
    
    val id = ModDataTracker.addGenerator(genType, position, player.level())
    player.sendSystemMessage(Component.literal("added $type generator at ${position.format} (Id: $id)"))
    return 1
}

private fun addGeneratorTeam(player: ServerPlayer, type: String, block: BlockPos, teamStr: String): Int {
    val position = Vec3.atBottomCenterOf(block)        
    val genType = try {
        GeneratorType.valueOf(type)
    } catch (e: IllegalArgumentException) {
        player.sendSystemMessage(Component.literal("$type is not a valid generator type"))
        return 0
    }
    
    val team = ModDataTracker.getActiveTeams().find { it.getName() == teamStr } ?: run {
        player.sendSystemMessage(Component.literal("$teamStr is not a valid team"))
        return 0
    }
    
    val id = ModDataTracker.addGenerator(genType, position, player.level(), team)
    player.sendSystemMessage(Component.literal("added $type generator at ${position.format} (Id: $id)"))
    return 1
}
