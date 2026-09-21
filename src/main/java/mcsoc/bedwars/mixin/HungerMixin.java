package mcsoc.bedwars.mixin;

import mcsoc.bedwars.datatrackers.GamePeriod;
import mcsoc.bedwars.datatrackers.LevelData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class HungerMixin {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(ServerPlayer player, CallbackInfo ci) {
        if (player.level() instanceof ServerLevel slevel && LevelData.getGameState(slevel).getGamePeriod() != GamePeriod.INACTIVE) {
            this.foodLevel = 20;
            this.saturationLevel = 5.0F;
            this.exhaustionLevel = 0.0F;
        }
    }
}