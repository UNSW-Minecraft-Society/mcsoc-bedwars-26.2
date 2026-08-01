package mcsoc.bedwars.utils

import net.minecraft.core.BlockPos

val BlockPos.format: String get() = "(${this.x}, ${this.y}, ${this.z})"