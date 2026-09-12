package mcsoc.bedwars.eventhandlers

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionResult

fun registerBlockUseEvents() {
    UseBlockCallback.EVENT.register{player, level, hand, hitResult ->
        if (level !is ServerLevel || player !is ServerPlayer || !player.gameMode().isSurvival) return@register InteractionResult.PASS    
        // if (level.getBlockState(hitResult.blockPos).`is`(BlockTags.BEDS)) return@register InteractionResult.FAIL
        InteractionResult.PASS
    }
}