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
        
sealed class GeneratorType(private val id_factory: (GeneratorType) -> String) {
    abstract val config: GeneratorConfig
    abstract fun getUpgrade(level: ServerLevel): Int
    val id: String get() = this.id_factory(this)
    
    data class BASE(val team: Team): GeneratorType(::getSerialId) {
        companion object {
            internal const val prefix = "base_"
            internal fun parseSerial(serial: String): GeneratorType.BASE {
                val team = Team.valueOf(serial.removePrefix(GeneratorType.BASE.prefix).uppercase())
                return BASE(team)
            }
            
            private fun getSerialId(type: GeneratorType): String {
                if (type !is GeneratorType.BASE) return ""
                return prefix + type.team.name
            }
        }
        
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
    
    data object DIAMOND: GeneratorType({"diamond"}) {
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
    
    data object EMERALD: GeneratorType({"emerald"}) {
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
        fun parseTypeString(str: String): GeneratorType? {
            val str_lower = str.lowercase()
            for (type in GeneratorType::class.sealedSubclasses) {
                val gentype = type.objectInstance ?: continue
                if (str_lower == gentype.id.lowercase()) return gentype
            }
            return null
        }
        val CODEC: Codec<GeneratorType> = Codec.STRING.xmap(
            { value ->
                return@xmap parseTypeString(value) ?: when {
                    value.startsWith(GeneratorType.BASE.prefix) -> GeneratorType.BASE.parseSerial(value)
                    else -> error("Unknown generator type: $value")
                }
            },
            { type -> type.id
            },
        )
    }
}

