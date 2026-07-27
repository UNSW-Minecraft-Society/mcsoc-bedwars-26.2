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
class PlayerDataRecord() : PlayerStateRecord {
    companion object {
        val CODEC = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerDataRecord::getLifeState)
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


private interface ModDataHolder : PlayerStateTracker, BlockPlacementData, BlockProtectionTracker


private class ModDataStore() : ModDataHolder, SavedData() {
    companion object {
        val CODEC = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(
                Codec.STRING.xmap(Uuid::parse, Uuid::toString), 
                PlayerDataRecord.CODEC
            ).fieldOf("player_data_map")
            .forGetter(ModDataStore::getPlayerDataMap)
        ).apply(it, ::ModDataStore)}
    }    
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    private val placed_blocks_set = HashSet<BlockPos>()
    private val block_protection_zone_list = HashMap<Long, MutableList<AABB>>()
    
    private constructor(map: Map<Uuid, PlayerDataRecord>): this() {
        this.player_data_map.putAll(map)
    }
    
    private fun getPlayerDataMap(): Map<Uuid, PlayerDataRecord> {
        return player_data_map
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
}


object ModDataTracker {
    private val mod_data = ModDataStore()
    
    fun isPlayerAlive(player: Player): Boolean {
        return mod_data.isPlayerAlive(player)
    }
    fun isPlayerRespawning(player: Player): Boolean {
        return mod_data.isPlayerRespawning(player)
    }
    fun isPlayerDead(player: Player): Boolean {
        return mod_data.isPlayerDead(player)
    }
    
    @JvmStatic
    fun isBlockBreakAllowed(pos: BlockPos): Boolean {
        return !mod_data.getIfBlockIsProtected(pos) && mod_data.getIfBlockWasPlaced(pos)
    }
    @JvmStatic
    fun isBlockPlacementAllowed(pos: BlockPos): Boolean {
        return mod_data.getIfBlockIsProtected(pos)
    }
    
    @JvmStatic
    fun trackPlacedBlock(pos: BlockPos) {
        mod_data.trackPlacedBlock(pos)
    }
    fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) {
        mod_data.registerProtectionZone(corner1, corner2)
    }
}