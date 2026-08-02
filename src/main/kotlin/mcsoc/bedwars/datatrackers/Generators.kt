package mcsoc.bedwars.datatrackers

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

// start generating when player is near
// stop at max items
// make gen scale with players, and each player can pick up their own when many near it

// over time game upgrades generator speeds
// diamond: +25%, +100% (total +150%)
// emerald: +30%, +42% (total +85%)

// team upgrades also increase speeds, separate to game timer
// +50%, +100%, add emeralds to base gen, +200%

// show time remaining until next gen (depending on generator type)


// enum containing hypixels 
enum class GeneratorType(val create: (Vec3, ServerLevel, String?) -> Generator) {
    IRON({ loc, l, t -> IslandGenerator(loc, 0.5, Items.IRON_INGOT, l, 48, t!!) }),
    GOLD({ loc, l, t -> IslandGenerator(loc, 4.0, Items.GOLD_INGOT, l, 16, t!!) }),
    DIAMOND({ loc, l, _ -> TieredGenerator(loc, 30.0, Items.DIAMOND, l, 4) }),
    EMERALD({ loc, l, _ -> TieredGenerator(loc, 65.0, Items.EMERALD, l, 2) });
}

internal object GeneratorFactory {
    fun createGenerator(type: GeneratorType, location: Vec3, level: ServerLevel, team: String? = null): Generator {
        return type.create(location, level, team)
    }

    fun createGenerator(type: GeneratorType, player: ServerPlayer): Generator {
        return createGenerator(type, player.position(), player.level()) // add way to get players team
    }
}

open class Generator(
    val location: Vec3,
    val genTime: Double,
    val item: Item,
    val level: ServerLevel,
    val maxItemStack: Int
) {
    // TODO add codec

    private var rateMultiplier: Double = 1.0
    private var timeSinceGen = 0

    private val PLAYER_RANGE = 15

    fun tick() {
        timeSinceGen++
        TODO("stop gen if reached max items")

        if (timeSinceGen < genTime) return
        TODO("use rate mutliplier")
        TODO("change to use delta_time instead of single increments")


        // generate
        val playersInRange = level.getPlayers { it.position().distanceTo(location) < PLAYER_RANGE }
        playersInRange.forEach { player ->
            TODO("detect players nearby, label items to players for pickup priority, scale generation with players")

        }
        TODO("ticking generator")

        timeSinceGen = 0
    }

    fun setRateMultiplier(rate: Double) {
        rateMultiplier = rate
    }

    private fun generateItem() {
        val itemstack = ItemStack(item, 1)
        // summon item
        TODO("summon item")

    }
}

// todo change team arg to whatever team system is used
class IslandGenerator(
    location: Vec3,
    genTime: Double,
    item: Item,
    level: ServerLevel,
    maxItemStack: Int,
    val team: String
) :
    Generator(location, genTime, item, level, maxItemStack) {
    private var currentUpgrade = 1

    fun upgrade() {
        TODO("implement island upgrades")
    }
}

class TieredGenerator(location: Vec3, genTime: Double, item: Item, level: ServerLevel, maxItemStack: Int) :
    Generator(location, genTime, item, level, maxItemStack) {
    private var currentTier = 1

    fun upgrade() {
        TODO("implement geerator tiers")
    }
}


internal interface GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)
    fun removeGenerator(location: Vec3)

    // todo change team arg to whatever team implementation uses
    fun upgradeIslandGenerators(team: String) // upgrade a specific team generator
    fun upgradeGeneratorTier() // upgrade diamond and emerald generators

    fun tickGenerators()
}


internal interface GeneratorsHolder : GeneratorsExposer {
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

    override fun tickGenerators() {
        getGenerators()
            .forEach { it.tick() }
    }

    override fun removeGenerator(location: Vec3) {
        val generators = getGenerators()
            .filter { it.location == location }
            .forEach { removeGenerator(it) }
    }
}

