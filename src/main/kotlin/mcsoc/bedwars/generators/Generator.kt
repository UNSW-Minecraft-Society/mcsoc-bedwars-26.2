package mcsoc.bedwars.generators

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

// todo show time remaining until next gen (depending on generator type)

internal object GeneratorFactory {
    fun createGenerator(config: GeneratorConfig, loc: Vec3): Generator {
        return when (config.kind) {
            is GeneratorKind.Default -> Generator(loc, config.cycleTime, config.items)
            is GeneratorKind.Upgradable -> IslandGenerator(
                loc,
                config.cycleTime,
                config.items,
                config.kind.upgradeMultipliers
            )

            is GeneratorKind.Tiered -> TieredGenerator(loc, config.cycleTime, config.items, config.kind.tierMultipliers)
        }
    }
}

// cycle time in ticks
internal open class Generator(val location: Vec3, val cycleTime: Int, val items: List<GeneratorItem>) {
    companion object {
        val CODEC: Codec<Generator> = RecordCodecBuilder.create {
            it.group(
                Vec3.CODEC.fieldOf("location").forGetter(Generator::location),
                Codec.INT.fieldOf("ticks_per_cycle").forGetter(Generator::cycleTime),
                GeneratorItem.CODEC.listOf().fieldOf("items").forGetter(Generator::items)
            ).apply(it, ::Generator)
        }
    }

    private var rateMultiplier: Double = 1.0
    private var playerRange = 15 // can move to config
    
    private var currentTick = 0
    private val curCycleItems: HashMap<GeneratorItem, Int> = HashMap()
    

    fun setRateMultiplier(rate: Double) {
        rateMultiplier = rate
    }

    fun setPlayerRange(range: Int) {
        playerRange = range
    }

    fun tick(level: ServerLevel) {
        currentTick++
        val cycle = cycleTime / rateMultiplier

        for (item in items) {
            val generated = curCycleItems[item] ?: 0
            val expected = (currentTick / cycle * item.itemsPerCycle).toInt()

            if (generated >= expected || !hasSpace(level, item.maxItems)) continue

            generateItem(level, item.item)
            curCycleItems[item] = generated + 1
        }

        if (currentTick >= cycle) {
            currentTick = 0
            curCycleItems.clear()
        }
    }

    private fun hasSpace(level: ServerLevel, max: Int): Boolean {
        val nearby = level.getEntitiesOfClass(
            ItemEntity::class.java,
            AABB.ofSize(location, 2.0, 2.0, 2.0)
        )

        return nearby.sumOf { it.item.count } < max
    }

    private fun generateItem(level: ServerLevel, item: Item) {
        val itemstack = ItemStack(item, 1)
        val entity = ItemEntity(level, location.x, location.y + 1, location.z, itemstack)
        entity.addTag("generator_item")
        entity.setDeltaMovement(0.0, 0.0, 0.0)
        level.addFreshEntity(entity)
    }
}


internal class IslandGenerator(location: Vec3, cycleTime: Int, items: List<GeneratorItem>, val upgrades: List<Double>) :
    Generator(location, cycleTime, items) {
    private var currentUpgrade = 0

    fun upgrade() {
        currentUpgrade++
        if (currentUpgrade >= upgrades.size) return
        setRateMultiplier(upgrades[currentUpgrade])
    }
}

internal class TieredGenerator(location: Vec3, cycleTime: Int, items: List<GeneratorItem>, val tiers: List<Double>) :
    Generator(location, cycleTime, items) {
    private var currentTier = 0

    fun upgrade() {
        currentTier++
        if (currentTier >= tiers.size) return
        setRateMultiplier(tiers[currentTier])
    }
}

