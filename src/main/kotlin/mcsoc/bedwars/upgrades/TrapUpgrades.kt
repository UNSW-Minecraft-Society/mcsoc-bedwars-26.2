package mcsoc.bedwars.upgrades

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects

enum class TrapUpgrade {
    BLINDNESS {
        override fun enemyEffect(level: ServerLevel, players: List<ServerPlayer>) {
            players.forEach { 
                it.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 8 * 20, 1, false, false))
                it.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 8 * 20, 0, false, false))
            }
        }
    },
    COUNTER {
        override fun teamEffect(level: ServerLevel, players: List<ServerPlayer>) {
            players.forEach { 
                it.addEffect(MobEffectInstance(MobEffects.SPEED, 10 * 20, 0, false, false))
                it.addEffect(MobEffectInstance(MobEffects.JUMP_BOOST, 10 * 20, 1, false, false))
            }
        }
    },
    REVEAL {
        override fun enemyEffect(level: ServerLevel, players: List<ServerPlayer>) {
            players.forEach { 
                it.addEffect(MobEffectInstance(MobEffects.GLOWING, 8 * 20, 0, false, false))
            }
        }
        // todo
        // override fun teamEffect(level: ServerLevel, players: List<ServerPlayer>) {
        //     players.forEach { 
        //         // alarm team members
        //     }
        // }
    },
    MINING {
        override fun enemyEffect(level: ServerLevel, players: List<ServerPlayer>) {
            players.forEach { 
                it.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE, 10 * 20, 0, false, false))
            }
        }
    };
    
    open fun enemyEffect(level: ServerLevel, players: List<ServerPlayer>) {}
    open fun teamEffect(level: ServerLevel, players: List<ServerPlayer>) {}
}