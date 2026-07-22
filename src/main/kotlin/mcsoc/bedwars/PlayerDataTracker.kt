package mcsoc.bedwars

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID
import kotlin.uuid.toKotlinUuid

private sealed class LifeState(val id: Byte) {
    companion object {
        fun fromId(id: Byte): LifeState {
            return when (id.toInt()) {
                0 -> ALIVE
                1 -> RESPAWNING
                2 -> DEAD
                else -> ALIVE
            }
        }
        
        val CODEC = RecordCodecBuilder.create{it.group(
            Codec.BYTE.fieldOf("hello").forGetter(LifeState::id)
        ).apply(it, ::fromId)}
    }
    
    object ALIVE : LifeState(0)
    object RESPAWNING : LifeState(1)
    object DEAD : LifeState(2)
}

@Serializable
private data class PlayerData(val life_state: LifeState = LifeState.ALIVE) {
    companion object {
        val CODEC = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerData::life_state)
        ).apply(it, ::PlayerData)}
    }
}


class PlayerDataTracker() {
    companion object {
        val CODEC = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(Codec.STRING.xmap(Uuid::parse, Uuid::toString), PlayerData.CODEC).fieldOf("player_data_map").forGetter(PlayerDataTracker::getPlayerDataMap)
        ).apply(it, ::PlayerDataTracker)}
    }
    private val player_data_map = HashMap<Uuid, PlayerData>()
    
    private constructor(map: Map<Uuid, PlayerData>): this() {
        this.player_data_map.putAll(map)
    }
    
    private fun getPlayerDataMap(): Map<Uuid, PlayerData> {
        return player_data_map
    }
    
    private fun getPlayerData(id: Uuid): PlayerData {
        return player_data_map[id] ?: run{
            player_data_map[id] = PlayerData()
            PlayerData()
        }
    }
    
    
}
