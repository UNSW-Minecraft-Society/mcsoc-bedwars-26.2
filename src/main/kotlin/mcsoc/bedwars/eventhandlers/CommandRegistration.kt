package mcsoc.bedwars.eventhandlers


import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.dataloaders.maploader.MapLoader
import mcsoc.bedwars.utils.format

const val ROOT_NODE = "bedwars"

const val POSITION_ARGUMENT = "pos"
const val FIRST_POSITION_ARGUMENT = "pos1"
const val SECOND_POSITION_ARGUMENT = "pos2"

const val MAP_NAME_ARGUMENT = "name"

fun setProtectionZoneMsg(p1: BlockPos, p2: BlockPos) = 
    Component.literal("Created new protection zone between ${p1.format} and ${p2.format}")

fun listProtectionZoneMsg(box: AABB): Component {
    val p1 = BlockPos.containing(box.minPosition)
    val p2 = BlockPos.containing(box.maxPosition)
    return Component.literal("  from ${p1.format} to ${p2.format}")
}


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
        dispatcher.register(Commands.literal(ROOT_NODE)
        .then(Commands.literal("set_protection_zone")
            .then(Commands.argument(FIRST_POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .then(Commands.argument(SECOND_POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes{
                    val p1 = BlockPosArgument.getBlockPos(it, FIRST_POSITION_ARGUMENT)
                    val p2 = BlockPosArgument.getBlockPos(it, SECOND_POSITION_ARGUMENT)
                    val res = ModDataTracker.registerProtectionZone(p1, p2)
                    
                    it.source.sendSuccess({setProtectionZoneMsg(p1, p2)}, true)
                    1
                })
            )
        )
        .then(Commands.literal("list_protection_zones")
        .executes{
            val source = it.source
            source.sendSystemMessage(Component.literal("Protected Zones:"))
            ModDataTracker.getProtectionZones().forEach{z -> source.sendSystemMessage(listProtectionZoneMsg(z))}
            
            1
        })
        .then(Commands.literal("place_map")
            .then(Commands.argument(MAP_NAME_ARGUMENT, StringArgumentType.string())
            // .suggests(TODO)
                .then(Commands.argument(POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes{
                    val map_name = StringArgumentType.getString(it, MAP_NAME_ARGUMENT)
                    val pos = BlockPosArgument.getLoadedBlockPos(it, POSITION_ARGUMENT)
                    
                    val source = it.source
                    
                    val map_loader = MapLoader.getMapLoader()
                    val level = source.level
                    map_loader.loadMap(map_name)
                    map_loader.placeMap(level, pos)

                    source.sendSystemMessage(Component.literal("Placed"))            
                    1
                })
            )
        )
        )
    }
}