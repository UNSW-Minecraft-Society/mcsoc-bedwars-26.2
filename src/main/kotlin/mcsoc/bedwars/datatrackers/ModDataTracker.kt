package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.utils.inWholeTicks
import mcsoc.bedwars.utils.ticks
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
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


private class ModDataStore() : SavedData(), PlayerStateHolder, Ticker {
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
    private var prev_tick_time = Clock.System.now()
    private var tick_delta = Duration.ZERO
    private var game_timer = Duration.ZERO
    private var timer_tick = false
    
    private constructor(map: Map<Uuid, PlayerDataRecord>): this() {
        this.player_data_map.putAll(map)
    }
    
    
    override fun tick() {
        
        timer_tick = game_timer.inWholeTicks != (game_timer + tick_delta).inWholeTicks
        game_timer += tick_delta
    }

    override fun getGameTime() = game_timer
        
    override fun getTimerTick() = timer_tick

    
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


object ModDataTracker : PlayerStateExposer, TickExposer {
    private val mod_data = ModDataStore()

    override fun tick() = mod_data.tick()
    override fun getGameTime() : Duration {
        return mod_data.getGameTime()
    }
    
    override fun getTimerTick(): Boolean {
        return mod_data.getTimerTick()
    }
    
    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)
}