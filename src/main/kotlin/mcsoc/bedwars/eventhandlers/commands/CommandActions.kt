package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.generators.GeneratorKind
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
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

    // Ideally only use for testing. Start command creates teams now
    // Will need to make a new command that stores a number of teams in future - refer to bedhunt
    // for template
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
        return addGenerator(ctx.source, ctx.source.position, genArg)
    }

    fun addGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val bpos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()
        val pos = Vec3.atBottomCenterOf(bpos)
        return addGenerator(ctx.source, pos, genArg)
    }
    
    fun addTeamGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val bpos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above() 
        val pos = Vec3.atBottomCenterOf(bpos)
        return addGeneratorTeam(ctx.source, pos, teamArg)
    }

    fun removeGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, GEN_POS_ARG).above()
        ctx.source.level.generatorState.removeGenerator(Vec3.atBottomCenterOf(pos))
        ctx.source.sendSystemMessage(Component.literal("removed generator"))
        return 1
    }
    
    fun removeGeneratorById(ctx: CommandContext<CommandSourceStack>): Int {
        val id: Int = IntegerArgumentType.getInteger(ctx, GEN_ID_ARG)
        ctx.source.level.generatorState.removeGenerator(id)
        ctx.source.sendSystemMessage(Component.literal("removed generator with id: $id"))
        return 1
    }

    fun upgradeGeneratorTier(ctx: CommandContext<CommandSourceStack>): Int {
        val type = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val genType = GeneratorType.ENTRIES[type.uppercase()]
        
        if (genType == null) {
            ctx.source.sendFailure(Component.literal("$type is not an upgradable generator"))
            return 0
        }
        
        ctx.source.level.generatorState.upgradeGenerator(genType)
        return 1
    }
    
    fun upgradeTeamGen(ctx: CommandContext<CommandSourceStack>): Int {
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val team = ctx.source.level.gameState.getActiveTeams().find { it.getName() == teamArg }
        if (team == null) {
            ctx.source.sendFailure(Component.literal("$teamArg is not a valid team"))
            return 0
        }
        
        ctx.source.level.gameState.upgradeGen(team)
        return 1
    }
}


private fun addGenerator(src: CommandSourceStack, pos: Vec3, type: String): Int {
    val genType = GeneratorType.ENTRIES[type.uppercase()]
        
    if (genType == null) {
        src.sendFailure(Component.literal("$type is not a valid generator type"))
        return 0
    }
    
    val id = src.level.generatorState.addGenerator(src.server, pos, src.level.dimension(), genType)
    src.sendSystemMessage(Component.literal("added $type generator at ${pos.format} (Id: $id)"))
    return 1
}

private fun addGeneratorTeam(src: CommandSourceStack, pos: Vec3, teamStr: String): Int {
    val team = src.level.gameState.getActiveTeams().find { it.getName() == teamStr } ?: run {
        src.sendFailure(Component.literal("$teamStr is not a valid team"))
        return 0
    }
    
    val id = src.level.generatorState.addTeamGenerator(src.server, pos, src.level.dimension(), team)
    src.sendSystemMessage(Component.literal("added base generator for team $teamStr at ${pos.format} (Id: $id)"))
    // try {
    // } catch (e: Exception) {
    //     println(e.cause)
    //     println(e.message)
    //     e.printStackTrace()
    // }
    return 1
}
