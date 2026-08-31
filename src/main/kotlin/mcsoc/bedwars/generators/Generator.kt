package mcsoc.bedwars.generators

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

// todo show time remaining until next gen (depending on generator type)
private const val PLAYER_RANGE = 15

internal object GeneratorFactory {
    fun createGenerator(config: GeneratorConfig, loc: Vec3, level: ServerLevel): Generator {
        val place = GenPlace(loc, level)
        return when (config.kind) {
            is GeneratorKind.Default -> Generator(place, config.cycleTime, config.items.toMutableList(), false)
            is GeneratorKind.Base -> BaseGenerator(place, config.cycleTime, config.items.toMutableList(), config.kind.upgradeMultipliers)
            is GeneratorKind.Tiered -> TieredGenerator(place, config.cycleTime, config.items.toMutableList(), config.kind.tierMultipliers)
        }
    }
}

internal data class GenPlace(val location: Vec3, val level: ServerLevel)

// cycle time in ticks
internal open class Generator(val place: GenPlace, val cycleTime: Int, val items: MutableList<GeneratorItem>, showTimer: Boolean) {
    private var rateMultiplier: Double = 1.0
    
    private var currentTick = 0
    private val curCycleItems: HashMap<GeneratorItem, Int> = HashMap()
    var id = -1
    private val timerDisplay = TimerDisplay(place.level, place.location.add(Vec3(0.0, 2.0, 0.0)), cycleTime)
    
    companion object { private var curId = 0 }
    init { 
        id = curId
        curId++
        if (!showTimer) timerDisplay.hide()
    }

    fun setRateMultiplier(rate: Double) {
        rateMultiplier = rate
        timerDisplay.maxTicks = (cycleTime / rateMultiplier).roundToInt()
    }

    fun addItem(item: GeneratorItem) {
        items.add(item)
    }
    
    fun remove() {
        timerDisplay.remove()
    }

    fun tick() {
        currentTick++
        val cycle = cycleTime / rateMultiplier

        for (item in items) {
            val generated = curCycleItems[item] ?: 0
            val expected = (currentTick / cycle * item.itemsPerCycle).toInt()

            if (generated >= expected) continue
            
            if (!hasSpace(place.level, item.maxItems, item.item) || !playersInRange()) {
                curCycleItems[item] = expected
                continue
            }

            generateItem(place.level, item.item)
            curCycleItems[item] = generated + 1
        }

        if (currentTick >= cycle) {
            currentTick = 0
            curCycleItems.clear()
        }
        
        timerDisplay.setText(currentTick)
    }

    private fun hasSpace(level: ServerLevel, max: Int, item: Item): Boolean {
        val nearby = level.getEntitiesOfClass(
            ItemEntity::class.java,
            AABB.ofSize(place.location, 2.0, 2.0, 2.0)
        )

        return nearby.filter { it.item.item == item }.sumOf { it.item.count } < max
    }
    
    private fun playersInRange(): Boolean {
        return place.level.getPlayers { it.position().distanceTo(place.location) < PLAYER_RANGE }.isNotEmpty()
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


internal class BaseGenerator(place: GenPlace, cycleTime: Int, items: MutableList<GeneratorItem>, val upgrades: List<Double>) :
    Generator(place, cycleTime, items, false) {
    
    private var currentUpgrade = 0

    fun upgrade() {
        currentUpgrade++
        if (currentUpgrade >= upgrades.size) return
        setRateMultiplier(upgrades[currentUpgrade])
        if (currentUpgrade == 3) {
            addItem(GeneratorItem(Items.EMERALD, 1, 4))
        }
    }
}

internal class TieredGenerator(place: GenPlace, cycleTime: Int, items: MutableList<GeneratorItem>, val tiers: List<Double>) :
    Generator(place, cycleTime, items, true) {
    
    private var currentTier = 0

    fun upgrade() {
        currentTier++
        if (currentTier >= tiers.size) return
        setRateMultiplier(tiers[currentTier])
    }
}



private class TimerDisplay(level: ServerLevel, pos: Vec3, var maxTicks: Int) {
    var entity: Display.TextDisplay = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level)
    
    init {
        entity.setPos(pos)
        entity.isNoGravity = true
        entity.billboardConstraints = Display.BillboardConstraints.CENTER
        entity.text = Component.literal("Soon").withColor(TextColor.WHITE)
        level.addFreshEntity(entity)
    }
    
    fun remove() {
        entity.discard()
    }
    
    fun setText(cur: Int) {
        val remaining = maxTicks - cur
        val secs = remaining / 20 
        entity.text = Component.literal("$secs seconds left").withColor(TextColor.WHITE)
    }
    
    fun hide() {
        entity.isInvisible = true
    }
}
