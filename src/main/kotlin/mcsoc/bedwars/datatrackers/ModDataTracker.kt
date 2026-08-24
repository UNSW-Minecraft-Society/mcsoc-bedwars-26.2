package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.upgrades.UpgradableItem
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.SavedData
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


@Serializable
private class PlayerDataRecord() : PlayerStateRecord, PlayerUpgradesRecord {
    companion object {
        val TOOL_UPGRADES_CODEC: Codec<HashMap<UpgradeItemType, UpgradableItem>> =
            Codec.unboundedMap(UpgradeItemType.CODEC, Codec.STRING).xmap(
                    { HashMap(it.mapValues { (type, tier) -> type.fromName(tier) }) },
                    { it.mapValues { (_, item) -> (item as Enum<*>).name } }
                )
    
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC
                .fieldOf("life_state")
                .forGetter(PlayerDataRecord::life_state),
            TOOL_UPGRADES_CODEC
                .fieldOf("player_upgrades")
                .forGetter(PlayerDataRecord::toolUpgrades)
        ).apply(it, ::PlayerDataRecord)}
    }
    
    private var life_state: LifeState = LifeState.ALIVE
    private var toolUpgrades = HashMap<UpgradeItemType, UpgradableItem>()

    private constructor(
        life_state: LifeState,
        toolUpgrades: Map<UpgradeItemType, UpgradableItem>
    ) : this() {
        this.life_state = life_state
        this.toolUpgrades.putAll(toolUpgrades)
    }
    
    override fun getLifeState(): LifeState {
        return this.life_state
    }

    override fun getItem(item: UpgradeItemType): UpgradableItem {
        return toolUpgrades.getOrPut(item) { item.default }
    }

    override fun setItem(item: UpgradableItem) {
        toolUpgrades[item.type] = item
    }

    override fun removeItem(item: UpgradeItemType) {
        toolUpgrades.remove(item)
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, PlayerUpgradesHolder {
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
    
    private constructor(map: Map<Uuid, PlayerDataRecord>): this() {
        this.player_data_map.putAll(map)
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

    override fun getItemUpgradeState(player: Player): PlayerUpgradesRecord {
        return getPlayerData(player)
    }
}


object ModDataTracker : PlayerStateExposer, PlayerUpgradesExposer {
    private val mod_data = ModDataStore()
    
    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun upgradeItem(player: ServerPlayer, item: UpgradeItemType) = mod_data.upgradeItem(player, item)
    override fun downgradeItems(player: ServerPlayer) = mod_data.downgradeItems(player)
    override fun clearItems(player: ServerPlayer) = mod_data.clearItems(player)

    override fun getNextItemStack(player: ServerPlayer, item: UpgradeItemType) = mod_data.getNextItemStack(player, item)
    override fun getTier(player: ServerPlayer, item: UpgradeItemType) = mod_data.getTier(player, item)
}