package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.SavedData
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


@Serializable
private class PlayerDataRecord() : PlayerStateRecord, PlayerUpgradesRecord {
    companion object {
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerDataRecord::getLifeState)
        ).apply(it, ::PlayerDataRecord)}
    }
    
    private var life_state: LifeState = LifeState.ALIVE
    private var personalUpgrades = HashMap<ToolType, Int>()
    
    private constructor(
        life_state: LifeState
    ) : this() {
        this.life_state = life_state
    }
    
    override fun getLifeState(): LifeState {
        return this.life_state
    }
    
    override fun upgradeTool(tool: ToolType) {
        personalUpgrades[tool] = personalUpgrades.getOrDefault(tool, 1)
    }
    
    override fun ressetToolUpgrade(tool: ToolType) {
        personalUpgrades.remove(tool)
    }
    
    // default (0) means tool doesnt exist
    override fun getCurrentUpgrade(tool: ToolType) = personalUpgrades[tool] ?: 0
}


private class ModDataStore() : SavedData(), PlayerStateHolder, PlayerUpgradesHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
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

    override fun getToolUpgradeState(player: ServerPlayer): PlayerUpgradesRecord {
        return getPlayerData(player)
    }
}


object ModDataTracker : PlayerStateExposer, PlayerUpgradesExposer {
    private val mod_data = ModDataStore()
    
    override fun isPlayerAlive(player: Player): Boolean {
        return mod_data.isPlayerAlive(player)
    }
    override fun isPlayerRespawning(player: Player): Boolean {
        return mod_data.isPlayerRespawning(player)
    }
    override fun isPlayerDead(player: Player): Boolean {
        return mod_data.isPlayerDead(player)
    }

    override fun getTool(player: ServerPlayer, tool: ToolType): ItemStack? {
        return mod_data.getTool(player, tool)
    }
}