package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.effects.AllOf.EntityEffects;

import mcsoc.bedwars.GameEffects;
import mcsoc.bedwars.datatrackers.LevelData;


@Mixin(ServerPlayer.class)
public class PlayerInvisibilityTickMixin {
    
    @Inject(at = @At("RETURN"), method = "tick")
    void tick(CallbackInfo ci) {
        ServerPlayer this_player = (ServerPlayer)(Object)this;
        ServerLevel level = this_player.level();
        var gameState = LevelData.getGameState(level);
        
        if (gameState.getPlayerInvisibility(this_player.getUUID())) {
            GameEffects.broadcastInvisibility(this_player);
        }
    }
    
}
