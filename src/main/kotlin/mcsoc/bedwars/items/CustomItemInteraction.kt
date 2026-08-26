package mcsoc.bedwars.items

import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

const val CUSTOM_FIREBALL_VAL = "fireball"
const val CUSTOM_BRIDGE_EGG_VAL = "bridge_egg"
const val CUSTOM_POPUP_TOWER_VAL = "popup_tower"
const val CUSTOM_PLAYER_TRACKER_VAL = "player_tracker"

object CustomItemInteraction {
    fun triggerCustomItemEffect(player: Player, level: Level, item: ItemStack, type: String?): InteractionResult {
        when (type) {
            CUSTOM_FIREBALL_VAL -> useFireballEffect(player, level, item)
            CUSTOM_BRIDGE_EGG_VAL -> {}
            CUSTOM_POPUP_TOWER_VAL -> {}
            CUSTOM_PLAYER_TRACKER_VAL -> {}
        }
        return InteractionResult.FAIL
    }

    fun useFireballEffect(player: Player, level: Level, item: ItemStack): InteractionResult {
        BedwarsPlugin.LOGGER.info("Doing fireball thing")
        val direction = player.direction
        var fireball = LargeFireball(EntityTypes.FIREBALL, level)
        fireball.setPos(player.position().relative(direction, 0.5))
        fireball.owner = player
        fireball.deltaMovement = direction.unitVec3
        level.addFreshEntity(fireball)
        return InteractionResult.SUCCESS
    }
}