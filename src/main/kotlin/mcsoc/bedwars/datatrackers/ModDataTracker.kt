package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid


private class PlayerDataRecord() : PlayerStateRecord, PlayerTeamState {
    companion object {
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerDataRecord::life_state),
            Team.CODEC.fieldOf("team").forGetter(PlayerDataRecord::team)
        ).apply(it, ::PlayerDataRecord)}
    }

    private var life_state: LifeState = LifeState.ALIVE
    private var team: Team = Team.NONE

    private constructor(
        life_state: LifeState,
        team: Team
    ) : this() {
        this.life_state = life_state
        this.team = Team.NONE
    }

    override fun getLifeState(): LifeState {
        return this.life_state
    }

    override fun getTeamName(): Team = team

    override fun setTeamName(team: Team) {
        this.team = team
    }
}


private class TeamDataRecord(
    private val players: MutableList<Uuid> = mutableListOf(),
    private var bedAlive: Boolean = true,
    private val spawn: Vec3 = Vec3(0.0, 0.0, 0.0),
) : TeamStateRecord {
    companion object {
        val UUID_LIST_CODEC: Codec<MutableList<Uuid>> = UUIDUtil.CODEC.listOf().xmap(
            { it.map(UUID::toKotlinUuid).toMutableList() },
            { it.map(Uuid::toJavaUuid) }
        )
        
        val CODEC: Codec<TeamDataRecord> = RecordCodecBuilder.create { it.group(
            UUID_LIST_CODEC.fieldOf("players").forGetter(TeamDataRecord::players),
            Codec.BOOL.fieldOf("bed_alive").forGetter(TeamDataRecord::bedAlive),
            Vec3.CODEC.fieldOf("spawn").forGetter(TeamDataRecord::spawn),
        ).apply(it, ::TeamDataRecord)}
    }

    override fun getBedAlive(): Boolean = bedAlive
    override fun getSpawn(): Vec3 = spawn
    override fun getPlayers(): MutableList<Uuid> = players

    override fun setBedAlive(bedAlive: Boolean) {
        this.bedAlive = bedAlive
    }

    override fun addPlayer(player: Uuid) {
        players.add(player)
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, BlockProtectionHolder, TeamStateHolder {
    companion object {
        val UUIDCodec: Codec<Uuid> = Codec.STRING.xmap(Uuid::parse, Uuid::toString)
        
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDCodec, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),
            
            Codec.list(BlockPos.CODEC)
                .xmap(List<BlockPos>::toSet, Set<BlockPos>::toList)
                .fieldOf("placed_blocks_set")
                .forGetter(ModDataStore::placed_blocks_set),
            
            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(ModDataStore::teams_map)
        ).apply(it, ::ModDataStore)}
    }
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    private val placed_blocks_set = HashSet<BlockPos>()
    private val block_protection_zone_list = HashMap<Long, MutableList<AABB>>()
    private val teams_map = HashMap<Team, TeamDataRecord>()
    private val active_players = mutableSetOf<UUID>()
    
    private constructor(
        player_data: Map<Uuid, PlayerDataRecord>,
        placed_blocks: Set<BlockPos>,
        teamMap: Map<Team, TeamDataRecord>
    ): this() {
        placed_blocks.toList().toSet()
        this.player_data_map.putAll(player_data)
        this.placed_blocks_set.addAll(placed_blocks)
        this.teams_map.putAll(teamMap)
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
    
    override fun getProtectionZones(): Iterable<AABB> {
        val toReturn = HashSet<AABB>()
        this.block_protection_zone_list.values.forEach(toReturn::addAll)
        return toReturn
    }
    

    override fun getTeam(team: Team): TeamDataRecord {
        return teams_map[team] ?: throw Exception("Invalid team")
    }

    override fun getActiveTeams(): List<Team> = teams_map.keys.toList()

    // to be updated by map loader
    // this will reset team data
    override fun initialiseTeams(numTeams: Int) {
        assert(numTeams < Team.entries.size) { "More teams specified than can be handled" }
        teams_map.clear()
        
        val teams = Team.entries.take(numTeams)
        teams.forEach { teams_map[it] = TeamDataRecord() }
    }

    override fun addPlayer(player: Uuid, team: Team) {
        getTeam(team).addPlayer(player)
        getPlayerData(player).setTeamName(team)
    }
    
    override fun getPlayersTeam(player: Uuid): Team = getPlayerData(player).getTeamName()
    
    fun addActivePlayer(uuid: UUID) = active_players.add(uuid)
    fun removeActivePlayer(uuid: UUID) = active_players.remove(uuid)
    fun getActivePlayers() = active_players
}


object ModDataTracker : PlayerStateExposer, BlockProtectionExposer, TeamStateExposer {
    private val mod_data = ModDataStore()
    
    override fun isPlayerAlive(player: Player) =  mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun isBlockBreakAllowed(pos: BlockPos) = mod_data.isBlockBreakAllowed(pos)
    override fun isBlockPlacementAllowed(pos: BlockPos) = mod_data.isBlockPlacementAllowed(pos)
    override fun trackPlacedBlock(pos: BlockPos) = mod_data.trackPlacedBlock(pos)
    override fun registerProtectionZone(corner1: BlockPos, corner2: BlockPos) = mod_data.registerProtectionZone(corner1, corner2)
    
    override fun getProtectionZones() = mod_data.getProtectionZones()

    override fun getBedDestroyed(team: Team): Boolean = mod_data.getBedDestroyed(team)
    override fun getPlayersInTeam(team: Team): List<Uuid> = mod_data.getPlayersInTeam(team)
    override fun getTeamSpawn(team: Team): Vec3 = mod_data.getTeamSpawn(team)
    override fun getActiveTeams(): List<Team> = mod_data.getActiveTeams()
    override fun setBedAlive(team: Team, state: Boolean) = mod_data.setBedAlive(team, state)
    override fun initialiseTeams(numTeams: Int) = mod_data.initialiseTeams(numTeams)
    override fun addPlayer(player: Uuid, team: Team) = mod_data.addPlayer(player, team)
    
    override fun getPlayersTeam(player: Uuid): Team = mod_data.getPlayersTeam(player)
    fun getActivePlayers() = mod_data.getActivePlayers()
    fun addActivePlayer(uuid: UUID) = mod_data.addActivePlayer(uuid)
    fun removeActivePlayer(uuid: UUID) = mod_data.removeActivePlayer(uuid)
}