package mcsoc.bedwars.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import mcsoc.bedwars.datatrackers.GamePhase;
import mcsoc.bedwars.datatrackers.LevelData;

@Mixin(Entity.class)
public abstract class VoidDamageMixin {
    @Shadow public abstract double getY();
    @Shadow public abstract Level level();
    @Shadow protected abstract void onBelowWorld();

    private int voidThreshold = -40;
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void injectCustomVoidThreshold(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof ServerPlayer player)) return;
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverlevel)) return;

        var mapY = LevelData.getGameState(serverlevel).getMap_centre().getY() + voidThreshold;
        var isGameActive = LevelData.getGameState(serverlevel).getGamePhase().equals(GamePhase.ACTIVE);
        var isPlayerActive = LevelData.getGameState(serverlevel).getActivePlayers().contains(player.getUUID());

        if (isGameActive && isPlayerActive && this.getY() < mapY) {
            this.onBelowWorld();
        }
    }
}
