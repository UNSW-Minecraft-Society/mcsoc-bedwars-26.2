package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.gamestate.GameManager
import mcsoc.bedwars.utils.ticks
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects


fun registerAfterDeathEvent() {
    ServerLivingEntityEvents.AFTER_DEATH.register{maybe_player, death_source ->
        if (maybe_player !is ServerPlayer) return@register
        if (maybe_player.level().gameState.getGamePhase() == GamePhase.ACTIVE) {
            GameManager.handlePlayerDeath(maybe_player, death_source)
        }
    }
}


fun registerAfterRespawnEvent() {
    ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
        GameManager.handlePlayerRespawn(newPlayer)
    }
}


fun registerAfterEffectAppliedEvent() {
    ServerMobEffectEvents.AFTER_ADD.register{effect, maybe_player, ctx ->
        if (maybe_player !is ServerPlayer) return@register
        if (effect.`is`(MobEffects.INVISIBILITY)) {
            maybe_player.level().gameState.setPlayerInvisibility(maybe_player.uuid, true)
            maybe_player.level().eventQueue.queueInvisExpiry((effect.duration + 1).ticks, maybe_player.uuid)
        }
    }
}


fun registerPlayerDamageEvent() {
    ServerLivingEntityEvents.AFTER_DAMAGE.register{maybe_player, source, baseDamageTaken, damageTaken, blocked ->
        if (maybe_player !is ServerPlayer || blocked) return@register
        maybe_player.level().gameState.setPlayerInvisibility(maybe_player.uuid, false)
    }
}


fun registerPlayerJoinEvent() {
    /* TODO have players given info on join
     * If joining between games, tell them to ready up with /bedwars join
     * If joining during a game, tell them to wait until this game finishes
     * ect. There could also be some thing to reroute them to any inactive worlds?
     */
    ServerPlayConnectionEvents.JOIN.register{ handler, _, server ->
        GameManager.handlePlayerJoin(server.scoreboard, handler.player)
    }
}