package spite.kickit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mixin targeting Bumblezone's NeoForge config class.
 * <p>
 * After {@code copyToCommon()} writes the config values into
 * {@code BzGeneralConfigs.variantBeeTypes}, this injector removes
 * LGBT/political variant bee IDs so they never spawn.
 * </p>
 * <p>
 * The filter re-applies on every config reload ({@code /reload}),
 * and preserves any custom variants the user may have added.
 * Existing bees in saves keep their variant — only
 * newly spawned bees are affected.
 * </p>
 *
 * @see MixinPride KickIt's existing Mekanism mixin (same reflection pattern)
 */
@Mixin(targets = "com.telepathicgrunt.the_bumblezone.configs.neoforge.BzGeneralConfig", remap = false)
public abstract class MixinBzVariantBees {

    // ---- Variant IDs to remove ----

    private static final Set<String> UNWANTED_VARIANTS = new HashSet<>(Arrays.asList(
            "ukraine_bee",
            "trans_bee",
            "asexual_bee",
            "agender_bee",
            "aroace_bee",
            "aromantic_bee",
            "bisexual_bee",
            "pan_bee",
            "enby_bee",
            "rainbow_bee"
    ));

    // ---- Cached reflection for BzGeneralConfigs.variantBeeTypes ----

    private static volatile Field variantBeeTypesField;
    private static volatile boolean reflectionInitialized;

    /**
     * Double-checked locking accessor for the config list field.
     * Reflection objects are cached to avoid repeated lookups.
     */
    private static Field getVariantBeeTypesField() throws ReflectiveOperationException {
        if (reflectionInitialized) return variantBeeTypesField;
        synchronized (MixinBzVariantBees.class) {
            if (reflectionInitialized) return variantBeeTypesField;
            Class<?> configsClass = Class.forName(
                    "com.telepathicgrunt.the_bumblezone.configs.BzGeneralConfigs"
            );
            variantBeeTypesField = configsClass.getDeclaredField("variantBeeTypes");
            variantBeeTypesField.setAccessible(true);
            reflectionInitialized = true;
            return variantBeeTypesField;
        }
    }

    // ---- Injection point ----

    /**
     * Strips unwanted variant IDs from the live config list
     * <em>after</em> Bumblezone has finished copying its config values.
     * <p>
     * Targeted method: {@code BzGeneralConfig.copyToCommon()}
     * — called on initial config load and on every {@code /reload}.
     * </p>
     */
    @Inject(method = "copyToCommon", at = @At("RETURN"), remap = false)
    private static void kickit$filterBzVariantBees(CallbackInfo ci) {
        try {
            Field field = getVariantBeeTypesField();
            if (field == null) return;

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) field.get(null);
            if (list != null) {
                list.removeAll(UNWANTED_VARIANTS);
            }
        } catch (ReflectiveOperationException ignored) {
            // Bumblezone not installed or internal structure changed — skip
        }
    }
}
