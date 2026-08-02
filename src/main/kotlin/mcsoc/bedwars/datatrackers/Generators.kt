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

enum class GeneratorType { IRON, GOLD, DIAMOND, EMERALD }

internal object GeneratorFactory {
    fun createGenerator(type: GeneratorType, location: Vec3, level: ServerLevel): Generator {
        return when (type) {
            GeneratorType.IRON -> Generator(location, 0.5, Items.IRON_INGOT, level, 48)
            GeneratorType.GOLD -> Generator(location, 4.0, Items.GOLD_INGOT, level, 16)
            GeneratorType.DIAMOND -> Generator(location, 30.0, Items.DIAMOND, level, 4)
            GeneratorType.EMERALD -> Generator(location, 65.0, Items.EMERALD, level, 2)
        }
    }

    fun createGenerator(type: GeneratorType, player: ServerPlayer): Generator {
        return createGenerator(type, player.position(), player.level())
    }

    fun createGenerator(location: Vec3, genTime: Double, item: Item, level: ServerLevel, maxItems: Int): Generator {
        return Generator(location, genTime, item, level, maxItems)
    }
}

// todo add serialise codec
class Generator(
    val location: Vec3,
    val genTime: Double,
    val item: Item,
    val level: ServerLevel,
    val maxItemStack: Int
) {
    private var rateMultiplier: Double = 1.0
    private var timeSinceGen = 0

    private val PLAYER_RANGE = 15

    fun tick() {
        timeSinceGen++

        if (timeSinceGen < genTime) return

        // generate
        val playersInRange = level.getPlayers { it.position().distanceTo(location) < PLAYER_RANGE }
        playersInRange.forEach { player ->

        }

        timeSinceGen = 0
    }

    fun setRateMultiplier(rate: Double) {
        rateMultiplier = rate
    }

    private fun generateItem() {
        val itemstack = ItemStack(item, 1)

    }
}


internal interface GeneratorsExposer {
    fun getGenerators(): ArrayList<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)
    fun upgradeIslandGenerators() // upgrade a specific team generator
    fun upgradeGeneratorTier() // upgrade diamond and emerald generators (can make more general)

    fun tickGenerators()
}


internal interface GeneratorsHolder : GeneratorsExposer {

}

