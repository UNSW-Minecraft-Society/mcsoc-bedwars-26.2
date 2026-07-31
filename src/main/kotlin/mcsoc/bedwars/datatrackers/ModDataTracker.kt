package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.SavedData
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


@Serializable
private class PlayerDataRecord() : PlayerStateRecord, PlayerUpgradesRecord {
    companion object {
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerDataRecord::getLifeState),
                // this codec not tested
            Codec.unboundedMap(ToolCategory.CODEC, ToolTier.CODEC).fieldOf("tool_upgrades")
                    .forGetter(PlayerDataRecord::getToolUpgrades)
        ).apply(it, ::PlayerDataRecord)}
    }
    
    private var life_state: LifeState = LifeState.ALIVE
    private var toolUpgrades = HashMap<ToolCategory, ToolTier>()

    private constructor(
        life_state: LifeState,
        toolUpgrades: Map<ToolCategory, ToolTier>
    ) : this() {
        this.life_state = life_state
        this.toolUpgrades.putAll(toolUpgrades)
    }
    
    override fun getLifeState(): LifeState {
        return this.life_state
    }
    
    fun getToolUpgrades(): Map<ToolCategory, ToolTier> {
        return toolUpgrades
    }

    override fun getTool(tool: ToolCategory): ToolTier? {
        return toolUpgrades[tool]
    }

    override fun setTool(tool: ToolCategory, tier: ToolTier?) {
        if (tier == null) {
            toolUpgrades.remove(tool)
            return
        }
        toolUpgrades[tool] = tier
    }
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
    
    override fun getTool(player: ServerPlayer, tool: ToolCategory): ItemStack? = mod_data.getTool(player, tool)
    override fun upgradeTool(player: ServerPlayer, tool: ToolCategory) = mod_data.upgradeTool(player, tool)
    override fun downgradeTools(player: ServerPlayer) = mod_data.downgradeTools(player)
    override fun clearTools(player: ServerPlayer) = mod_data.clearTools(player)
}