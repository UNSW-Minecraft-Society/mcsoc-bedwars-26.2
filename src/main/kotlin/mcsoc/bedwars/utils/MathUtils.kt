package mcsoc.bedwars.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import net.minecraft.core.BlockPos


data class CylindricalBlockPos(
    var origin: BlockPos = BlockPos(0, 0, 0),
    var radius: Float = 0F,
    var angle: Float = 0F,
    var height: Int = 0
) {
    companion object {
        fun BlockPos.toCylindricalBlockPos(): CylindricalBlockPos {
            val new_angle = atan2(this.x.toFloat(), this.z.toFloat())
            val new_radius = hypot(this.x.toFloat(), this.z.toFloat())
            return CylindricalBlockPos(this.immutable(), new_radius, new_angle, this.y)
        }
    }
    fun toBlockPos(): BlockPos {
        return origin.offset((radius * sin(angle)).roundToInt(), height, (radius * cos(angle)).roundToInt())
    }
}