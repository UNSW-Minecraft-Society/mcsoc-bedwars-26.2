package mcsoc.bedwars.eventhandlers

import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ThrowableProjectile
import net.minecraft.world.phys.HitResult

fun interface ProjectileHitCallback {
    companion object {
        @JvmStatic
        val EVENT = EventFactory.createArrayBacked(ProjectileHitCallback::class.java) { listeners ->
            ProjectileHitCallback { projectile: Projectile, hitResult: HitResult ->
                for (listener in listeners) {
                    val result = listener.onHit(projectile, hitResult)

                    if (result != InteractionResult.PASS) {
                        return@ProjectileHitCallback result
                    }
                }
                InteractionResult.PASS
            }
        }
    }

    fun onHit(projectile: Projectile, hitResult: HitResult): InteractionResult
}
