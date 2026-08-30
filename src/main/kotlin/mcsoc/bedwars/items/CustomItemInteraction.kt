package mcsoc.bedwars.items

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.ModDataTracker
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrowableProjectile
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

const val CUSTOM_FIREBALL_VAL = "fireball"
const val CUSTOM_BRIDGE_EGG_VAL = "bridge_egg"
const val CUSTOM_POPUP_TOWER_VAL = "popup_tower"
const val CUSTOM_PLAYER_TRACKER_VAL = "player_tracker"

const val FIREBALL_SPEED = 1.0

object CustomItemInteraction {
    fun triggerCustomItemEffect(player: Player, level: Level, hand: InteractionHand, hitResult: HitResult? = null): InteractionResult {
        val item = player.getItemInHand(hand)
        if (!ModDataTracker.isPlayerAlive(player))
            return InteractionResult.PASS
        if (!(item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.contains(CUSTOM_ITEM_TAG) ?: false))
            return InteractionResult.PASS
        if (level.isClientSide)
            return InteractionResult.PASS
        val type = item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString(CUSTOM_ITEM_TAG)?.get()
        BedwarsPlugin.LOGGER.info("Item has $CUSTOM_ITEM_TAG $type")
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
        val directionVector = player.getViewVector(1.0f)
        var fireball = LargeFireball(EntityTypes.FIREBALL, level)
        fireball.setPos(player.eyePosition.add(directionVector.scale(0.5)))
        fireball.owner = player
        fireball.deltaMovement = directionVector.scale(FIREBALL_SPEED)
        level.addFreshEntity(fireball)
        if (!player.isCreative) item.count -= 1
        return InteractionResult.SUCCESS
    }

    fun triggerCustomThrowableProjectileEffect(projectile: ThrowableProjectile): InteractionResult {
        val level = projectile.level()
        val owner = projectile.owner
        if (level.isClientSide)
            return InteractionResult.PASS
        if (owner !is Player)
            return InteractionResult.PASS
        val blockPos = projectile.blockPosition().relative(Direction.DOWN, 2)
        level.setBlockAndUpdate(blockPos, Blocks.WOOL.white.defaultBlockState())
        return InteractionResult.FAIL
    }
}
