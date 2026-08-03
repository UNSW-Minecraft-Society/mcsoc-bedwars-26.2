package mcsoc.bedwars.datatrackers

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// todo show time remaining until next gen (depending on generator type)

enum class GeneratorType { IRON, GOLD, DIAMOND, EMERALD }

private object GeneratorFactory {
    fun createGenerator(type: GeneratorType, location: Vec3, player: ServerPlayer): Generator {
        val team = "TEMP_TEAM" // change way to get players team
        return createGenerator(type, location, player.level(), team)
    }

    fun createGenerator(type: GeneratorType, location: Vec3, level: ServerLevel, team: String? = null): Generator {
        return when (type) {
            GeneratorType.IRON -> IslandGenerator(location, 0.5.seconds, Items.IRON_INGOT, level, 48, team!!)
            GeneratorType.GOLD -> IslandGenerator(location, 4.seconds, Items.GOLD_INGOT, level, 16, team!!)
            GeneratorType.DIAMOND -> TieredGenerator(location, 30.seconds, Items.DIAMOND, level, 4)
            GeneratorType.EMERALD -> TieredGenerator(location, 65.seconds, Items.EMERALD, level, 2)
        }
    }
}

internal open class Generator(
    val location: Vec3,
    val genTime: Duration,
    val item: Item,
    val level: ServerLevel,
    val maxItems: Int
) {
    // TODO add codec

    private var rateMultiplier: Double = 1.0
    private var timeSinceGen = 0.seconds
    private var playerRange = 15 // can move to config

    fun setRateMultiplier(rate: Double) {
        rateMultiplier = rate
    }

    fun setPlayerRange(range: Int) {
        playerRange = range
    }

    fun tick(deltaTime: Duration) {
        timeSinceGen += deltaTime
        if (timeSinceGen < (genTime / rateMultiplier) || !hasSpace()) return

        generateItem(level)
        timeSinceGen = 0.seconds
    }

    private fun hasSpace(): Boolean {
        val nearby = level.getEntitiesOfClass(
            ItemEntity::class.java,
            AABB.ofSize(location, 2.0, 2.0, 2.0)
        )

        return nearby.sumOf { it.item.count } < maxItems
    }

    private fun generateItem(level: ServerLevel) {
        val itemstack = ItemStack(item, 1)
        val entity = ItemEntity(level, location.x, location.y + 1, location.z, itemstack)
        entity.addTag("generator_item")
        entity.setDeltaMovement(0.0, 0.0, 0.0)
        level.addFreshEntity(entity)
    }
}


// over time game upgrades generator speeds
// diamond: +25%, +100% (total +150%)
// emerald: +30%, +42% (total +85%)

// todo change team arg to whatever team system is used
internal class IslandGenerator(
    location: Vec3,
    genTime: Duration,
    item: Item,
    level: ServerLevel,
    maxItems: Int,
    val team: String
) :
    Generator(location, genTime, item, level, maxItems) {
    private var currentUpgrade = 1

    fun upgrade() {
        TODO("implement island upgrades")
    }
}


// team upgrades increase speeds
// +50%, +100%, add emeralds to base gen, +200%

internal class TieredGenerator(location: Vec3, genTime: Duration, item: Item, level: ServerLevel, maxItems: Int) :
    Generator(location, genTime, item, level, maxItems) {
    private var currentTier = 1

    fun upgrade() {
        TODO("implement generator tiers")
    }
}


internal interface GeneratorsExposer {
    fun addGenerator(type: GeneratorType, location: Vec3, player: ServerPlayer)
    fun removeGenerator(location: Vec3)

    // change team arg to whatever team implementation uses
    fun upgradeIslandGenerators(team: String)
    fun upgradeGeneratorTier()

    fun tickGenerators(deltaTime: Duration)
}


internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)

    override fun addGenerator(type: GeneratorType, location: Vec3, player: ServerPlayer) {
        addGenerator(GeneratorFactory.createGenerator(type, location, player))
    }

    override fun removeGenerator(location: Vec3) {
        val generators = getGenerators()
            .filter { it.location == location }
            .forEach { removeGenerator(it) }
    }

    override fun upgradeGeneratorTier() {
        getGenerators()
            .filterIsInstance<TieredGenerator>()
            .forEach { gen ->
                gen.upgrade()
            }
    }

    override fun upgradeIslandGenerators(team: String) {
        getGenerators()
            .filterIsInstance<IslandGenerator>()
            .filter { it.team == team }
            .forEach { gen ->
                gen.upgrade()
            }
    }

    override fun tickGenerators(deltaTime: Duration) {
        getGenerators()
            .forEach { it.tick(deltaTime) }
    }
}
