package mcsoc.bedwars.generators

import com.mojang.serialization.Codec
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items


data class GeneratorConfig(val kind: GeneratorKind, val cycleTime: Int, val showTimer: Boolean)

sealed interface GeneratorKind {
    fun itemsAt(level: Int): Iterable<GeneratorItem>
    fun rateAt(level: Int): Double

    data class Default(val items: Iterable<GeneratorItem>) : GeneratorKind {
        override fun itemsAt(level: Int) = items
        override fun rateAt(level: Int) = 1.0
    }

    data class Upgradable(val items: Map<Int, Iterable<GeneratorItem>>, val rates: List<Double>) : GeneratorKind {
        override fun itemsAt(level: Int) = items[level] ?: items.values.lastOrNull() ?: emptyList()
        override fun rateAt(level: Int) = rates.getOrElse(level) { rates.last() }
    }
}

data class GeneratorItem(val item: Item, val itemsPerCycle: Int, val maxItems: Int)


private val baseGenT1Items = listOf(GeneratorItem(Items.IRON_INGOT, 4 * 80, 48), GeneratorItem(Items.GOLD_INGOT, 1 * 80, 16))
private val baseGenT3Items = baseGenT1Items + GeneratorItem(Items.EMERALD, 1, 4)
        
sealed interface GeneratorType {
    val config: GeneratorConfig
    fun getUpgrade(level: ServerLevel): Int
    
    data class BASE(val team: Team): GeneratorType {
        override val config = GeneratorConfig(
            GeneratorKind.Upgradable(
                listOf(
                    (0..2).map { it to baseGenT1Items },
                    (3..4).map {it to baseGenT3Items}
                ).flatten().toMap(),
                listOf(1.0, 1.5, 2.5, 2.5, 4.5)
            ),
            4 * 80 * 20, // 80 scale factor to ensure emerald upgrade generation is slow
            false
        )
        
        override fun getUpgrade(level: ServerLevel) = level.gameState.getGenUpgrade(team)
    }
    
    data object DIAMOND: GeneratorType {
        override val config = GeneratorConfig(
            GeneratorKind.Upgradable(
                mapOf(0 to listOf(GeneratorItem(Items.DIAMOND, 1, 8))),
                listOf(1.0, 1.25, 2.5)
            ),
            30 * 20,
            true
        )

        override fun getUpgrade(level: ServerLevel) = level.generatorState.getGeneratorUpgrade(this)
    }
    
    data object EMERALD: GeneratorType {
        override val config = GeneratorConfig(
            GeneratorKind.Upgradable(
                mapOf(0 to listOf(GeneratorItem(Items.EMERALD, 1, 4))),
                listOf(1.0, 1.3, 1.85)
            ),
            65 * 20,
            true
        )
        
        override fun getUpgrade(level: ServerLevel) = level.generatorState.getGeneratorUpgrade(this)
    }
    
    companion object {
        val ENTRIES = mapOf("DIAMOND" to DIAMOND, "EMERALD" to EMERALD)
        
        val CODEC: Codec<GeneratorType> = Codec.STRING.xmap(
            { value ->
                when {
                    value == "diamond" -> DIAMOND
                    value == "emerald" -> EMERALD
                    value.startsWith("base_") -> {
                        val team = Team.entries.first {
                            it.getName() == value.removePrefix("base_")
                        }
                        BASE(team)
                    }
                    else -> error("Unknown generator type: $value")
                }
            },
            { type ->
                when (type) {
                    DIAMOND -> "diamond"
                    EMERALD -> "emerald"
                    is BASE -> "base_${type.team.getName()}"
                }
            },
        )
    }
}

