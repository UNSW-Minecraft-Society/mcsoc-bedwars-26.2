package mcsoc.bedwars.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
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
    return BlockPos(floor(vector.x).toInt(), floor(vector.y).toInt(), floor(vector.z).toInt())
}

fun getCardinalDirection(vector: Vec3): Direction {
    val x = vector.x
    val z = vector.z
    return if (x.absoluteValue > z.absoluteValue) {
        if (x > 0) Direction.EAST
        else Direction.WEST
    } else {
        if (z > 0) Direction.SOUTH
        else Direction.NORTH
    }
}

fun rotateVec(vector: Vec3i, rotation: Rotation): Vec3i {
    return when (rotation) {
        Rotation.COUNTERCLOCKWISE_90 -> Vec3i(vector.z, vector.y, -vector.x)
        Rotation.NONE -> vector
        Rotation.CLOCKWISE_180 -> vector.multiply(-1, 1, -1)
        Rotation.CLOCKWISE_90 -> Vec3i(-vector.z,vector.y,vector.x)
    }
}