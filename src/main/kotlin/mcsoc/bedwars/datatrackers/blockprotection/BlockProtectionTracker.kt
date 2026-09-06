package mcsoc.bedwars.datatrackers.blockprotection

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB


private class BlockProtectionStore(): BlockProtectionHolder {
    companion object {        
        val CODEC: Codec<BlockProtectionStore> = RecordCodecBuilder.create{it.group(
            Codec.list(BlockPos.CODEC)
                .xmap(List<BlockPos>::toSet, Set<BlockPos>::toList)
                .fieldOf("placed_blocks_set")
                .forGetter(BlockProtectionStore::placed_blocks_set)
        ).apply(it, ::BlockProtectionStore)}
    }
    
    private val placed_blocks_set = HashSet<BlockPos>()
    private val block_protection_zone_list = HashMap<Long, MutableList<ProtectionZone>>()
    
    private constructor(placed_blocks: Set<BlockPos>) : this() {
        this.placed_blocks_set.addAll(placed_blocks)
    }
    
    
    override fun getIfBlockWasPlaced(pos: BlockPos): Boolean {
        return placed_blocks_set.contains(pos)
    }
    override fun trackPlacedBlock(pos: BlockPos) {
        placed_blocks_set.add(pos)
    }
    
    override fun getIfBlockIsProtected(pos: BlockPos): Boolean {
        val chunk_key = ChunkPos.containing(pos).pack()
        return block_protection_zone_list[chunk_key]?.any{
            it.box.contains(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        } ?: false
    }
    override fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) {
        val to_box = ProtectionZone(AABB.of(BoundingBox.fromCorners(corner1, corner2)))
        
        val cpos1 = ChunkPos.containing(corner1)
        val cpos2 = ChunkPos.containing(corner2)
        
        for (x in minOf(cpos1.x, cpos2.x)..maxOf(cpos1.x, cpos2.x)) {
            for (z in minOf(cpos1.z, cpos2.z)..maxOf(cpos1.z, cpos2.z)) {
                val chunk_key = ChunkPos.pack(x, z)
                block_protection_zone_list.getOrPut(chunk_key){mutableListOf<ProtectionZone>()}.add(to_box)
            }
        }
    }
    
    override fun getProtectionZones(): Iterable<ProtectionZone> {
        return this.block_protection_zone_list.values.flatten()
    }
}


object BlockProtectionTracker : BlockProtectionExposer {
    private val protection_data = BlockProtectionStore()
    
    override fun isBlockBreakAllowed(pos: BlockPos): Boolean {
        return protection_data.isBlockBreakAllowed(pos)
    }
    override fun isBlockPlacementAllowed(pos: BlockPos): Boolean {
        return protection_data.isBlockPlacementAllowed(pos)
    }
    override fun trackPlacedBlock(pos: BlockPos) {
        protection_data.trackPlacedBlock(pos)
    }
    override fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) {
        protection_data.registerProtectionZone(corner1, corner2)
    }
    
    override fun getProtectionZones() = protection_data.getProtectionZones()
}