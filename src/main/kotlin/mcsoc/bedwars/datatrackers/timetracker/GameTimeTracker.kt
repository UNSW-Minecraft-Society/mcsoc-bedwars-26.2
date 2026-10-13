package mcsoc.bedwars.datatrackers.timetracker

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.LevelDataType
import mcsoc.bedwars.datatrackers.LevelTiedData
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import net.minecraft.util.StringRepresentable
import kotlin.time.Duration


enum class GamePhase : StringRepresentable {
    STARTING,
    ACTIVE,
    ENDED,
    INACTIVE;
    
    companion object {
        val CODEC: Codec<GamePhase> = StringRepresentable.fromEnum(GamePhase::values)
    }

    override fun getSerializedName(): String = this.name
}

enum class GamePeriod(val next: GamePeriod?, val title: String, val notif: String) : StringRepresentable {
    INACTIVE(null, "Inactive", ""),
    TERMINAL(null, "Game End", "Game has ended."),
    DEATHMATCH(TERMINAL, "Deathmatch", "Deathmatch has begun."),
    EMERALD_III(DEATHMATCH, "Emerald Generator III", "Emerald generators have been upgraded."),
    DIAMOND_III(EMERALD_III, "Diamond Generator III", "Diamond generators have been upgraded."),
    EMERALD_II(DIAMOND_III, "Emerald Generator II", "Emerald generators have been upgraded."),
    DIAMOND_II(EMERALD_II, "Diamond Generator II", "Diamond generators have been upgraded."),
    INITIAL(DIAMOND_II, "Game Start", "Game has begun.");
    
    val startTime: Duration? 
        get() = BedwarsConfigData.generator_times[this]
    
    companion object {
        val CODEC: Codec<GamePeriod> = StringRepresentable.fromEnum(GamePeriod::values)
    }

    override fun getSerializedName(): String = this.name
}


private class TimeStore() {
    companion object {
        val CODEC: Codec<TimeStore> = RecordCodecBuilder.create{it.group(
            GamePeriod.CODEC
                .fieldOf("game_period")
                .forGetter(TimeStore::game_period),
            GamePhase.CODEC
                .fieldOf("game_phase")
                .forGetter(TimeStore::game_phase)
        ).apply(it, ::TimeStore)}
    }

    private var game_period = GamePeriod.INACTIVE
    private var game_phase = GamePhase.INACTIVE
    
    private constructor(game_period: GamePeriod, game_phase: GamePhase) : this() {
        this.game_phase = game_phase
        this.game_period = game_period
    }

    fun getGamePhase(): GamePhase {
        return game_phase
    }

    fun setGamePhase(phase: GamePhase) {
        game_phase = phase
    }

    fun getGamePeriod(): GamePeriod {
        return game_period
    }

    fun setGamePeriod(period: GamePeriod) {
        game_period = period
    }
    
    fun resetModData() {
        game_phase = GamePhase.INACTIVE
        game_period = GamePeriod.INACTIVE
    }
}

class GameTimeTracker: LevelTiedData {
    companion object {
        val CODEC: MapCodec<GameTimeTracker> = RecordCodecBuilder.mapCodec{ it.group(
            TimeStore.CODEC.fieldOf("mod_data").forGetter(GameTimeTracker::mod_data)
        ).apply(it, ::GameTimeTracker)}
    }
    override fun getType() = LevelDataType.TimeState

    private val mod_data: TimeStore
    private constructor(mod_data: TimeStore) {
        this.mod_data = mod_data
    }
    internal constructor() : this(TimeStore())
    fun resetModData() = mod_data.resetModData()
    
    fun getGamePhase(): GamePhase = mod_data.getGamePhase()
    fun setGamePhase(phase: GamePhase) {
        setDirty()
        mod_data.setGamePhase(phase)
    }
    fun getGamePeriod(): GamePeriod = mod_data.getGamePeriod()
    fun setGamePeriod(period: GamePeriod) {
        setDirty()
        mod_data.setGamePeriod(period)
    }
}