package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import mcsoc.bedwars.datatrackers.LevelData;

@Mixin(Entity.class)
public abstract class VoidDamageMixin {
    @Shadow public abstract double getY();
    @Shadow public abstract Level level();
    @Shadow protected abstract void onBelowWorld();

    private int voidThreshold = -50;
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void injectCustomVoidThreshold(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverlevel)) return;


        var mapY = LevelData.getGameState(serverlevel).getMap_centre().getY() + voidThreshold;

        if (this.getY() < mapY) {
            this.onBelowWorld();
        }
    }
}
