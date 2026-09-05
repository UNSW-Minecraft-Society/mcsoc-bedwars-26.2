package mcsoc.bedwars.mixin;

import mcsoc.bedwars.eventhandlers.ThrowableProjectileTickCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrowableProjectile.class)
public class ThrowableProjectileMixin extends ProjectileMixin {
    @Override
    protected void onTick(CallbackInfo ci) {
        InteractionResult result = ThrowableProjectileTickCallback.Companion.getEVENT().invoker()
                .tick((ThrowableProjectile) (Object) this);
    }
}

// * (The room between... There is a room between.)