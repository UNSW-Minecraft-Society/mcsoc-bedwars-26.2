package mcsoc.bedwars.eventhandlers

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrowableProjectile

interface ThrowableProjectileTickCallback {
    companion object {
        val EVENT: Event<ThrowableProjectileTickCallback>
            get() = EventFactory.createArrayBacked(ThrowableProjectileTickCallback::class.java,{
                    listeners: Array<ThrowableProjectileTickCallback> -> object : ThrowableProjectileTickCallback {
                override fun tick(projectile: ThrowableProjectile): InteractionResult {
                    for (listener in listeners) {
                        val result = listener.tick(projectile)
                        if (result != InteractionResult.PASS) return result
                    }

                    return InteractionResult.PASS
                }
            }
            })
    }

    fun tick(projectile: ThrowableProjectile): InteractionResult
}

//⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀ ⠀ ⠀⠀⢀⣤⣤⣤⣤⣄⠀⠀⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⠀ ⠀⠀⢀⣀⣀⣀⣀⠀⠀⣠⣤⣬⣽⣿⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⠀⣀⡀⢸⣿⣿⣿⣿⣶⣶⣶⡾⣿⡿⠿⣿⣿⣿⣿⣷⠀⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⢸⢕⢽⣿⣿⣿⣿⣿⣿⣿⣿⢕⢕⢕⢕⣿⣿⣿⣿⣿⣄⣀⣀⡀⠀⠀
//⠀⣤⣤⣤⣼⣕⢕⢕⢕⢕⢕⢕⣿⣿⣷⣷⣷⣷⣷⣿⣿⣿⣿⣿⣿⣿⣿⡇⠀⠀
//⠀⠉⠙⣝⢝⢕⢕⢕⢕⡯⡯⡯⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢿⢿⢿⢿⢿⡇⠀⠀
//⠀⠀⠀⣗⢕⢕⢕⢕⢕⡯⡯⡯⣿⣿⢿⢿⢿⢿⢿⢿⢿⣿⢝⢝⡽⠽⠽⠀⠀⠀
//⠀⠀⠀⠉⠉⠉⠉⢰⣿⣿⣿⣿⣏⣷⣷⣷⣷⣷⢕⢕⢕⣕⣕⣕⠇⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠋⣿⣿⣿⣿⠈⡝⢛⡓⠓⠁⠀⠀⠀⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠩⣭⣭⠭⠥⠊⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡰⠻⠗⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
//⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠠⠖⠡⠇⠧⠦⠄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
// * (Well, there is a man here.)
