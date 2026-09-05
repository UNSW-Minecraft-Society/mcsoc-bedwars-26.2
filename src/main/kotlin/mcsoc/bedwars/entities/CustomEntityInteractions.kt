package mcsoc.bedwars.entities

import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.customEntityData
import mcsoc.bedwars.gui.ShopGui
import mcsoc.bedwars.gui.ShopType
import mcsoc.bedwars.upgrades.UpgradableItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

enum class CustomEntityType {
    PLAYER_SHOPKEEPER,
    TEAM_SHOPKEEPER,
}

object CustomEntityInteractions {
    fun triggerShopkeeperOpen(player: Player, level: Level, hand: InteractionHand, entity: Entity): InteractionResult {
        if (level !is ServerLevel || level.isClientSide)
            return InteractionResult.PASS
        if (player !is ServerPlayer || !level.gameState.isPlayerAlive(player))
            return InteractionResult.PASS
        if (!level.customEntityData.containsKey(entity.uuid))
            return InteractionResult.PASS
        val type = level.customEntityData[entity.uuid]!!
        when (type) {
            CustomEntityType.PLAYER_SHOPKEEPER -> ShopGui.displayShop(player, ShopType.PLAYER_SHOP)
            CustomEntityType.TEAM_SHOPKEEPER -> ShopGui.displayShop(player, ShopType.TEAM_SHOP)
        }
        return InteractionResult.SUCCESS
    }
}