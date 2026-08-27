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
    data class Upgradable(val upgradeMultipliers: List<Double>) : GeneratorKind
    data class Tiered(val tierMultipliers: List<Double>) : GeneratorKind
}

data class GeneratorItem(val item: Item, val itemsPerCycle: Int, val maxItems: Int) {
    companion object {
        val CODEC: Codec<GeneratorItem> = RecordCodecBuilder.create {
            it.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(GeneratorItem::item),
                Codec.INT.fieldOf("generation_time").forGetter(GeneratorItem::itemsPerCycle),
                Codec.INT.fieldOf("maxItems").forGetter(GeneratorItem::maxItems)
            ).apply(it, ::GeneratorItem)
        }
    }
}

// can be made to be loaded from a default json file and deserialised
object DefaultGeneratorTypes {
    val generators = mapOf<String, GeneratorConfig>(
        "base" to GeneratorConfig(
            GeneratorKind.Upgradable(listOf(1.0, 1.5, 2.5, 2.5, 4.5)),
            listOf(
                GeneratorItem(Items.IRON_INGOT, 8, 48),
                GeneratorItem(Items.GOLD_INGOT, 1, 16)
            ),
            4 * 20
        ),
        "diamond" to GeneratorConfig(
            GeneratorKind.Tiered(listOf(1.0, 1.25, 2.5)),
            listOf(GeneratorItem(Items.DIAMOND, 1, 8)),
            30 * 20
        ),
        "emerald" to GeneratorConfig(
            GeneratorKind.Tiered(listOf(1.0, 1.3, 1.85)),
            listOf(GeneratorItem(Items.EMERALD, 1, 4)),
            65 * 20
        )
    )
    
    fun getCurrentGenerators() = generators.keys
}
