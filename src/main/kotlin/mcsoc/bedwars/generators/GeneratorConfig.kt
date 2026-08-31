package mcsoc.bedwars.generators

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items


// these can be serialised and moved to a json
data class GeneratorConfig(val kind: GeneratorKind, val items: List<GeneratorItem>, val cycleTime: Int)

sealed interface GeneratorKind {
    data object Default : GeneratorKind
    data class Base(val upgradeMultipliers: List<Double>) : GeneratorKind
    data class Tiered(val tierMultipliers: List<Double>) : GeneratorKind
}

data class GeneratorItem(val item: Item, val itemsPerCycle: Int, val maxItems: Int)


enum class GeneratorType {
    BASE,
    DIAMOND,
    EMERALD;

    fun getConfig(): GeneratorConfig {
        return when (this) {
            BASE -> GeneratorConfig(
                GeneratorKind.Base(listOf(1.0, 1.5, 2.5, 2.5, 4.5)),
                listOf(
                    GeneratorItem(Items.IRON_INGOT, 4 * 80, 48),
                    GeneratorItem(Items.GOLD_INGOT, 1 * 80, 16)
                ),
                4 * 80 * 20 // 80 scale factor to ensure emerald upgrade generation is slow
            )

            DIAMOND -> GeneratorConfig(
                GeneratorKind.Tiered(listOf(1.0, 1.25, 2.5)),
                listOf(GeneratorItem(Items.DIAMOND, 1, 8)),
                30 * 20
            )

            EMERALD -> GeneratorConfig(
                GeneratorKind.Tiered(listOf(1.0, 1.3, 1.85)),
                listOf(GeneratorItem(Items.EMERALD, 1, 4)),
                65 * 20
            )
        }

    }
}
