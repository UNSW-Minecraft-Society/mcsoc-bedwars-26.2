package mcsoc.bedwars.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

val BlockPos.format: String get() = "(${this.x}, ${this.y}, ${this.z})"
val Vec3.format: String get() = "(${this.x}, ${this.y}, ${this.z})"