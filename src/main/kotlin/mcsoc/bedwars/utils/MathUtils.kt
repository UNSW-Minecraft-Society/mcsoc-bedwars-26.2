package mcsoc.bedwars.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

data class CylindricalBlockPos(val origin: BlockPos, var radius: Float, var angle: Float, var height: Int) {
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

fun roundVec(vector: Vec3): Vec3i {
    return Vec3i(vector.x.roundToInt(), vector.y.roundToInt(), vector.z.roundToInt())
}

fun vecToBlockPos(vector: Vec3): BlockPos {
    return BlockPos(roundVec(vector))
}