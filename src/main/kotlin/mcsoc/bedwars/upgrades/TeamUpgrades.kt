package mcsoc.bedwars.upgrades

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level

sealed class TeamUpgradeType<T>(val default: () -> TeamUpgrade<T>) {
    object PROTECTION : TeamUpgradeType<Int>({ IntUpgrade(4) })
    object FEATHER_FALLING : TeamUpgradeType<Int>({ IntUpgrade(2) })
    object HASTE : TeamUpgradeType<Int>({ IntUpgrade(2) })
    object SHARPNESS : TeamUpgradeType<Boolean>({ BooleanUpgrade() })
    object HEAL_POOL : TeamUpgradeType<Boolean>({ BooleanUpgrade() })
}

interface TeamUpgrade<T> {
    val value: T
    fun upgrade()
}

class BooleanUpgrade(override var value: Boolean = false) : TeamUpgrade<Boolean> {
    override fun upgrade() {
        value = true
    }
}

class IntUpgrade(var max: Int, override var value: Int = 0) : TeamUpgrade<Int> {
    override fun upgrade() {
        if (value >= max) return
        value++
    }
}
