package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.utils.Colour
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
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
    private var team: Colour = Colour.NONE // TODO add team to codec

    private constructor(
        life_state: LifeState
    ) : this() {
        this.life_state = life_state
    }

    override fun getLifeState(): LifeState {
        return this.life_state
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, TeamStateHolder {
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
    private val teams_map = HashMap<Colour, TeamDataRecord>() // TODO add team_map to codec

    private constructor(map: Map<Uuid, PlayerDataRecord>) : this() {
        this.player_data_map.putAll(map)
    }

    private fun getPlayerData(id: Uuid): PlayerDataRecord {
        return player_data_map.getOrPut(id) { PlayerDataRecord() }
    }

    private fun getPlayerData(player: Player): PlayerDataRecord {
        return getPlayerData(player.uuid.toKotlinUuid())
    }

    override fun getPlayerState(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }

    override fun getTeam(colour: Colour): TeamDataRecord {
        return teams_map[colour] ?: throw Exception("Invalid team colour")
    }

    override fun createTeams(players: List<ServerPlayer>, numTeams: Int) {
        val teams = Colour.entries.take(numTeams)

        teams.forEach {
            // todo replace location and max people per team with config of some sorts
            teams_map[it] = TeamDataRecord(it, mutableListOf(), true, Vec3(0.0, 0.0, 0.0), 4)
        }

        players.forEachIndexed { i, player ->
            addPlayerToTeam(player, teams[i % numTeams])
        }
    }
}


object ModDataTracker : PlayerStateExposer, TeamStateExposer {
    private val mod_data = ModDataStore()

    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun getBedDestroyed(colour: Colour): Boolean = mod_data.getBedDestroyed(colour)
    override fun getPlayersInTeam(colour: Colour): List<ServerPlayer> = mod_data.getPlayersInTeam(colour)
    override fun getTeamSpawn(colour: Colour): Vec3 = mod_data.getTeamSpawn(colour)
    override fun addPlayerToTeam(player: ServerPlayer, team: Colour) = mod_data.addPlayerToTeam(player, team)
    override fun destroyBed(colour: Colour) = mod_data.destroyBed(colour)
    override fun createTeams(players: List<ServerPlayer>, numTeams: Int) = mod_data.createTeams(players, numTeams)
}