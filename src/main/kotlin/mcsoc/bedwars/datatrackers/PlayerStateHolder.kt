package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

internal sealed class LifeState(val id: Byte) {
    companion object {
        fun fromId(id: Byte): LifeState {
            return when (id.toInt()) {
                0 -> ALIVE
                1 -> RESPAWNING
                2 -> DEAD(Vec3.ZERO)
                3 -> ELIMINATED
                else -> ALIVE
            }
        }
        
        val CODEC: Codec<LifeState> = RecordCodecBuilder.create{it.group(
            Codec.BYTE.fieldOf("life_state").forGetter(LifeState::id)
        ).apply(it, ::fromId)}
    }
    
    object ALIVE : LifeState(0)
    object RESPAWNING : LifeState(1)
    class DEAD(val death_position: Vec3) : LifeState(2)
    object ELIMINATED : LifeState(3)
}


internal interface PlayerStateRecord {
    fun getLifeState(): LifeState
    fun setLifeState(new_state: LifeState)

    fun getDeathPosition(): Vec3
}

internal interface PlayerStateExposer {
    fun isPlayerAlive(player: Player): Boolean
    fun isPlayerRespawning(player: Player): Boolean
    fun getPlayerDeathPosition(player: Player): Vec3
    fun isPlayerEliminated(player: Player): Boolean

    fun setPlayerAlive(player: Player)
    fun setPlayerRespawning(player: Player)
    fun setPlayerDead(player: Player, position: Vec3)
    fun setPlayerEliminated(player: Player)
}

internal interface PlayerStateHolder : PlayerStateExposer {
    fun getPlayerState(player: Player): PlayerStateRecord
    
    override fun isPlayerAlive(player: Player): Boolean {
        val player_state = getPlayerState(player)
        return player_state.getLifeState() is LifeState.ALIVE
    }
    override fun isPlayerRespawning(player: Player): Boolean {
        val player_state = getPlayerState(player)
        return player_state.getLifeState() is LifeState.RESPAWNING
    }

    override fun getPlayerDeathPosition(player: Player): Vec3 {
        val player_state = getPlayerState(player)
        return player_state.getDeathPosition()
    }

    override fun isPlayerEliminated(player: Player): Boolean {
        val player_state = getPlayerState(player)
        return player_state.getLifeState() is LifeState.ELIMINATED
    }

    override fun setPlayerAlive(player: Player) {
        val player_state = getPlayerState(player)
        player_state.setLifeState(LifeState.ALIVE)
    }

    override fun setPlayerRespawning(player: Player) {
        val player_state = getPlayerState(player)
        player_state.setLifeState(LifeState.ALIVE)
    }

    override fun setPlayerDead(player: Player, position: Vec3) {
        val player_state = getPlayerState(player)
        player_state.setLifeState(LifeState.DEAD(position))
    }

    override fun setPlayerEliminated(player: Player) {
        val player_state = getPlayerState(player)
        player_state.setLifeState(LifeState.ELIMINATED)
    }

}
