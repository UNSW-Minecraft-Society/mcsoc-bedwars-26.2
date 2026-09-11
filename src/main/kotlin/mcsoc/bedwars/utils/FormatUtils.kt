package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.util.UUID

val BlockPos.format: String get() = "(${this.x}, ${this.y}, ${this.z})"
val Vec3.format: String get() = "(${this.x}, ${this.y}, ${this.z})"
val romanNumeralMap = mapOf<Int, String>(
    1 to "", 2 to " II", 3 to " III", 4 to " IV", 5 to " V", 6 to " VI", 7 to " VII", 8 to " VIII", 9 to " IX", 10 to " X"
)

val UUID_CODEC: Codec<UUID> = Codec.STRING.xmap(
    {str -> UUID.fromString(str)},
    {id -> id.toString() }
)