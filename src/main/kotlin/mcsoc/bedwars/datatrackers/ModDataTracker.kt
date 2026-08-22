package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid



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


private class ModDataStore() : SavedData(), PlayerStateHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(
                Codec.STRING
                    .xmap(Uuid::parse, Uuid::toString), 
                PlayerDataRecord.CODEC
            )
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map)
        ).apply(it, ::ModDataStore)}
    }
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    
    private constructor(map: Map<Uuid, PlayerDataRecord>): this() {
        this.player_data_map.putAll(map)
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
}


object ModDataTracker : PlayerStateExposer {
    private val mod_data = ModDataStore()
    
    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)
}