package mcsoc.bedwars.utils

import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

val BlockPos.format: String get() = "(${this.x}, ${this.y}, ${this.z})"
val Vec3.format: String get() = "(${this.x}, ${this.y}, ${this.z})"
val romanNumeralMap = mapOf<Int, String>(
    1 to "", 2 to " II", 3 to " III", 4 to " IV", 5 to " V", 6 to " VI", 7 to " VII", 8 to " VIII", 9 to " IX", 10 to " X"
)

fun getProgressBar(fraction: Double, size: Int): String {
    val numBar = (fraction * size).roundToInt()
    val numSpace = size - numBar
    BedwarsPlugin.LOGGER.info("fraction: $fraction, [$numBar:$numSpace]")
    return "[" + "█".repeat(numBar) + "░".repeat(numSpace) + "]"
}
