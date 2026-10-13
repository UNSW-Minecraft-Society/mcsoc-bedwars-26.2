package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import java.util.UUID


private class ActivePlayerStore() {
    companion object {
        val CODEC: Codec<ActivePlayerStore> = RecordCodecBuilder.create{it.group(
            UUIDUtil.CODEC.listOf()
                .xmap({ l -> l.toMutableSet() }, { s -> s.toList() })
                .fieldOf("game_phase")
                .forGetter(ActivePlayerStore::active_players)
        ).apply(it, ::ActivePlayerStore)}
    }
    
    private val active_players = mutableSetOf<UUID>()
    
    private constructor(players: MutableSet<UUID>) : this() {
        this.active_players.addAll(players)
    }
    
    fun reset() {
        active_players.clear()
    }
    
    fun add(uuid: UUID) = active_players.add(uuid)
    fun remove(uuid: UUID) = active_players.remove(uuid)
    fun get() = active_players
}

fun ServerLevel.getActivePlayers(): Iterable<ServerPlayer> = this.activePlayers.get().mapNotNull(this.server.playerList::getPlayer)


class ActivePlayersTracker: LevelTiedData {
    companion object {
        val CODEC: MapCodec<ActivePlayersTracker> = RecordCodecBuilder.mapCodec{ it.group(
            ActivePlayerStore.CODEC.fieldOf("active_players_data").forGetter(ActivePlayersTracker::mod_data)
        ).apply(it, ::ActivePlayersTracker)}
    }
    override fun getType() = LevelDataType.TimeState

    private val mod_data: ActivePlayerStore
    private constructor(mod_data: ActivePlayerStore) {
        this.mod_data = mod_data
    }
    internal constructor() : this(ActivePlayerStore())
    fun reset() = mod_data.reset()
    
    fun get() = mod_data.get()
    fun add(uuid: UUID): Boolean {
        setDirty()
        return mod_data.add(uuid)
    }
    fun remove(uuid: UUID): Boolean {
        setDirty()
        return mod_data.remove(uuid)
    }
}