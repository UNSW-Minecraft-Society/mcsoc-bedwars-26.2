package mcsoc.bedwars.datatrackers.blockprotection

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import java.util.UUID


data class ProtectionZone(val box: AABB) {
    val id: UUID = UUID.randomUUID() 
}

internal interface BlockPlacementHolder {
    fun getIfBlockWasPlaced(pos: BlockPos): Boolean 
}
internal interface BlockProtectionZoneHolder {
    fun getIfBlockIsProtected(pos: BlockPos): Boolean
}

internal interface BlockProtectionExposer {
    fun isBlockBreakAllowed(pos: BlockPos): Boolean
    fun isBlockPlacementAllowed(pos: BlockPos): Boolean
    
    fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos)
    fun getProtectionZones(): Iterable<ProtectionZone>
    fun trackPlacedBlock(pos: BlockPos)
}
internal interface BlockProtectionHolder : BlockProtectionExposer, BlockPlacementHolder, BlockProtectionZoneHolder {
    override fun isBlockBreakAllowed(pos: BlockPos): Boolean {
        return !getIfBlockIsProtected(pos) && getIfBlockWasPlaced(pos)
    }
    override fun isBlockPlacementAllowed(pos: BlockPos): Boolean {
        return !getIfBlockIsProtected(pos)
    }
}


