package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.entities.CustomEntityType
import mcsoc.bedwars.entities.spawnShopkeeper
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.gui.ShopGui.displayShop
import mcsoc.bedwars.gui.ShopType
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.Vec3Argument
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

    fun summonShopkeeper(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        val posInput = Vec3Argument.getVec3(ctx, POSITION_ARG)
        val typeInput = StringArgumentType.getString(ctx, ENTITY_TYPE_ARG)
        val type = try {
            CustomEntityType.valueOf(typeInput.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendSystemMessage(Component.literal("$typeInput is not a valid entity"))
            return 0
        }
        when (type) {
            CustomEntityType.PLAYER_SHOPKEEPER -> spawnShopkeeper(player.level(), posInput, type)
            CustomEntityType.TEAM_SHOPKEEPER -> spawnShopkeeper(player.level(), posInput, type)
        }
        return 1
    }

    fun openShop(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        val input = StringArgumentType.getString(ctx, SHOP_TYPE_ARG)
        val type = try {
            ShopType.valueOf(input.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendSystemMessage(Component.literal("$input is not a valid shop"))
            return 0
        }
        try {
            displayShop(player, type)
            return 1
        } catch (e: Exception) {
            BedwarsPlugin.LOGGER.error(e.stackTraceToString())
            e.printStackTrace()
            return 0
        }
    }
}

