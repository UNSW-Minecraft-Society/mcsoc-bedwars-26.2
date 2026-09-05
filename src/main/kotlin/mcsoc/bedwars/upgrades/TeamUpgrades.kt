package mcsoc.bedwars.upgrades

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level

// basically enum
sealed class TeamUpgradeType<T>(val default: () -> TeamUpgrade<T>) {
    object PROTECTION : TeamUpgradeType<Int>({ IntUpgrade(4, listOf(Cost(0), Cost(0), Cost(0), Cost(0))) })
    object FEATHER_FALLING : TeamUpgradeType<Int>({ IntUpgrade(2, listOf(Cost(0), Cost(0))) })
    object HASTE : TeamUpgradeType<Int>({ IntUpgrade(2, listOf(Cost(0), Cost(0))) }) // todo
    object SHARPNESS : TeamUpgradeType<Boolean>({ BooleanUpgrade(Cost(0)) })
    object HEAL_POOL : TeamUpgradeType<Boolean>({ BooleanUpgrade(Cost(0)) }) // todo
}

// temp
data class Cost(val iron: Int)

interface TeamUpgrade<T> {
    val value: T
    fun upgrade()
    fun getCurrentCost(): Cost
}

class BooleanUpgrade(val cost: Cost, override var value: Boolean = false) : TeamUpgrade<Boolean> {
    override fun upgrade() {
        value = true
    }

    override fun getCurrentCost(): Cost = cost
}

class IntUpgrade(
    var max: Int,
    val costs: List<Cost>,
    override var value: Int = 0
) : TeamUpgrade<Int> {
    override fun upgrade() {
        if (value >= max) return
        value++
    }

    override fun getCurrentCost(): Cost = costs[value]
}

// could change to pass cost value and array into function parameter if needed