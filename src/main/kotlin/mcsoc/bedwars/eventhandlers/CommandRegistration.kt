package mcsoc.bedwars.eventhandlers


import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.dataloaders.maploader.BedwarsIsland
import mcsoc.bedwars.dataloaders.maploader.BedwarsMapStructure
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.configloader.TomlConfigReader
import mcsoc.bedwars.datatrackers.configloader.YamlConfigReader
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.dataloaders.maploader.StructureLoader.Companion.getStructureLoader
import mcsoc.bedwars.utils.CylindricalBlockPos
import mcsoc.bedwars.utils.format
import kotlin.math.PI


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
        .then(Commands.literal("place_structure")
            .then(Commands.argument(MAP_NAME_ARGUMENT, StringArgumentType.string())
            // .suggests(TODO)
                .then(Commands.argument(POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes{
                    val map_name = StringArgumentType.getString(it, MAP_NAME_ARGUMENT)
                    val pos = BlockPosArgument.getLoadedBlockPos(it, POSITION_ARGUMENT)
                    
                    val source = it.source
                    
                    val level = source.level
                    val map_loader = level.getStructureLoader()
                    
                    if (!map_loader.queueStructure(map_name, pos).join()) {
                        source.sendSystemMessage(Component.literal("joever"))
                        0
                    } else {
                        source.sendSystemMessage(Component.literal("Placed"))
                        1
                    }
                })
            )   
        )
        .then(Commands.literal("place_map")
            .then(Commands.argument("base", StringArgumentType.string())
            .then(Commands.argument("mid", StringArgumentType.string())
            .then(Commands.argument("diamond", StringArgumentType.string())
            .then(Commands.argument("misc", StringArgumentType.string())
                .then(Commands.argument(POSITION_ARGUMENT, BlockPosArgument.blockPos())
                .executes{
                    val base_structure_name = StringArgumentType.getString(it, "base")
                    val mid_structure_name = StringArgumentType.getString(it, "mid")
                    val diamond_structure_name = StringArgumentType.getString(it, "diamond")
                    val misc_structure_name = StringArgumentType.getString(it, "misc")
                    
                    val pos = BlockPosArgument.getLoadedBlockPos(it, POSITION_ARGUMENT)
                    
                    val source = it.source
                    
                    val level = source.level
                    val map = BedwarsMapStructure(level,
                        listOf(
                            BedwarsIsland(CylindricalBlockPos(pos, 50F, (PI / 2.0F).toFloat(), 6), base_structure_name),
                            BedwarsIsland(CylindricalBlockPos(pos, 50F, (3 * PI / 2.0F).toFloat(), 6), base_structure_name)
                        ),
                        listOf(
                            BedwarsIsland(CylindricalBlockPos(pos, 20F, (PI / 4.0F).toFloat(), 3), diamond_structure_name),
                            BedwarsIsland(CylindricalBlockPos(pos, 20F, (5 * PI / 4.0F).toFloat(), 3), diamond_structure_name)
                        ),
                        BedwarsIsland(CylindricalBlockPos(pos, 0F, 0F, 0), mid_structure_name),
                        listOf(
                            BedwarsIsland(CylindricalBlockPos(pos, 20F, (3 * PI / 4.0F).toFloat(), -2), misc_structure_name),
                            BedwarsIsland(CylindricalBlockPos(pos, 20F, (7 * PI / 4.0F).toFloat(), -2), misc_structure_name)
                        )
                    )
                    map.place()
                    source.sendSystemMessage(Component.literal("Placed"))
                    1
                })
            ))))
        )
        .then(Commands.literal("reload")
        .requires{it.permissionContext.permissionLevel().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS)}
        .executes{ctx ->
            BedwarsConfigData.reloadConfig()
            1
        })
    )}
}