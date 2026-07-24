package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
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


private interface ModDataHolder : PlayerStateTracker


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
}


object ModDataTracker : ModDataHolder {
    private val mod_data = ModDataStore()
    
    override fun getPlayerState(player: Player): PlayerStateRecord {
        return mod_data.getPlayerState(player)
    }
}