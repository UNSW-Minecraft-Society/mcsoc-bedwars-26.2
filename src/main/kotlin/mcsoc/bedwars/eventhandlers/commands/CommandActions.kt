package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.TeamEffects
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader.Companion.place
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.MapData
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.world.phys.AABB
import kotlin.uuid.toKotlinUuid


fun setProtectionZoneMsg(p1: BlockPos, p2: BlockPos) = 
    Component.literal("Created new protection zone between ${p1.format} and ${p2.format}")

fun listProtectionZoneMsg(box: AABB): Component {
    val p1 = BlockPos.containing(box.minPosition)
    val p2 = BlockPos.containing(box.maxPosition)
    return Component.literal("  from ${p1.format} to ${p2.format}")
}

object CommandActions {
    fun setProtectionZone(ctx: CommandContext<CommandSourceStack>): Int {
        val p1 = BlockPosArgument.getBlockPos(ctx, FIRST_POSITION_ARGUMENT)
        val p2 = BlockPosArgument.getBlockPos(ctx, SECOND_POSITION_ARGUMENT)
        val res = ModDataTracker.registerProtectionZone(p1, p2)
        
        ctx.source.sendSuccess({setProtectionZoneMsg(p1, p2)}, true)
        return 1
    }
    
    fun listProtectionZones(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        source.sendSystemMessage(Component.literal("Protected Zones:"))
        ModDataTracker.getProtectionZones().forEach{z -> source.sendSystemMessage(listProtectionZoneMsg(z))}
        return 1
    }
    
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
    
    fun upgradeTool(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendFailure(Component.literal("Command must be run by a player"))
            return 0
        }
        val input = StringArgumentType.getString(ctx, "type")
        val type = try {
            UpgradeItemType.valueOf(input)
        } catch (e: IllegalArgumentException) {
            player.sendSystemMessage(Component.literal("$input is not a valid upgrade"))
            return 0
        }

        ModDataTracker.upgradeItem(player, type)
        return 1
    }
    
    fun resetTools(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        ModDataTracker.clearItems(player)
        return 1
    }
}