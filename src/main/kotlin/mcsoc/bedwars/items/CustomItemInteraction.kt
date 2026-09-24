package mcsoc.bedwars.items

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.entities.spawnBedBrute
import mcsoc.bedwars.entities.spawnBedBug
import mcsoc.bedwars.entities.spawnDreamDefender
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.pitchDeg
import mcsoc.bedwars.utils.placeBlockIfValid
import mcsoc.bedwars.utils.toCardinalDirection
import mcsoc.bedwars.utils.toBlockPos
import mcsoc.bedwars.utils.yawDeg
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.commands.TeleportCommand
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.LodestoneTracker
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.HitResult
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt


const val FIREBALL_SPEED = 1.0
const val FIREBALL_POWER = 3
const val BRIDGE_EGG_OFFSET = -0.5
const val PLAYER_TRACKER_RANGE = 20
const val PLAYER_TRACKER_DURATION = 60 // in ticks

object CustomItemInteraction {
    fun triggerCustomItemEffect(player: Player, level: Level, hand: InteractionHand, hitResult: HitResult? = null): InteractionResult {
        val item = player.getItemInHand(hand)
        if (level.isClientSide || level !is ServerLevel)
            return InteractionResult.PASS
        val gameState = level.gameState
        if (!gameState.isPlayerAlive(player))
            return InteractionResult.PASS
        val type = item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString(CUSTOM_ITEM_TAG)?.getOrNull()
        val team = gameState.getPlayersTeam(player.uuid)
        BedwarsPlugin.LOGGER.info("Item has $CUSTOM_ITEM_TAG $type")
        when (type) {
            CustomItemTypes.FIREBALL.value -> return useFireballEffect(player, level, item)
            CustomItemTypes.INSTANT_TNT.value -> return useInstantTNTEffect(player, level, item, hitResult)
            CustomItemTypes.POPUP_TOWER.value -> return usePopupTowerEffect(player, level, item, hitResult, team)
            CustomItemTypes.PLAYER_TRACKER.value -> return usePlayerTrackerEffect(player, level, item, team)
            CustomItemTypes.DREAM_DEFENDER.value -> return useDreamDefenderEffect(player, level, item, hitResult, team)
            CustomItemTypes.BED_BRUTE.value -> return useBedBruteEffect(player, level, item, hitResult, team)
        }
        return InteractionResult.PASS
    }

    fun triggerCustomProjectileTickEffect(projectile: Projectile): InteractionResult {
        val level = projectile.level()
        val owner = projectile.owner
        if (level.isClientSide || level !is ServerLevel)
            return InteractionResult.PASS
        val gameState = level.gameState
        if (owner !is Player)
            return InteractionResult.PASS
        if (projectile !is ThrowableItemProjectile) // required for casting to ThrowableItemProjectile to grab item data (which all custom projectiles are)
            return InteractionResult.PASS

        val type = projectile.item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString(CUSTOM_ITEM_TAG)?.getOrNull()
        BedwarsPlugin.LOGGER.info("Entity has $CUSTOM_ITEM_TAG $type")
        val team = gameState.getPlayersTeam(owner.uuid)
        when (type) {
            CustomItemTypes.BRIDGE_EGG.value -> return tickBridgeEggEffect(level, projectile, team)
        }
        return InteractionResult.PASS
    }

    fun triggerCustomProjectileHitEffect(projectile: Projectile, hitResult: HitResult): InteractionResult {
        val level = projectile.level()
        val owner = projectile.owner
        if (level.isClientSide || level !is ServerLevel)
            return InteractionResult.PASS
        val gameState = level.gameState
        if (owner !is Player)
            return InteractionResult.PASS
        if (projectile !is ThrowableItemProjectile) // required for casting to ThrowableItemProjectile to grab item data (which all custom projectiles are)
            return InteractionResult.PASS

        val type = projectile.item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString(CUSTOM_ITEM_TAG)?.getOrNull()
        BedwarsPlugin.LOGGER.info("Entity has $CUSTOM_ITEM_TAG $type")
        val team = gameState.getPlayersTeam(owner.uuid)
        when (type) {
            CustomItemTypes.BALL_OF_BUGS.value -> return doBallOfBugsEffect(level, projectile, team, hitResult)
        }
        return InteractionResult.PASS
    }

    private fun useFireballEffect(player: Player, level: Level, item: ItemStack): InteractionResult {
        BedwarsPlugin.LOGGER.info("Doing fireball thing")
        val directionVector = player.getViewVector(1.0f)
        val fireball = LargeFireball(level, player, directionVector.scale(FIREBALL_SPEED), FIREBALL_POWER)
        fireball.setPos(player.eyePosition.add(directionVector.scale(0.5)))
        fireball.owner = player
        fireball.deltaMovement = directionVector.scale(FIREBALL_SPEED)
        level.addFreshEntity(fireball)
        if (!player.isCreative) item.count -= 1
        return InteractionResult.SUCCESS
    }

    private fun useInstantTNTEffect(player: Player, level: Level, item: ItemStack, hitResult: HitResult?): InteractionResult {
        if (hitResult !is HitResult)
            return InteractionResult.PASS
        val pos = hitResult.location
        val tnt = PrimedTnt(level, pos.x, pos.y, pos.z, player)
        level.addFreshEntity(tnt)
        if (!player.isCreative) item.count -= 1
        return InteractionResult.SUCCESS
    }

    private fun tickBridgeEggEffect(level: Level, egg: ThrowableItemProjectile, team: Team): InteractionResult {
        val bridgePos = egg.position().relative(Direction.DOWN, 2.0)
        val newBlockState = Blocks.WOOL.pick(team.dyeColour).defaultBlockState()
        placeBlockIfValid(level,bridgePos.add(0.5, BRIDGE_EGG_OFFSET, 0.5), newBlockState)
        placeBlockIfValid(level,bridgePos.add(0.5, BRIDGE_EGG_OFFSET, -0.5), newBlockState)
        placeBlockIfValid(level,bridgePos.add(-0.5, BRIDGE_EGG_OFFSET, 0.5), newBlockState)
        placeBlockIfValid(level,bridgePos.add(-0.5, BRIDGE_EGG_OFFSET, -0.5), newBlockState)
        return InteractionResult.SUCCESS
    }

    private fun doBallOfBugsEffect(level: Level, ball: ThrowableItemProjectile, team: Team, hitResult: HitResult): InteractionResult {
        if (level !is ServerLevel)
            return InteractionResult.PASS
        spawnBedBug(level, hitResult.location, team)
        ball.owner = null
        return InteractionResult.SUCCESS
    }

    private fun usePopupTowerEffect(player: Player, level: Level, item: ItemStack, hitResult: HitResult?, team: Team): InteractionResult {
        if (hitResult !is HitResult || level !is ServerLevel)
            return InteractionResult.PASS
        val centerPos = hitResult.location.toBlockPos()
        val buildingBlockState = Blocks.WOOL.pick(team.dyeColour).defaultBlockState()
        val direction = player.lookAngle.toCardinalDirection()
        level.eventQueue.queuePopupTowerConstruction(centerPos, buildingBlockState, direction)
        if (!player.isCreative) item.count -= 1
        player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f)
        player.sendSystemMessage(Component.literal("Deploying tower."))
        return InteractionResult.SUCCESS
    }

    private fun usePlayerTrackerEffect(player: Player, level: Level, item: ItemStack, team: Team): InteractionResult {
        if (level.isClientSide)
            return InteractionResult.PASS
        if (level !is ServerLevel)
            return InteractionResult.PASS
        fun isEnemy(otherPlayer: Player): Boolean {
            val otherTeam = level.gameState.getPlayersTeam(otherPlayer.uuid)
            return (otherTeam != Team.NONE && otherTeam != team)
        }
        fun getDistance(otherPlayer: Entity): Double {
            return player.position().subtract(otherPlayer.position()).length()
        }
//        val nearestEnemy = level.allEntities.filter { !it.`is`(player) }.minByOrNull { getDistance(it) } ?: run {
//            player.sendSystemMessage(Component.literal("No enemy player found."))
//            return InteractionResult.SUCCESS
//        }
        val nearestEnemy = level.players().filter { isEnemy(it) }.minByOrNull { getDistance(it) } ?: run {
            player.sendSystemMessage(Component.literal("No enemy player found."))
            return InteractionResult.SUCCESS
        }

        val enemyPos = GlobalPos.of(level.dimension(), nearestEnemy.position().toBlockPos())
        val displacement = player.eyePosition.subtract(nearestEnemy.position())
        val distance = displacement.length()
        item.set(DataComponents.LODESTONE_TRACKER, LodestoneTracker(Optional.of(enemyPos), true))
        player.sendSystemMessage(Component.literal("Enemy ${distance.roundToInt()} blocks away."))
        player.teleportTo(level, player.x, player.y, player.z, emptySet(), displacement.pitchDeg(), displacement.yawDeg(), true)
        return InteractionResult.SUCCESS
    }

    private fun useDreamDefenderEffect(player: Player, level: Level, item: ItemStack, hitResult: HitResult?, team: Team): InteractionResult {
        if (hitResult !is HitResult || level !is ServerLevel)
            return InteractionResult.PASS
        val position = hitResult.location
        spawnDreamDefender(level, position, team)
        if (!player.isCreative) item.count -= 1
        return InteractionResult.SUCCESS
    }

    private fun useBedBruteEffect(player: Player, level: Level, item: ItemStack, hitResult: HitResult?, team: Team): InteractionResult {
        if (hitResult !is HitResult || level !is ServerLevel)
            return InteractionResult.PASS
        val position = hitResult.location
        spawnBedBrute(level, position, team)
        if (!player.isCreative) item.count -= 1
        return InteractionResult.SUCCESS
    }
}
