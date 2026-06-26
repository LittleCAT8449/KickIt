package spite.kickit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mekanism.common.integration.gender.GenderCapabilityHelper", remap = false)
public abstract class MixinMekanismGenderArmor {

    @Inject(method = "addGenderCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private static void kickit$blockGenderCapability(CallbackInfo ci) {
        ci.cancel();
    }
}
