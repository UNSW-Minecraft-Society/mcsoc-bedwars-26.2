package mcsoc.bedwars.generators

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.utils.Team
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

private const val PLAYER_RANGE = 15
private const val ITEM_SPAWN_HEIGHT = 2.0

internal object GeneratorFactory {
    fun createGenerator(type: GeneratorType, loc: Vec3, level: ResourceKey<Level>): Generator {
        require(type.getConfig().kind !is GeneratorKind.Base) {"$type requires a team — use createTeamGenerator"}
        return Generator(loc, level, type, team = null)
    }

    fun createGenerator(type: GeneratorType, loc: Vec3, level: ResourceKey<Level>, team: Team): Generator {
        require(type.getConfig().kind is GeneratorKind.Base) {"$type is not a team generator — use createGenerator"}
        return Generator(loc, level, type, team)
    }
}

// cycle time in ticks
internal open class Generator(val location: Vec3, val levelKey: ResourceKey<Level>, val type: GeneratorType, val team: Team? = null) {
    companion object { 
        private var curId = 0
        val CODEC: Codec<Generator> = RecordCodecBuilder.create{it.group(
            Vec3.CODEC.fieldOf("location").forGetter(Generator::location),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(Generator::levelKey),
            GeneratorType.CODEC.fieldOf("type").forGetter(Generator::type),
            Team.CODEC.fieldOf("team").forGetter(Generator::team)
        ).apply(it, ::Generator)}
    }
    
    private lateinit var level: ServerLevel
    private lateinit var timerDisplay: TimerDisplay
    private val config = type.getConfig()
    
    private var currentTick = 0
    var id = -1
    private val curCycleItems: HashMap<GeneratorItem, Int> = HashMap()
    var placed = false
    
    init { 
        id = curId
        curId++
    }
    
    fun place(server: MinecraftServer) {
        level = server.getLevel(levelKey) ?: throw Exception("Invalid level sent to generator")
        timerDisplay = TimerDisplay(level, location.add(Vec3(0.0, ITEM_SPAWN_HEIGHT, 0.0)))
        if (!config.showTimer) timerDisplay.hide()
        placed = true
    }
    
    fun remove() = timerDisplay.remove()

    private var lastUpgrade = -1
    fun tick() {
        currentTick++
        val upgrade = currentUpgrade()
        if (upgrade != lastUpgrade) {
            lastUpgrade = upgrade
            currentTick = 0
            curCycleItems.clear()
        }
        
        val rateMultiplier = config.kind.rateAt(upgrade)
        val cycle = config.cycleTime / rateMultiplier

        for (item in config.kind.itemsAt(upgrade)) {
            val generated = curCycleItems[item] ?: 0
            val expected = (currentTick / cycle * item.itemsPerCycle).toInt()

            if (generated >= expected) continue
            if (!hasSpace(level, item.maxItems, item.item) || !playersInRange()) {
                curCycleItems[item] = expected
                continue
            }

            generateItem(level, item.item)
            curCycleItems[item] = generated + 1
        }

        if (currentTick >= cycle) {
            currentTick = 0
            curCycleItems.clear()
        }
        
        timerDisplay.setText(currentTick, cycle.toInt())
    }
    
    private fun currentUpgrade(): Int = when (config.kind) {
        is GeneratorKind.Default -> 0
        is GeneratorKind.Base -> level.gameState.getGenUpgrade(team!!)
        is GeneratorKind.Tiered -> level.generatorState.getGeneratorUpgrade(type)
    }

    private fun hasSpace(level: ServerLevel, max: Int, item: Item): Boolean {
        val nearby = level.getEntitiesOfClass(
            ItemEntity::class.java,
            AABB.ofSize(location, 2.0, 2.0, 2.0)
        )

        return nearby.filter { it.item.item == item }.sumOf { it.item.count } < max
    }
    
    private fun playersInRange(): Boolean {
        return level.getPlayers { it.position().distanceTo(location) < PLAYER_RANGE }.isNotEmpty()
    }

    private fun generateItem(level: ServerLevel, item: Item) {
        val itemstack = ItemStack(item, 1)
        val entity = ItemEntity(level, location.x, location.y + 1, location.z, itemstack)
        entity.addTag("generator_item")
        entity.setDeltaMovement(0.0, 0.0, 0.0)
        level.addFreshEntity(entity)
    }
}


private class TimerDisplay(level: ServerLevel, pos: Vec3) {
    var entity: Display.TextDisplay = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level)
    private var hidden = false
    
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
    
    fun setText(cur: Int, max: Int) {
        if (hidden) return
        val remaining = max - cur
        val secs = remaining / 20 
        entity.text = Component.literal("$secs seconds left").withColor(TextColor.WHITE)
    }
    
    fun hide() {
        hidden = true
        entity.text = Component.empty()
    }
}
