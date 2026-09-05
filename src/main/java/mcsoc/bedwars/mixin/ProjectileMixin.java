package mcsoc.bedwars.mixin;

import mcsoc.bedwars.eventhandlers.ProjectileHitCallback;
import mcsoc.bedwars.eventhandlers.ThrowableProjectileTickCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class ProjectileMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    protected void onTick(CallbackInfo ci) {}

    @Inject(at = @At("HEAD"), method = "onHit")
    protected void onOnHit(HitResult hitResult, CallbackInfo ci) {
        InteractionResult result = ProjectileHitCallback.Companion.getEVENT().invoker()
                .onHit((Projectile) (Object) this, hitResult);
    }
}
