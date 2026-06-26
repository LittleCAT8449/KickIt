package spite.kickit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mekanism.tools.common.integration.gender.ToolsGenderCapabilityHelper", remap = false)
public abstract class MixinToolsGenderArmor {

    @Inject(method = "addGenderCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private static void kickit$blockToolsGenderCapability(CallbackInfo ci) {
        ci.cancel();
    }
}
