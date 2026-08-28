package mcsoc.bedwars.entities

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level

class Shopkeeper(type: EntityType<out LivingEntity>, level: Level) : LivingEntity(type, level) {
    override fun getMainArm(): HumanoidArm {
        return HumanoidArm.LEFT
    }

}