package mcsoc.bedwars.utils

import kotlinx.serialization.Serializable
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.phys.Vec3
import kotlin.math.absoluteValue
import net.minecraft.world.phys.AABB
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin


const val FLOAT_PI = PI.toFloat()

@Serializable(with=CylindricalBlockPosSerialiser::class)
data class CylindricalBlockPos(
    val radius: Float = 0F,
    val angle: Float = 0F,
    val height: Int = 0
) {    
    companion object {
        fun BlockPos.toCylindricalBlockPos(): CylindricalBlockPos {
            val new_angle = atan2(this.x.toFloat(), this.z.toFloat())
            val new_radius = hypot(this.x.toFloat(), this.z.toFloat())
            return CylindricalBlockPos(new_radius, new_angle, this.y)
        }
    }
    fun toBlockPos(origin: BlockPos): BlockPos {
        return origin.offset((radius * sin(angle)).roundToInt(), height, (radius * cos(angle)).roundToInt())
    }
    
    fun rotated(angle: Float): CylindricalBlockPos = CylindricalBlockPos(
        this.radius,
        (this.angle + angle) % (2 * FLOAT_PI),
        this.height
    )
}

object CylindricalBlockPosSerialiser: KSerializer<CylindricalBlockPos> {
    override val descriptor = buildClassSerialDescriptor("CylindricalBlockPos") {
        element<Float>("radius") // 0
        element<Float>("angle") // 1
        element<Int>("height") // 2
    }
    override fun serialize(encoder: Encoder, value: CylindricalBlockPos) {
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, value.radius)
            encodeFloatElement(descriptor, 1, value.angle * 180 / FLOAT_PI)
            encodeIntElement(descriptor, 2, value.height)
        }
    }
    override fun deserialize(decoder: Decoder): CylindricalBlockPos = decoder.decodeStructure(descriptor) {
        var radius = 0F
        var angle = 0F
        var height = 0
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> radius = decodeFloatElement(descriptor, index)
                1 -> angle  = decodeFloatElement(descriptor, index) * FLOAT_PI / 180
                2 -> height = decodeIntElement(descriptor, index)
                else -> error("Unexpected index: $index")
            }
        }
        
        CylindricalBlockPos(radius, angle, height)
    }
}


fun roundVec(vector: Vec3): Vec3i = BlockPos.containing(vector)
fun vecToBlockPos(vector: Vec3): BlockPos = BlockPos.containing(vector)
fun getCardinalDirection(vector: Vec3): Direction = Direction.getApproximateNearest(vector.horizontal())
fun rotateVec(vector: Vec3i, rotation: Rotation): Vec3i = StructureTemplate.transform(BlockPos(vector), Mirror.NONE, rotation, BlockPos.ZERO)


val AABB_CODEC: Codec<AABB> = RecordCodecBuilder.create {inst -> inst.group(
        Codec.DOUBLE.fieldOf("min_x").forGetter(AABB::minX),
        Codec.DOUBLE.fieldOf("min_y").forGetter(AABB::minY),
        Codec.DOUBLE.fieldOf("min_z").forGetter(AABB::minZ),
        Codec.DOUBLE.fieldOf("max_x").forGetter(AABB::maxX),
        Codec.DOUBLE.fieldOf("max_y").forGetter(AABB::maxY),
        Codec.DOUBLE.fieldOf("max_z").forGetter(AABB::maxZ)
    ).apply(inst, ::AABB)}
