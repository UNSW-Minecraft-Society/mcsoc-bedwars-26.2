package mcsoc.bedwars.eventhandlers

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrowableProjectile

fun interface ThrowableProjectileTickCallback {
    companion object {
        @JvmStatic
        val EVENT = EventFactory.createArrayBacked(ThrowableProjectileTickCallback::class.java) { listeners ->
            ThrowableProjectileTickCallback { projectile: ThrowableProjectile ->
                for (listener in listeners) {
                    val result = listener.tick(projectile)

                    if (result != InteractionResult.PASS) {
                        return@ThrowableProjectileTickCallback result
                    }
                }
                InteractionResult.PASS
            }
        }
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
