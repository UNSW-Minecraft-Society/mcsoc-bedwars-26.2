package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
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

val AABB_CODEC: Codec<AABB> = RecordCodecBuilder.create {inst -> inst.group(
        Codec.DOUBLE.fieldOf("min_x").forGetter(AABB::minX),
        Codec.DOUBLE.fieldOf("min_y").forGetter(AABB::minY),
        Codec.DOUBLE.fieldOf("min_z").forGetter(AABB::minZ),
        Codec.DOUBLE.fieldOf("max_x").forGetter(AABB::maxX),
        Codec.DOUBLE.fieldOf("max_y").forGetter(AABB::maxY),
        Codec.DOUBLE.fieldOf("max_z").forGetter(AABB::maxZ)
    ).apply(inst, ::AABB)}