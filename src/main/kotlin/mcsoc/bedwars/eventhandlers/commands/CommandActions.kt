package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import mcsoc.bedwars.dataloaders.maploader.StructureLoader.Companion.getStructureLoader
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.utils.MapData
import mcsoc.bedwars.utils.format
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB


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
        val map_loader = level.getStructureLoader()
        
        return if (!map_loader.queueStructure(map_name, pos).join()) {
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
}

