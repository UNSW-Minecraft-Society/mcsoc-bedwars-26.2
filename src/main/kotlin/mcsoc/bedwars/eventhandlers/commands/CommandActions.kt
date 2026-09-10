package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.items.BedwarsItems
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.blockProtection
import mcsoc.bedwars.datatrackers.blockprotection.BlockProtectionTracker
import mcsoc.bedwars.datatrackers.blockprotection.ProtectionZone
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.configloader.MapData
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader.Companion.place
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.entities.CustomEntityType
import mcsoc.bedwars.entities.spawnShopkeeper
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.items.CustomItemTypes
import mcsoc.bedwars.gui.ShopGui.displayShop
import mcsoc.bedwars.gui.ShopType
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.TextColor
import net.minecraft.world.phys.AABB
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3


private fun setProtectionZoneMsg(p1: BlockPos, p2: BlockPos): () -> Component = 
        {Component.literal("Created new protection zone between ${p1.format} and ${p2.format}")}

private fun listProtectionZoneMsg(zone: ProtectionZone): () -> Component {
    return {
        val p1 = BlockPos.containing(zone.box.minPosition)
        val p2 = BlockPos.containing(zone.box.maxPosition)
        Component.literal("  ID: ${zone.id}, from ${p1.format} to ${p2.format}")
    }
}

private fun blockProtectionGetMsg(state: Boolean): () -> Component {
    return {
        if (state) Component.literal("Block Protection is enabled.")
        else Component.literal("Block Protection is disabled.")
    }
}

private fun blockProtectionSetMsg(state: Boolean): () -> Component {
    return {
        if (state) Component.literal("Block Protection is now enabled.")
        else Component.literal("Block Protection is now disabled.")
    }
}

internal object CommandActions {
    fun placeStructure(ctx: CommandContext<CommandSourceStack>): Int {
        val map_name = StringArgumentType.getString(ctx, MAP_NAME_ARGUMENT)
        val pos = BlockPosArgument.getLoadedBlockPos(ctx, POSITION_ARGUMENT)
        
        val source = ctx.source
        
        val level = source.level
        
        
        return if (!level.place(map_name, pos).join()) {
            source.sendFailure(Component.literal("Failed to place $map_name"))
            0
        } else {
            source.sendSystemMessage(Component.literal("Placed $map_name at ${pos.format}"))
            1
        }
    }
    
    fun placeMap(ctx: CommandContext<CommandSourceStack>): Int {
        val map_name = StringArgumentType.getString(ctx, MAP_NAME_ARGUMENT)
        val pos = BlockPosArgument.getLoadedBlockPos(ctx, POSITION_ARGUMENT)
        
        val source = ctx.source
        
        val level = source.level
        val map: MapData = BedwarsConfigData.map_data[map_name] ?: run{
            source.sendFailure(Component.literal("No map exists with id $map_name"))
            return 0
        }
        map.place(level, pos)
        source.sendSystemMessage(Component.literal("Placed $map_name"))
        return 1
    }
    
    fun reload(ctx: CommandContext<CommandSourceStack>): Int {
        BedwarsConfigData.reloadConfig()
        return 1
    }
    
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
        TeamEffects.createTeamsWithPlayers(ctx.source.level)
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
        val map_name = StringArgumentType.getString(ctx, MAP_NAME_ARGUMENT)
        val pos = BlockPosArgument.getLoadedBlockPos(ctx, POSITION_ARGUMENT)
        
        GameManager.setupGame(map_name, ctx.source.level, pos)
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

    fun giveCustomItem(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        val input = StringArgumentType.getString(ctx, CUSTOM_ITEM_ARG)
        val type = try {
            CustomItemTypes.valueOf(input.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendSystemMessage(Component.literal("$input is not a valid custom item"))
            return 0
        }
        return type.giveToPlayer(ctx.source.player)
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
        val posInput = Vec3Argument.getVec3(ctx, POSITION_ARGUMENT)
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

    fun setProtectionZone(ctx: CommandContext<CommandSourceStack>): Int {
        val p1 = BlockPosArgument.getBlockPos(ctx, FIRST_POSITION_ARGUMENT)
        val p2 = BlockPosArgument.getBlockPos(ctx, SECOND_POSITION_ARGUMENT)
        val res = ctx.source.level.blockProtection.registerProtectionZone(p1, p2)

        ctx.source.sendSuccess(setProtectionZoneMsg(p1, p2), true)
        return 1
    }

    fun listProtectionZones(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        source.sendSystemMessage(Component.literal("Protected Zones:"))
        source.level.blockProtection.getProtectionZones().forEach{z -> source.sendSystemMessage(listProtectionZoneMsg(z)())}
        return 1
    }

    fun getProtectionState(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        val state = source.level.blockProtection.protectionEnabled
        source.sendSystemMessage(blockProtectionGetMsg(state)())
        return 1
    }

    fun setProtectionState(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        val state = BoolArgumentType.getBool(ctx, BOOL_ARGUMENT)
        source.sendSuccess(blockProtectionSetMsg(state), true)
        source.level.blockProtection.protectionEnabled = state
        return 1
    }    fun addGeneratorAtPlayer(ctx: CommandContext<CommandSourceStack>): Int {
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        return addGenerator(ctx.source, ctx.source.position, genArg)
    }

    fun addGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val genArg = StringArgumentType.getString(ctx, GEN_TYPE_ARG)
        val bpos: BlockPos = BlockPosArgument.getBlockPos(ctx, POSITION_ARGUMENT).above()
        val pos = Vec3.atBottomCenterOf(bpos)
        return addGenerator(ctx.source, pos, genArg)
    }

    fun addTeamGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val teamArg = StringArgumentType.getString(ctx, GEN_TEAM_ARG)
        val bpos: BlockPos = BlockPosArgument.getBlockPos(ctx, POSITION_ARGUMENT
        ).above()
        val pos = Vec3.atBottomCenterOf(bpos)
        return addGeneratorTeam(ctx.source, pos, teamArg)
    }

    fun removeGenerator(ctx: CommandContext<CommandSourceStack>): Int {
        val pos: BlockPos = BlockPosArgument.getBlockPos(ctx, POSITION_ARGUMENT).above()
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
        val genType = GeneratorType.parseTypeString(type.uppercase()) ?: run {
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
    val genType = GeneratorType.parseTypeString(type.uppercase()) ?: run {
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
    return 1
}
