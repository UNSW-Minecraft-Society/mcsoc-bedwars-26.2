package mcsoc.bedwars.datatrackers.blockprotection

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.LevelDataType
import mcsoc.bedwars.datatrackers.LevelTiedData
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
                .forGetter(BlockProtectionStore::placed_blocks_set),
            Codec.unboundedMap(
                Codec.STRING
                    .xmap(String::toLong, Long::toString),
                Codec.list(ProtectionZone.CODEC)
                    .xmap(List<ProtectionZone>::toMutableSet, MutableSet<ProtectionZone>::toList)
            )
                .fieldOf("protection_zones_map")
                .forGetter(BlockProtectionStore::block_protection_zones)
        ).apply(it, ::BlockProtectionStore)}
    }
    
    private val placed_blocks_set = HashSet<BlockPos>()
    private val block_protection_zones = HashMap<Long, MutableSet<ProtectionZone>>()
    
    private constructor(placed_blocks: Set<BlockPos>, block_protection_zones: Map<Long, MutableSet<ProtectionZone>>) : this() {
        this.placed_blocks_set.addAll(placed_blocks)
        this.block_protection_zones.putAll(block_protection_zones)
    }
    
    override fun getIfBlockWasPlaced(pos: BlockPos): Boolean {
        return placed_blocks_set.contains(pos)
    }
    override fun trackPlacedBlock(pos: BlockPos) {
        placed_blocks_set.add(pos)
    }
    
    override fun getIfBlockIsProtected(pos: BlockPos): Boolean {
        val chunk_key = ChunkPos.containing(pos).pack()
        return block_protection_zones[chunk_key]?.any{
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
                block_protection_zones.getOrPut(chunk_key){mutableSetOf<ProtectionZone>()}.add(to_box)
            }
        }
    }
    
    override fun getProtectionZones(): Iterable<ProtectionZone> {
        return this.block_protection_zones.values.flatten().distinctBy(ProtectionZone::id)
    }
}


class BlockProtectionTracker : LevelTiedData, BlockProtectionExposer {
    companion object {
        val CODEC: MapCodec<BlockProtectionTracker> = RecordCodecBuilder.mapCodec{ it.group(
            BlockProtectionStore.CODEC.fieldOf("block_protection_data").forGetter(BlockProtectionTracker::protection_data)
        ).apply(it, ::BlockProtectionTracker)}
    }
    override val type get() = LevelDataType.BlockProtection
    
    private val protection_data: BlockProtectionStore
    private constructor(protection_data: BlockProtectionStore) {
        this.protection_data = protection_data
    }
    internal constructor() : this(BlockProtectionStore())
    
    override fun isBlockBreakAllowed(pos: BlockPos): Boolean = protection_data.isBlockBreakAllowed(pos)
    override fun isBlockPlacementAllowed(pos: BlockPos): Boolean = protection_data.isBlockPlacementAllowed(pos)
    override fun trackPlacedBlock(pos: BlockPos) {
        setDirty()
        protection_data.trackPlacedBlock(pos)
    }
    
    override fun getProtectionZones() = protection_data.getProtectionZones()
    override fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) {
        setDirty()
        protection_data.registerProtectionZone(corner1, corner2)
    }
}