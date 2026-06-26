package spite.kickit.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Mixin targeting Mekanism's Pride holiday class.
 * <ul>
 *   <li>Reuses Mekanism's own Holiday notification system (borders,
 *       rainbow themed lines, {@code [Mekanism]} signature) —
 *       only the text content is replaced.</li>
 *   <li>Forces Robit to use its default BASE skin instead of
 *       LGBTQ+ flag-themed skins during June.</li>
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(targets = "mekanism.common.base.holiday.Pride", remap = false)
public abstract class MixinPride {

    // ---- Cached reflection for package-private record HolidayMessage ----

    private static volatile Constructor<?> holidayMessageCtor;
    private static volatile Method themedLinesMethod;
    private static volatile Method linesMethod;

    private static void initReflection(Object messageInstance) throws ReflectiveOperationException {
        if (holidayMessageCtor != null) return;
        synchronized (MixinPride.class) {
            if (holidayMessageCtor != null) return;
            Class<?> clazz = messageInstance.getClass(); // HolidayMessage
            holidayMessageCtor = clazz.getDeclaredConstructor(Component.class, Component[].class);
            holidayMessageCtor.setAccessible(true);
            themedLinesMethod = clazz.getDeclaredMethod("themedLines");
            themedLinesMethod.setAccessible(true);
            linesMethod = clazz.getDeclaredMethod("lines");
            linesMethod.setAccessible(true);
        }
    }

    // ---- 1. Replace Pride message text, keep Mekanism formatting ----

    /**
     * Replaces only the three text lines inside the {@code HolidayMessage}
     * returned by {@code Pride.getMessage()}. Everything else from Mekanism's
     * holiday notification system is preserved:
     * <ul>
     *   <li>Rainbow themed dashes ({@code ------------})</li>
     *   <li>{@code [Mekanism]} top border and {@code [=======]} bottom border</li>
     *   <li>Colored text formatting</li>
     *   <li>Once-per-day tracked via {@code hasNotified}</li>
     * </ul>
     */
    @Inject(method = "getMessage", at = @At("RETURN"), cancellable = true, remap = false)
    private void kickit$replacePrideText(Player player, CallbackInfoReturnable<Object> cir) throws Throwable {
        Object original = cir.getReturnValue();
        if (original == null) return;

        initReflection(original);

        // Rainbow dashes — keep Mekanism's themed border
        Component themedLines = (Component) themedLinesMethod.invoke(original);

        // Extract the Mekanism signature (last element of varargs)
        Component[] originalLines = (Component[]) linesMethod.invoke(original);
        Component signature = originalLines[originalLines.length - 1];

        // New HolidayMessage: same themedLines + signature, new text
        Object newMessage = holidayMessageCtor.newInstance(themedLines, new Component[]{
                Component.translatable("kickit.june.line1"),
                Component.translatable("kickit.june.line2"),
                Component.translatable("kickit.june.line3"),
                signature
        });
        cir.setReturnValue(newMessage);
    }

    // ---- 2. Force default Robit skin ----

    /**
     * Force Robit to use the default "robit" skin instead of random
     * LGBTQ+ flag skins (gay, lesbian, trans, bi, pan, ace, aro,
     * enby, agender, genderfluid, pride) during June.
     */
    @Inject(method = "randomBaseSkin", at = @At("HEAD"), cancellable = true, remap = false)
    private void kickit$forceDefaultRobitSkin(RandomSource random, CallbackInfoReturnable cir) {
        ResourceKey robitSkinRegistry = ResourceKey.createRegistryKey(
                ResourceLocation.fromNamespaceAndPath("mekanism", "robit_skin")
        );
        cir.setReturnValue(ResourceKey.create(
                robitSkinRegistry,
                ResourceLocation.fromNamespaceAndPath("mekanism", "robit")
        ));
    }
}
