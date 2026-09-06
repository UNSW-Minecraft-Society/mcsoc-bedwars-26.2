package mcsoc.bedwars.generators

import com.mojang.serialization.Codec
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items


// these can be serialised and moved to a json
data class GeneratorConfig(val kind: GeneratorKind, val cycleTime: Int, val showTimer: Boolean)

sealed interface GeneratorKind {
    fun itemsAt(level: Int): Iterable<GeneratorItem>
    fun rateAt(level: Int): Double

    data class Default(val items: Iterable<GeneratorItem>) : GeneratorKind {
        override fun itemsAt(level: Int) = items
        override fun rateAt(level: Int) = 1.0
    }

    data class Base(val items: Map<Int, Iterable<GeneratorItem>>, val rates: List<Double>) : GeneratorKind {
        override fun itemsAt(level: Int) = items[level] ?: emptyList()
        override fun rateAt(level: Int) = rates.getOrElse(level) { rates.last() }
    }

    data class Tiered(val items: Iterable<GeneratorItem>, val rates: List<Double>) : GeneratorKind {
        override fun itemsAt(level: Int) = items
        override fun rateAt(level: Int) = rates.getOrElse(level) { rates.last() }
    }
}

data class GeneratorItem(val item: Item, val itemsPerCycle: Int, val maxItems: Int)


enum class GeneratorType {
    BASE,
    DIAMOND,
    EMERALD;
    
    companion object {
        val CODEC: Codec<GeneratorType> = Codec.STRING.xmap(::valueOf, GeneratorType::name)
        fun getTieredTypes() = entries.filter { it.getConfig().kind !is GeneratorKind.Base}
    }

    fun getConfig(): GeneratorConfig {
        val baseGenT1Items = listOf(GeneratorItem(Items.IRON_INGOT, 4 * 80, 48), GeneratorItem(Items.GOLD_INGOT, 1 * 80, 16))
        val baseGenT3Items = baseGenT1Items + GeneratorItem(Items.EMERALD, 1, 4)
        
        return when (this) {
            BASE -> GeneratorConfig(
                GeneratorKind.Base(
                    listOf(
                        (0..2).map { it to baseGenT1Items },
                        (3..4).map {it to baseGenT3Items}
                    ).flatten().toMap(),
                    listOf(1.0, 1.5, 2.5, 2.5, 4.5)
                ),
                4 * 80 * 20, // 80 scale factor to ensure emerald upgrade generation is slow
                false
            )

            DIAMOND -> GeneratorConfig(
                GeneratorKind.Tiered(
                    listOf(GeneratorItem(Items.DIAMOND, 1, 8)),
                    listOf(1.0, 1.25, 2.5)
                ),
                30 * 20,
                true
            )

            EMERALD -> GeneratorConfig(
                GeneratorKind.Tiered(
                    listOf(GeneratorItem(Items.EMERALD, 1, 4)),
                    listOf(1.0, 1.3, 1.85)
                ),
                65 * 20,
                true
            )
        }
    }    
}
