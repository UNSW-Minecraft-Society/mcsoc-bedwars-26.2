package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.AABB
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


@Serializable
private class PlayerDataRecord() : PlayerStateRecord {
    companion object {
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC
                .fieldOf("life_state")
                .forGetter(PlayerDataRecord::life_state)
        ).apply(it, ::PlayerDataRecord)}
    }
    
    private var life_state: LifeState = LifeState.ALIVE
    private constructor(
        life_state: LifeState
    ) : this() {
        this.life_state = life_state
    }
    
    override fun getLifeState(): LifeState {
        return this.life_state
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, BlockProtectionHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(
                Codec.STRING
                    .xmap(Uuid::parse, Uuid::toString),
                PlayerDataRecord.CODEC
            )
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),
            Codec.list(BlockPos.CODEC)
                .xmap(List<BlockPos>::toSet, Set<BlockPos>::toList)
                .fieldOf("placed_blocks_set")
                .forGetter(ModDataStore::placed_blocks_set)
        ).apply(it, ::ModDataStore)}
    }
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    private val placed_blocks_set = HashSet<BlockPos>()
    private val block_protection_zone_list = HashMap<Long, MutableList<AABB>>()
    
    private constructor(
        player_data: Map<Uuid, PlayerDataRecord>,
        placed_blocks: Set<BlockPos>,
    ): this() {
        placed_blocks.toList().toSet()
        this.player_data_map.putAll(player_data)
        this.placed_blocks_set.addAll(placed_blocks)
    }
    
    private fun getPlayerData(id: Uuid): PlayerDataRecord {
        return player_data_map.getOrPut(id){PlayerDataRecord()}
    }
    private fun getPlayerData(player: Player): PlayerDataRecord {
        return getPlayerData(player.uuid.toKotlinUuid())
    }
    
    
    override fun getPlayerState(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }
    
    
    override fun getIfBlockWasPlaced(pos: BlockPos): Boolean {
        return placed_blocks_set.contains(pos)
    }
    override fun trackPlacedBlock(pos: BlockPos) {
        placed_blocks_set.add(pos)
        setDirty()
    }
    
    override fun getIfBlockIsProtected(pos: BlockPos): Boolean {
        val chunk_key = ChunkPos.containing(pos).pack()
        return block_protection_zone_list[chunk_key]?.any{
            it.contains(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        } ?: false
    }
    override fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) {
        val to_box = AABB.of(BoundingBox.fromCorners(corner1, corner2))
        
        val cpos1 = ChunkPos.containing(corner1)
        val cpos2 = ChunkPos.containing(corner2)
        
        for (x in minOf(cpos1.x, cpos2.x)..maxOf(cpos1.x, cpos2.x)) {
            for (z in minOf(cpos1.z, cpos2.z)..maxOf(cpos1.z, cpos2.z)) {
                val chunk_key = ChunkPos.pack(x, z)
                block_protection_zone_list.getOrPut(chunk_key){mutableListOf<AABB>()}.add(to_box)
            }
        }

        setDirty()
    }
    
    override fun getProtectionZones(): Iterable<AABB> {
        val toReturn = HashSet<AABB>()
        this.block_protection_zone_list.values.forEach(toReturn::addAll)
        return toReturn
    }
}


object ModDataTracker : PlayerStateExposer, BlockProtectionExposer {
    private val mod_data = ModDataStore()
    
    override fun isPlayerAlive(player: Player): Boolean {
        return mod_data.isPlayerAlive(player)
    }
    override fun isPlayerRespawning(player: Player): Boolean {
        return mod_data.isPlayerRespawning(player)
    }
    override fun isPlayerDead(player: Player): Boolean {
        return mod_data.isPlayerDead(player)
    }
    
    override fun isBlockBreakAllowed(pos: BlockPos): Boolean {
        return mod_data.isBlockBreakAllowed(pos)
    }
    override fun isBlockPlacementAllowed(pos: BlockPos): Boolean {
        return mod_data.isBlockPlacementAllowed(pos)
    }
    override fun trackPlacedBlock(pos: BlockPos) {
        mod_data.trackPlacedBlock(pos)
    }
    override fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) {
        mod_data.registerProtectionZone(corner1, corner2)
    }
    
    override fun getProtectionZones() = mod_data.getProtectionZones()
}