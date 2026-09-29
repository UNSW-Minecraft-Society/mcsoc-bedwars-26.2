package mcsoc.bedwars.utils

import mcsoc.bedwars.datatrackers.blockProtection
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

fun placeBlockIfValid(level: Level, blockPos: BlockPos, blockState: BlockState) {
    if (level !is ServerLevel) return
    val curBlockState = level.getBlockState(blockPos)
    if (curBlockState.`is`(Blocks.AIR) && level.blockProtection.isBlockPlacementAllowed(blockPos))
        level.setBlockAndUpdate(blockPos, blockState)
}

fun placeBlockIfValid(level: Level, pos: Vec3, blockState: BlockState) {
    placeBlockIfValid(level, pos.toBlockPos(), blockState)
}
