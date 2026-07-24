package mcsoc.bedwars.datatrackers

import net.minecraft.core.BlockPos


interface BlockPlacementData {
    fun getIfBlockWasPlaced(pos: BlockPos): Boolean
    fun trackPlacedBlock(pos: BlockPos)
}

interface BlockProtectionTracker {
    fun getIfBlockIsProtected(pos: BlockPos): Boolean
    fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos)
}