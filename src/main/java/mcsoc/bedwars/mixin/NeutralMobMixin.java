package mcsoc.bedwars.mixin;

import mcsoc.bedwars.datatrackers.LevelData;
import mcsoc.bedwars.utils.Team;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NeutralMob.class)
public interface NeutralMobMixin {
    @Inject(at = @At("HEAD"), method = "isAngryAt", cancellable = true)
    default void onIsAngryAt(final LivingEntity entity, final ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayer) {
            @Nullable Team entityTeam = LevelData.getCustomEntityData(level).getEntityTeam((Entity) (Object) this);
            @Nullable Team playerTeam = LevelData.getGameState(level).getPlayersTeam(entity.getUUID());
            if (entityTeam != null && playerTeam != Team.NONE) cir.setReturnValue(entityTeam == playerTeam);
        }
    }
}
