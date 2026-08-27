package mcsoc.bedwars.generators

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

// todo show time remaining until next gen (depending on generator type)

internal object GeneratorFactory {
    fun createGenerator(config: GeneratorConfig, loc: Vec3, dim: ResourceKey<Level>): Generator {
        val place = GenPlace(loc, dim)
        return when (config.kind) {
            is GeneratorKind.Default -> Generator(place, config.cycleTime, config.items)
            is GeneratorKind.Upgradable -> IslandGenerator(place, config.cycleTime, config.items, config.kind.upgradeMultipliers)
            is GeneratorKind.Tiered -> TieredGenerator(place, config.cycleTime, config.items, config.kind.tierMultipliers)
        }
    }
}


internal data class GenPlace(val location: Vec3, val dim: ResourceKey<Level>) {
    companion object {
        val CODEC: Codec<GenPlace> = RecordCodecBuilder.create { it.group(
            Vec3.CODEC.fieldOf("location").forGetter(GenPlace::location),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(GenPlace::dim)
        ).apply(it, ::GenPlace)}
    }
}

// cycle time in ticks
internal open class Generator(val place: GenPlace, val cycleTime: Int, val items: List<GeneratorItem>) {
    companion object {
        val CODEC: Codec<Generator> = RecordCodecBuilder.create {
            it.group(
                GenPlace.CODEC.fieldOf("location").forGetter(Generator::place),
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

    fun tick(server: MinecraftServer) {
        val level = server.getLevel(place.dim) ?: return
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
            AABB.ofSize(place.location, 2.0, 2.0, 2.0)
        )

        return nearby.sumOf { it.item.count } < max
    }

    private fun generateItem(level: ServerLevel, item: Item) {
        val itemstack = ItemStack(item, 1)
        val loc = place.location
        val entity = ItemEntity(level, loc.x, loc.y + 1, loc.z, itemstack)
        entity.addTag("generator_item")
        entity.setDeltaMovement(0.0, 0.0, 0.0)
        level.addFreshEntity(entity)
    }
}


internal class IslandGenerator(place: GenPlace, cycleTime: Int, items: List<GeneratorItem>, val upgrades: List<Double>) :
    Generator(place, cycleTime, items) {
    private var currentUpgrade = 0

    fun upgrade() {
        currentUpgrade++
        if (currentUpgrade >= upgrades.size) return
        setRateMultiplier(upgrades[currentUpgrade])
    }
}

internal class TieredGenerator(place: GenPlace, cycleTime: Int, items: List<GeneratorItem>, val tiers: List<Double>) :
    Generator(place, cycleTime, items) {
    private var currentTier = 0

    fun upgrade() {
        currentTier++
        if (currentTier >= tiers.size) return
        setRateMultiplier(tiers[currentTier])
    }
}

