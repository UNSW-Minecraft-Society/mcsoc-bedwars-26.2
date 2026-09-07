package mcsoc.bedwars.datatrackers.blockprotection

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.utils.AABB_CODEC
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import java.util.UUID


data class ProtectionZone(val box: AABB) {
    companion object {
        val CODEC: Codec<ProtectionZone> = RecordCodecBuilder.create{ inst -> inst.group(
            AABB_CODEC.fieldOf("box").forGetter(ProtectionZone::box)
        ).apply(inst, ::ProtectionZone)}
    }
    val id: UUID = UUID.randomUUID() 
}

internal interface BlockPlacementHolder {
    fun getIfBlockWasPlaced(pos: BlockPos): Boolean 
}
internal interface BlockProtectionZoneHolder {
    fun getIfBlockIsProtected(pos: BlockPos): Boolean
}

internal interface BlockProtectionExposer {
    var protectionEnabled: Boolean
    
    fun isBlockBreakAllowed(pos: BlockPos): Boolean
    fun isBlockPlacementAllowed(pos: BlockPos): Boolean
    
    fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos)
    fun getProtectionZones(): Iterable<ProtectionZone>
    fun trackPlacedBlock(pos: BlockPos)
}
internal interface BlockProtectionHolder : BlockProtectionExposer, BlockPlacementHolder, BlockProtectionZoneHolder {
    override fun isBlockBreakAllowed(pos: BlockPos): Boolean {
        return !protectionEnabled || (!getIfBlockIsProtected(pos) && getIfBlockWasPlaced(pos))
    }
    override fun isBlockPlacementAllowed(pos: BlockPos): Boolean {
        return !(protectionEnabled && getIfBlockIsProtected(pos))
    }
}


