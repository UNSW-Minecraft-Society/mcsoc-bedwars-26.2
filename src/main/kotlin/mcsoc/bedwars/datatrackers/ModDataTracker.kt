package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.generators.Generator
import net.minecraft.server.level.ServerLevel
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
    private constructor(
        life_state: LifeState
    ) : this() {
        this.life_state = life_state
    }
    
    override fun getLifeState(): LifeState {
        return this.life_state
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, GeneratorsHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(
                Codec.STRING
                    .xmap(Uuid::parse, Uuid::toString), 
                PlayerDataRecord.CODEC
            )
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),
            Codec.list(Generator.CODEC).fieldOf("generators").forGetter(ModDataStore::generators)
        ).apply(it, ::ModDataStore)}
    }    
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    private val generators = ArrayList<Generator>()
    
    private constructor(map: Map<Uuid, PlayerDataRecord>, gens: List<Generator>): this() {
        this.player_data_map.putAll(map)
        this.generators.addAll(gens)
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

    override fun addGenerator(gen: Generator) {
        generators.add(gen)
    }
    
    override fun getGenerators(): List<Generator> = generators

    override fun removeGenerator(gen: Generator) {
        generators.remove(gen)
    }
}


object ModDataTracker : PlayerStateExposer, GeneratorsExposer {
    private val mod_data = ModDataStore()
    
    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun addGenerator(type: String, location: Vec3) = mod_data.addGenerator(type, location)
    override fun removeGenerator(location: Vec3) = mod_data.removeGenerator(location)
    override fun tickGenerators(level: ServerLevel) = mod_data.tickGenerators(level)
    override fun upgradeGeneratorTier() = mod_data.upgradeGeneratorTier()
}