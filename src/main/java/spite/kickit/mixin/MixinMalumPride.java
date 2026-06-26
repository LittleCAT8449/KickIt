package spite.kickit.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mixin targeting Minecraft's {@code RecipeManager}.
 * <p>
 * After all recipes are loaded from datapacks and mod JARs,
 * removes the 18 Malum prideweave recipes from the recipe lookup.
 * The prideweave <em>items</em> remain fully functional —
 * only the survival crafting path is removed.
 * </p>
 * <p>
 * When Malum is not installed, the recipe IDs simply won't exist
 * in the loaded map, so the filter is a no-op with minimal overhead.
 * </p>
 *
 * @see MixinPride         KickIt's Mekanism mixin (same reflection pattern)
 * @see MixinBzVariantBees KickIt's Bumblezone mixin (same reflection pattern)
 */
@Mixin(RecipeManager.class)
public abstract class MixinMalumPride {

    @Unique
    private static final String MALUM = "malum";

    @Unique
    private static final String PRIDEWEAVE_SUFFIX = "_prideweave";

    // ---- Cached reflection for RecipeManager internals ----

    @Unique
    private static volatile Field byNameField;

    @Unique
    private static volatile Field byTypeField;

    @Unique
    private static volatile boolean reflectionInitialized;

    @Unique
    private static void initReflection() throws ReflectiveOperationException {
        if (reflectionInitialized) return;
        synchronized (MixinMalumPride.class) {
            if (reflectionInitialized) return;
            byNameField = RecipeManager.class.getDeclaredField("byName");
            byNameField.setAccessible(true);
            byTypeField = RecipeManager.class.getDeclaredField("byType");
            byTypeField.setAccessible(true);
            reflectionInitialized = true;
        }
    }

    // ---- Injection point ----

    /**
     * Filters prideweave recipes <em>after</em> all recipes have been loaded.
     * <p>
     * Target: {@code RecipeManager.apply(Map, ResourceManager, ProfilerFiller)}
     * — called once on world load and again on every {@code /reload}.
     * </p>
     */
    @Inject(method = "apply", at = @At("RETURN"))
    private void kickit$removeMalumPrideRecipes(CallbackInfo ci) {
        try {
            initReflection();
            if (byNameField == null) return;

            // ---- filter byName (primary recipe lookup map) ----
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, RecipeHolder<?>> byName =
                    (Map<ResourceLocation, RecipeHolder<?>>) byNameField.get(this);

            Map<ResourceLocation, RecipeHolder<?>> filtered = new LinkedHashMap<>(byName);
            Set<RecipeHolder<?>> removed = new HashSet<>();

            filtered.keySet().removeIf(key -> {
                if (MALUM.equals(key.getNamespace())
                        && key.getPath().endsWith(PRIDEWEAVE_SUFFIX)) {
                    removed.add(byName.get(key));
                    return true;
                }
                return false;
            });

            if (removed.isEmpty()) {
                return; // Malum not installed — nothing to filter
            }

            byNameField.set(this, ImmutableMap.copyOf(filtered));

            // ---- filter byType (recipe book grouping multimap) ----
            @SuppressWarnings("unchecked")
            Multimap<RecipeType<?>, RecipeHolder<?>> byType =
                    (Multimap<RecipeType<?>, RecipeHolder<?>>) byTypeField.get(this);

            ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> builder =
                    ImmutableMultimap.builder();
            for (Map.Entry<RecipeType<?>, RecipeHolder<?>> entry : byType.entries()) {
                if (!removed.contains(entry.getValue())) {
                    builder.put(entry.getKey(), entry.getValue());
                }
            }
            byTypeField.set(this, builder.build());

        } catch (ReflectiveOperationException ignored) {
            // RecipeManager structure changed — silently skip
        }
    }
}
