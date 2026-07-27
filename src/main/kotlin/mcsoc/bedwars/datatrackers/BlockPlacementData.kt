package mcsoc.bedwars.datatrackers

import net.minecraft.core.BlockPos


internal interface BlockPlacementData {
    fun getIfBlockWasPlaced(pos: BlockPos): Boolean
    fun trackPlacedBlock(pos: BlockPos)
}

internal interface BlockProtectionTracker {
    fun getIfBlockIsProtected(pos: BlockPos): Boolean
    fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos)
}