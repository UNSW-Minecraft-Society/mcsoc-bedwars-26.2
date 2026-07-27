package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.entity.player.Player

internal sealed class LifeState(val id: Byte) {
    companion object {
        fun fromId(id: Byte): LifeState {
            return when (id.toInt()) {
                0 -> ALIVE
                1 -> RESPAWNING
                2 -> DEAD
                else -> ALIVE
            }
        }
        
        val CODEC: Codec<LifeState> = RecordCodecBuilder.create{it.group(
            Codec.BYTE.fieldOf("life_state").forGetter(LifeState::id)
        ).apply(it, ::fromId)}
    }
    
    object ALIVE : LifeState(0)
    object RESPAWNING : LifeState(1)
    object DEAD : LifeState(2)
}


internal interface PlayerStateRecord {
    fun getLifeState(): LifeState
}

internal interface PlayerStateExposer {
    fun isPlayerAlive(player: Player): Boolean
    fun isPlayerRespawning(player: Player): Boolean
    fun isPlayerDead(player: Player): Boolean
}

internal interface PlayerStateHolder : PlayerStateExposer {
    fun getPlayerState(player: Player): PlayerStateRecord
    
    override fun isPlayerAlive(player: Player): Boolean {
        val player_state = getPlayerState(player)
        return player_state.getLifeState() == LifeState.ALIVE
    }
    override fun isPlayerRespawning(player: Player): Boolean {
        val player_state = getPlayerState(player)
        return player_state.getLifeState() == LifeState.RESPAWNING
    }
    override fun isPlayerDead(player: Player): Boolean {
        val player_state = getPlayerState(player)
        return player_state.getLifeState() == LifeState.DEAD
    }
}
