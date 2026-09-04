package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.upgrades.UpgradeItemType
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

        val team = ctx.source.level.gameState.getPlayersTeam(player.uuid)
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

    fun addGeneratorAtPlayer(ctx: CommandContext<CommandSourceStack>): Int {
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        return addGenerator(ctx.source, genArg, ctx.source.position)
    }

    fun addGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val bpos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()
        val pos = Vec3.atBottomCenterOf(bpos)
        return addGenerator(ctx.source, genArg, pos)
    }
    
    fun addGeneratorForTeam(ctx: CommandContext<CommandSourceStack>): Int {
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val bpos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above() 
        val pos = Vec3.atBottomCenterOf(bpos)
        return addGeneratorTeam(ctx.source, pos, teamArg)
    }

    fun removeGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()
        ctx.source.level.gameState.removeGenerator(Vec3.atBottomCenterOf(pos))
        return 1
    }
    
    fun removeGeneratorById(ctx: CommandContext<CommandSourceStack>): Int {
        val id: Int = IntegerArgumentType.getInteger(ctx, GEN_ID_ARG)
        ctx.source.level.gameState.removeGenerator(id)
        return 1
    }

    fun upgradeGeneratorTier(ctx: CommandContext<CommandSourceStack>): Int {
        val type = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val genType = try {
            GeneratorType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            ctx.source.sendFailure(Component.literal("$type is not a valid generator type"))
            return 0
        }
        
        ctx.source.level.gameState.upgradeGeneratorTier(genType)
        return 1
    }
    
    fun upgradeTeamGen(ctx: CommandContext<CommandSourceStack>): Int {
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val team = ctx.source.level.gameState.getActiveTeams().find { it.getName() == teamArg }
        if (team == null) {
            ctx.source.sendFailure(Component.literal("$teamArg is not a valid team"))
            return 0
        }
        
        ctx.source.level.gameState.upgradeTeamGenerator(team)
        return 1
    }
}


private fun addGenerator(src: CommandSourceStack, type: String, pos: Vec3): Int {
    val genType = try {
        GeneratorType.valueOf(type.uppercase())
    } catch (e: IllegalArgumentException) {
        src.sendFailure(Component.literal("$type is not a valid generator type"))
        return 0
    }
    
    val id = src.level.gameState.addGenerator(genType, pos, src.level)
    src.sendSystemMessage(Component.literal("added $type generator at ${pos.format} (Id: $id)"))
    return 1
}

private fun addGeneratorTeam(src: CommandSourceStack, pos: Vec3, teamStr: String): Int {
    val genType = GeneratorType.BASE
    
    val team = src.level.gameState.getActiveTeams().find { it.getName() == teamStr } ?: run {
        src.sendFailure(Component.literal("$teamStr is not a valid team"))
        return 0
    }
    
    val id = src.level.gameState.addGenerator(pos, src.level, team)
    src.sendSystemMessage(Component.literal("added base generator for team $teamStr at ${pos.format} (Id: $id)"))
    return 1
}
