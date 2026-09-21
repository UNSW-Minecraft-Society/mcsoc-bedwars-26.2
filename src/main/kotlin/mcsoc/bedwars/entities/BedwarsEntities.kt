package mcsoc.bedwars.entities

import mcsoc.bedwars.datatrackers.CustomEntityType
import mcsoc.bedwars.datatrackers.customEntityData
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.withEnchant
import mcsoc.bedwars.utils.withTrim
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.Endermite
import net.minecraft.world.entity.monster.piglin.PiglinBrute
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.equipment.trim.TrimPatterns
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration

val DREAM_DEFENDER_EXPIRY_TIME: Duration = Duration.parse("1m")
val BED_BRUTE_TRIM = TrimPatterns.SNOUT
val BED_BRUTE_EXPIRY_TIME: Duration = Duration.parse("14s")

fun spawnShopkeeper(level: ServerLevel, position: Vec3, type: CustomEntityType) {
    val shopkeeper = Villager(EntityTypes.VILLAGER, level)
    shopkeeper.setPos(position)
    shopkeeper.isNoAi = true
    shopkeeper.isInvulnerable = true
    shopkeeper.customName = Component.literal(type.title)
    shopkeeper.isCustomNameVisible = true
    if (level.addFreshEntity(shopkeeper)) level.customEntityData.addEntity(shopkeeper, type)
}

fun spawnBedBug(level: Level, position: Vec3, team: Team) {
    val bug = Endermite(EntityTypes.ENDERMITE, level)
    bug.setPos(position)
    bug.health = 1.0f
    bug.speed = 2.0f
    val scoreboardTeam = level.scoreboard.getPlayerTeam(team.getName())
    if (scoreboardTeam != null) level.scoreboard.addPlayerToTeam(bug.stringUUID, scoreboardTeam)
    level.addFreshEntity(bug)
}

fun spawnDreamDefender(level: ServerLevel, position: Vec3, team: Team) {
    val defender = IronGolem(EntityTypes.IRON_GOLEM, level)
    defender.setPos(position)
    val scoreboardTeam = level.scoreboard.getPlayerTeam(team.getName())
    if (scoreboardTeam != null) level.scoreboard.addPlayerToTeam(defender.stringUUID, scoreboardTeam)

    if (level.addFreshEntity(defender)) {
        level.eventQueue.queueEntityExpiry(DREAM_DEFENDER_EXPIRY_TIME, defender.uuid)
        level.customEntityData.addTeamEntity(defender, CustomEntityType.DREAM_DEFENDER, team)
    }
}

fun spawnBedBrute(level: ServerLevel, position: Vec3, team: Team) {
    val brute = PiglinBrute(EntityTypes.PIGLIN_BRUTE, level)
    brute.setPos(position)
    brute.equipItemIfPossible(level, Items.GOLDEN_LEGGINGS.defaultInstance
        .withTrim(team.trimMaterial, BED_BRUTE_TRIM, level)
        .withEnchant(Enchantments.VANISHING_CURSE, 1, level)
    )
    brute.equipItemIfPossible(level, Items.LEATHER_BOOTS.defaultInstance
        .also {it.set(DataComponents.DYED_COLOR, DyedItemColor(team.dyeColour.textureDiffuseColor))}
        .withEnchant(Enchantments.VANISHING_CURSE, 1, level)
    )
    brute.equipItemIfPossible(level, Items.GOLDEN_AXE.defaultInstance
        .withEnchant(Enchantments.VANISHING_CURSE, 1, level)
        .also { it.damageValue = 0 }
    )
    brute.getAttribute(Attributes.ATTACK_DAMAGE)?.let { it.baseValue = 0.0 }
    val scoreboardTeam = level.scoreboard.getPlayerTeam(team.getName())
    if (scoreboardTeam != null) level.scoreboard.addPlayerToTeam(brute.stringUUID, scoreboardTeam)
    if (level.addFreshEntity(brute)) {
        level.eventQueue.queueEntityExpiry(BED_BRUTE_EXPIRY_TIME, brute.uuid)
        // "Doomed to death of KARMA!" - NarraChara UnderTale
    }
}

fun spawnDeathmatchDragon(level: ServerLevel, position: Vec3) {
    val dragon = WitherBoss(EntityTypes.WITHER, level)
    dragon.setPos(position)
    dragon.customName = Component.literal("Waking Wither")
    if (level.addFreshEntity(dragon)) level.customEntityData.addEntity(dragon, CustomEntityType.DEATHMATCH_DRAGON)
}