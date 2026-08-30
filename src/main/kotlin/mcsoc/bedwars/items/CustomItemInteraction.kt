package mcsoc.bedwars.items

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.vecToBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrowableProjectile
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.uuid.toKotlinUuid

const val CUSTOM_FIREBALL_VAL = "fireball"
const val CUSTOM_BRIDGE_EGG_VAL = "bridge_egg"
const val CUSTOM_POPUP_TOWER_VAL = "popup_tower"
const val CUSTOM_PLAYER_TRACKER_VAL = "player_tracker"

const val FIREBALL_SPEED = 1.0
const val BRIDGE_EGG_OFFSET = -1.8

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
            CUSTOM_FIREBALL_VAL -> return useFireballEffect(player, level, item)
            CUSTOM_PLAYER_TRACKER_VAL -> {}
        }
        return InteractionResult.PASS
    }

    private fun useFireballEffect(player: Player, level: Level, item: ItemStack): InteractionResult {
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
        if (projectile !is ThrowableItemProjectile) // required for casting to ThrowableItemProjectile to grab item data (which all custom projectiles are)
            return InteractionResult.PASS

        val type = projectile.item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString(CUSTOM_ITEM_TAG)?.getOrNull()
        BedwarsPlugin.LOGGER.info("Entity has $CUSTOM_ITEM_TAG $type")
        val team = ModDataTracker.getPlayersTeam(owner.uuid.toKotlinUuid())
        when (type) {
            CUSTOM_BRIDGE_EGG_VAL -> return tickBridgeEggEffect(level, projectile, team)
            CUSTOM_POPUP_TOWER_VAL -> {}
        }
        return InteractionResult.PASS
    }

    private fun placeBlockIfValid(level: Level, blockPos: BlockPos, blockState: BlockState) {
        val curBlockState = level.getBlockState(blockPos)
        if (curBlockState.`is`(Blocks.AIR))
            level.setBlockAndUpdate(blockPos, blockState)
    }

    private fun tickBridgeEggEffect(level: Level, egg: ThrowableItemProjectile, team: Team): InteractionResult {
        val bridgePos = egg.position().relative(Direction.DOWN, 2.0)
        val newBlockState = Blocks.WOOL.pick(team.dyeColour).defaultBlockState()
        placeBlockIfValid(level,vecToBlockPos(bridgePos.add(0.5, BRIDGE_EGG_OFFSET, 0.5)), newBlockState)
        placeBlockIfValid(level,vecToBlockPos(bridgePos.add(0.5, BRIDGE_EGG_OFFSET, -0.5)), newBlockState)
        placeBlockIfValid(level,vecToBlockPos(bridgePos.add(-0.5, BRIDGE_EGG_OFFSET, 0.5)), newBlockState)
        placeBlockIfValid(level,vecToBlockPos(bridgePos.add(-0.5, BRIDGE_EGG_OFFSET, -0.5)), newBlockState)
        return InteractionResult.SUCCESS
    }
}
