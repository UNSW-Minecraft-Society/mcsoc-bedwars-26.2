package mcsoc.bedwars.mixin;

import mcsoc.bedwars.datatrackers.LevelData;
import mcsoc.bedwars.utils.Team;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
public class IronGolemMixin {
    @Inject(at = @At("HEAD"), method = "registerGoals")
    private void onRegisterGoals(CallbackInfo ci) {
        IronGolem golem = (IronGolem) (Object) this;
        golem.getGoalSelector().addGoal(3, new NearestAttackableTargetGoal<>(golem, Player.class, 5, false, false, (target, level) -> isDefenderAggro(golem, target, level)));
    }

    @Unique
    private boolean isDefenderAggro(IronGolem golem, LivingEntity player, ServerLevel level) {
        if (LevelData.getCustomEntityData(level).getEntityType(golem) == null) return false;
        Team golemTeam = LevelData.getCustomEntityData(level).getEntityTeam(golem);
        Team playerTeam = LevelData.getGameState(level).getPlayersTeam(player.getUUID());
        if (golemTeam == null) return false;
        else return golemTeam != playerTeam;
    }
}
