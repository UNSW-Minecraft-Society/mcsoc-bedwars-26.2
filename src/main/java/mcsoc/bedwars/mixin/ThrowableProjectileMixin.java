package mcsoc.bedwars.mixin;

import mcsoc.bedwars.eventhandlers.ThrowableProjectileTickCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.projectile.ThrowableProjectile.class)
public class ThrowableProjectileMixin {
    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci) {
        InteractionResult result = ThrowableProjectileTickCallback.Companion.getEVENT().invoker()
                .tick((ThrowableProjectile) (Object) this);
    }
}

// * (The room between... There is a room between.)
